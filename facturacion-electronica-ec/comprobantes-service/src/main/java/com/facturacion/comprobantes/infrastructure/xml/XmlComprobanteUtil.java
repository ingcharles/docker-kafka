package com.facturacion.comprobantes.infrastructure.xml;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.model.vo.Pago;

/**
 * Construcción de DOM y serialización compartidas entre los 6 generadores
 * de XML — cada uno arma su propio árbol de etiquetas (la estructura difiere
 * bastante entre tipos, ver ficha_tecnica_233.txt), pero todos pasan por el
 * mismo {@link DocumentBuilder}/{@link Transformer} y las mismas
 * convenciones de formato numérico/fecha.
 */
final class XmlComprobanteUtil {

    private XmlComprobanteUtil() {
    }

    static Document nuevoDocumento() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    static String serializar(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(writer.toString().getBytes(StandardCharsets.UTF_8));
        return out.toString(StandardCharsets.UTF_8);
    }

    static void texto(Document doc, Element padre, String etiqueta, String valor) {
        Element elemento = doc.createElement(etiqueta);
        elemento.setTextContent(valor);
        padre.appendChild(elemento);
    }

    static String montoTexto(BigDecimal monto) {
        return monto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** dirEstablecimiento es obligatorio en NC/ND/Retención pero este servicio no rastrea establecimientos aparte — se usa la matriz como valor por defecto. */
    static String direccionEstablecimiento(Comprobante c) {
        return c.getDireccionEstablecimiento() != null && !c.getDireccionEstablecimiento().isBlank()
            ? c.getDireccionEstablecimiento()
            : c.getEmpresaDireccionMatriz();
    }

    /** {@code <detalle>} completo (código, descripción, cantidad, precio, descuento, impuestos) — Factura/Nota de Crédito/Liquidación de Compra comparten esta forma. */
    static Element construirDetalle(Document doc, DetalleComprobante d) {
        Element detalle = doc.createElement("detalle");
        texto(doc, detalle, "codigoPrincipal", d.getCodigoPrincipal());
        if (d.getCodigoAuxiliar() != null && !d.getCodigoAuxiliar().isBlank()) {
            texto(doc, detalle, "codigoAuxiliar", d.getCodigoAuxiliar());
        }
        texto(doc, detalle, "descripcion", d.getDescripcion());
        texto(doc, detalle, "cantidad", d.getCantidad().toPlainString());
        texto(doc, detalle, "precioUnitario", montoTexto(d.getPrecioUnitario()));
        texto(doc, detalle, "descuento", montoTexto(d.getDescuento() == null ? BigDecimal.ZERO : d.getDescuento()));
        texto(doc, detalle, "precioTotalSinImpuesto", montoTexto(d.precioTotalSinImpuesto()));

        Element impuestos = doc.createElement("impuestos");
        for (Impuesto imp : d.getImpuestos()) {
            Element impuesto = doc.createElement("impuesto");
            texto(doc, impuesto, "codigo", imp.codigo());
            texto(doc, impuesto, "codigoPorcentaje", imp.codigoPorcentaje());
            texto(doc, impuesto, "tarifa", imp.tarifa().toPlainString());
            texto(doc, impuesto, "baseImponible", montoTexto(imp.baseImponible()));
            texto(doc, impuesto, "valor", montoTexto(imp.valor()));
            impuestos.appendChild(impuesto);
        }
        detalle.appendChild(impuestos);
        return detalle;
    }

    /** Agrupa los impuestos de todas las líneas de detalle por código+código de porcentaje — para el {@code <totalConImpuestos>}/{@code <impuestos>} de encabezado. */
    static Map<String, List<Impuesto>> agruparImpuestos(Comprobante c) {
        return c.getDetalles().stream()
            .flatMap(d -> d.getImpuestos().stream())
            .collect(Collectors.groupingBy(imp -> imp.codigo() + "|" + imp.codigoPorcentaje()));
    }

    /**
     * {@code <infoAdicional>} completo: el campo "granContribuyente" (Anexo
     * 24, solo cuando la empresa es Gran Contribuyente), luego "RUC Proveedor"
     * (Anexo 26, Res. NAC-DGERCGC26-00000027 — solo cuando la empresa
     * configuró {@code sri.proveedor.ruc} en {@code configuracion_empresa},
     * ver {@code ComprobanteService.emitir}) — ambos calculados por el
     * sistema, no algo que el llamador mande — seguidos de cada
     * {@code campoAdicional} que haya venido en el request. Devuelve
     * {@code null} si no hay nada que agregar (el tag completo es opcional
     * en el XSD del SRI).
     */
    static Element construirInfoAdicional(Document doc, Comprobante c) {
        boolean hayGranContribuyente = c.isEmpresaEsGranContribuyente();
        boolean hayProveedorRuc = c.getProveedorRuc() != null && !c.getProveedorRuc().isBlank();
        boolean hayCamposAdicionales = c.getCamposAdicionales() != null && !c.getCamposAdicionales().isEmpty();
        if (!hayGranContribuyente && !hayProveedorRuc && !hayCamposAdicionales) {
            return null;
        }
        Element infoAdicional = doc.createElement("infoAdicional");
        if (hayGranContribuyente) {
            Element campo = doc.createElement("campoAdicional");
            campo.setAttribute("nombre", "granContribuyente");
            campo.setTextContent("Gran Contribuyente " + c.getEmpresaNumeroResolucionGranContribuyente());
            infoAdicional.appendChild(campo);
        }
        if (hayProveedorRuc) {
            Element campo = doc.createElement("campoAdicional");
            campo.setAttribute("nombre", "RUC Proveedor");
            campo.setTextContent(c.getProveedorRuc());
            infoAdicional.appendChild(campo);
        }
        if (hayCamposAdicionales) {
            for (CampoAdicional campoAdicional : c.getCamposAdicionales()) {
                Element campo = doc.createElement("campoAdicional");
                campo.setAttribute("nombre", campoAdicional.nombre());
                campo.setTextContent(campoAdicional.valor());
                infoAdicional.appendChild(campo);
            }
        }
        return infoAdicional;
    }

    /** {@code <pagos>} — solo Factura, Nota de Débito y Liquidación de Compra lo llevan (ver {@code Pago}). */
    static Element construirPagos(Document doc, List<Pago> pagos) {
        Element pagosEl = doc.createElement("pagos");
        for (Pago pago : pagos) {
            Element pagoEl = doc.createElement("pago");
            texto(doc, pagoEl, "formaPago", pago.formaPago());
            texto(doc, pagoEl, "total", montoTexto(pago.total()));
            if (pago.plazo() != null && !pago.plazo().isBlank()) {
                texto(doc, pagoEl, "plazo", pago.plazo());
            }
            if (pago.unidadTiempo() != null && !pago.unidadTiempo().isBlank()) {
                texto(doc, pagoEl, "unidadTiempo", pago.unidadTiempo());
            }
            pagosEl.appendChild(pagoEl);
        }
        return pagosEl;
    }
}
