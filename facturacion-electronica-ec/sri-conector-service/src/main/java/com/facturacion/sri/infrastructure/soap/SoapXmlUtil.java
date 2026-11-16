package com.facturacion.sri.infrastructure.soap;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** Parseo/serialización de las respuestas SOAP del SRI, compartido entre {@link SriRecepcionClient} y {@link SriAutorizacionClient}. */
final class SoapXmlUtil {

    private SoapXmlUtil() {
    }

    static Document parsear(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        // La respuesta viene de un servidor externo (y la URL puede configurarse por empresa): nunca debe poder
        // declarar un DOCTYPE ni resolver entidades externas (XXE). Una respuesta SOAP legítima no los usa.
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    /** Primer elemento con esa etiqueta en todo el documento (namespaces no importan acá: el SRI no los usa para calificar los hijos). */
    static String obtenerTexto(Document doc, String etiqueta) {
        NodeList nodos = doc.getElementsByTagName(etiqueta);
        return nodos.getLength() == 0 ? null : nodos.item(0).getTextContent().trim();
    }

    /** Texto del primer hijo directo de {@code padre} con esa etiqueta, o null si no existe. */
    static String obtenerTextoHijo(Element padre, String etiqueta) {
        NodeList hijos = padre.getElementsByTagName(etiqueta);
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo.getParentNode() == padre) {
                String texto = hijo.getTextContent();
                return texto == null ? null : texto.trim();
            }
        }
        return null;
    }

    static Instant parsearFechaSri(String fechaIso) {
        if (fechaIso == null || fechaIso.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(fechaIso.trim()).toInstant();
    }

    /**
     * El SRI manda {@code <comprobante>} como {@code xs:string} (Tabla del
     * WSDL) — su contenido es el XML firmado completo, pero como *texto*, no
     * como hijos reales del elemento. Guardarlo tal cual produciría
     * {@code &lt;factura&gt;...} al re-serializar (cualquier {@code <}/{@code >}
     * dentro de un nodo de texto XML se tiene que escapar, es la única forma
     * válida de representarlo). Para que {@code xml_autorizado} quede legible
     * como XML real (mismo criterio que {@code xml_firmado}, que se guarda
     * sin envolver), se re-parsea ese texto como su propio documento y se
     * reemplaza el nodo de texto por el elemento raíz real — así
     * {@code <comprobante>} queda con hijos XML genuinos en vez de texto escapado.
     */
    static void desanidarComprobante(Document doc, Element autorizacion) throws Exception {
        NodeList comprobantes = autorizacion.getElementsByTagName("comprobante");
        if (comprobantes.getLength() == 0) {
            return;
        }
        Element comprobanteElemento = (Element) comprobantes.item(0);
        String xmlInterno = comprobanteElemento.getTextContent();
        if (xmlInterno == null || xmlInterno.isBlank()) {
            return;
        }
        Document docInterno = parsear(xmlInterno);
        Node raizImportada = doc.importNode(docInterno.getDocumentElement(), true);

        while (comprobanteElemento.hasChildNodes()) {
            comprobanteElemento.removeChild(comprobanteElemento.getFirstChild());
        }
        comprobanteElemento.appendChild(raizImportada);
    }

    static String serializarElemento(Element elemento) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(elemento), new StreamResult(writer));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(writer.toString().getBytes(StandardCharsets.UTF_8));
        return out.toString(StandardCharsets.UTF_8);
    }
}
