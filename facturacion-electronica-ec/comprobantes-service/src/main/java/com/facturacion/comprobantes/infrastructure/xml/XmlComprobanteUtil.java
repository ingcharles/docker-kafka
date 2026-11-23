package com.facturacion.comprobantes.infrastructure.xml;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    static Document crearDocumento() throws Exception {
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

    static void agregarTexto(Document doc, Element padre, String etiqueta, String valor) {
        Element elemento = doc.createElement(etiqueta);
        elemento.setTextContent(valor);
        padre.appendChild(elemento);
    }

    static String formatearMonto(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** dirEstablecimiento es obligatorio en NC/ND/Retención pero este servicio no rastrea establecimientos aparte — se usa la matriz como valor por defecto. */
    static String obtenerDireccionEstablecimiento(Comprobante c) {
        return c.getDireccionEstablecimiento() != null && !c.getDireccionEstablecimiento().isBlank()
            ? c.getDireccionEstablecimiento()
            : c.getEmpresaDireccionMatriz();
    }

    /** {@code <detalle>} completo (código, descripción, cantidad, precio, descuento, impuestos) — Factura/Nota de Crédito/Liquidación de Compra comparten esta forma. */
    static Element construirDetalle(Document doc, DetalleComprobante d) {
        Element detalle = doc.createElement("detalle");
        agregarTexto(doc, detalle, "codigoPrincipal", d.getCodigoPrincipal());
        if (d.getCodigoAuxiliar() != null && !d.getCodigoAuxiliar().isBlank()) {
            agregarTexto(doc, detalle, "codigoAuxiliar", d.getCodigoAuxiliar());
        }
        agregarTexto(doc, detalle, "descripcion", d.getDescripcion());
        agregarTexto(doc, detalle, "cantidad", d.getCantidad().toPlainString());
        agregarTexto(doc, detalle, "precioUnitario", formatearMonto(d.getPrecioUnitario()));
        if (d.getPrecioSinSubsidio() != null) {
            agregarTexto(doc, detalle, "precioSinSubsidio", formatearMonto(d.getPrecioSinSubsidio()));
        }
        agregarTexto(doc, detalle, "descuento", formatearMonto(d.getDescuento() == null ? BigDecimal.ZERO : d.getDescuento()));
        agregarTexto(doc, detalle, "precioTotalSinImpuesto", formatearMonto(d.calcularPrecioTotalSinImpuesto()));
        if (d.getDetallesAdicionales() != null && !d.getDetallesAdicionales().isEmpty()) {
            Element adicionales = doc.createElement("detallesAdicionales");
            for (CampoAdicional adicional : d.getDetallesAdicionales()) {
                Element detAdicional = doc.createElement("detAdicional");
                detAdicional.setAttribute("nombre", adicional.nombre());
                detAdicional.setAttribute("valor", adicional.valor());
                adicionales.appendChild(detAdicional);
            }
            detalle.appendChild(adicionales);
        }

        Element impuestos = doc.createElement("impuestos");
        for (Impuesto imp : d.getImpuestos()) {
            Element impuesto = doc.createElement("impuesto");
            agregarTexto(doc, impuesto, "codigo", imp.codigo());
            agregarTexto(doc, impuesto, "codigoPorcentaje", imp.codigoPorcentaje());
            agregarTexto(doc, impuesto, "tarifa", imp.tarifa().toPlainString());
            agregarTexto(doc, impuesto, "baseImponible", formatearMonto(imp.baseImponible()));
            agregarTexto(doc, impuesto, "valor", formatearMonto(imp.valor()));
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
     * {@code <agenteRetencion>} (Anexo 21: número de resolución sin ceros a la izquierda) y {@code <contribuyenteRimpe>}
     * (Anexo 22: leyenda del régimen), en ese orden y al final de {@code <infoTributaria>}. Solo se escriben los que
     * aplican a la empresa.
     */
    static void agregarAgenteYRegimen(Document doc, Element info, Comprobante c) {
        if (c.getEmpresaAgenteRetencion() != null && !c.getEmpresaAgenteRetencion().isBlank()) {
            agregarTexto(doc, info, "agenteRetencion", c.getEmpresaAgenteRetencion());
        }
        if (c.getEmpresaRegimenRimpe() != null) {
            agregarTexto(doc, info, "contribuyenteRimpe", c.getEmpresaRegimenRimpe().leyenda());
        }
    }

    /**
     * {@code <infoAdicional>} completo: el campo "Gran Contribuyente" (Anexo
     * 24: nombre "Gran Contribuyente" y, como valor, el número de resolución; solo si la empresa es Gran Contribuyente), luego "RUC Proveedor"
     * (Anexo 26, Res. NAC-DGERCGC26-00000027 — solo cuando la empresa
     * configuró {@code sri.proveedor.ruc} en {@code configuracion_empresa},
     * ver {@code ComprobanteService.emitir}) — ambos calculados por el
     * sistema, no algo que el llamador mande — seguidos de cada
     * {@code campoAdicional} que haya venido en el request. Devuelve
     * {@code null} si no hay nada que agregar (el tag completo es opcional
     * en el XSD del SRI).
     */
    static Element construirInfoAdicional(Document doc, Comprobante c) {
        boolean hayGranContribuyente = c.isEmpresaEsGranContribuyente()
            && c.getEmpresaNumeroResolucionGranContribuyente() != null
            && !c.getEmpresaNumeroResolucionGranContribuyente().isBlank();
        boolean hayProveedorRuc = c.getProveedorRuc() != null && !c.getProveedorRuc().isBlank();
        boolean hayCamposAdicionales = c.getCamposAdicionales() != null && !c.getCamposAdicionales().isEmpty();
        if (!hayGranContribuyente && !hayProveedorRuc && !hayCamposAdicionales) {
            return null;
        }
        Element infoAdicional = doc.createElement("infoAdicional");
        if (hayGranContribuyente) {
            Element campo = doc.createElement("campoAdicional");
            campo.setAttribute("nombre", "Gran Contribuyente");
            campo.setTextContent(c.getEmpresaNumeroResolucionGranContribuyente());
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
            agregarTexto(doc, pagoEl, "formaPago", pago.formaPago());
            agregarTexto(doc, pagoEl, "total", formatearMonto(pago.total()));
            if (pago.plazo() != null && !pago.plazo().isBlank()) {
                agregarTexto(doc, pagoEl, "plazo", pago.plazo());
            }
            if (pago.unidadTiempo() != null && !pago.unidadTiempo().isBlank()) {
                agregarTexto(doc, pagoEl, "unidadTiempo", pago.unidadTiempo());
            }
            pagosEl.appendChild(pagoEl);
        }
        return pagosEl;
    }
}
