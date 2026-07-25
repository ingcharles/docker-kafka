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
import com.facturacion.comprobantes.domain.model.vo.Impuesto;

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
            : c.getEmisorDireccionMatriz();
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

    /** {@code <infoAdicional>} con el campo "granContribuyente" — Anexo 24, solo cuando el emisor es Gran Contribuyente. */
    static Element construirInfoAdicionalGranContribuyente(Document doc, Comprobante c) {
        Element infoAdicional = doc.createElement("infoAdicional");
        Element campo = doc.createElement("campoAdicional");
        campo.setAttribute("nombre", "granContribuyente");
        campo.setTextContent("Gran Contribuyente " + c.getEmisorNumeroResolucionGranContribuyente());
        infoAdicional.appendChild(campo);
        return infoAdicional;
    }
}
