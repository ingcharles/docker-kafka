package com.facturacion.comprobantes.infrastructure.xml;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import jakarta.enterprise.context.ApplicationScoped;

import org.xml.sax.SAXException;

import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.port.ValidadorXml;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Valida el XML sin firmar de cada comprobante contra el XSD real y más
 * reciente del SRI para su tipo (empaquetado en {@code src/main/resources/xsd},
 * copiado de {@code documentacion/XML y XSD *} del repo) — así un error
 * estructural se detecta acá, no como un rechazo tardío del SRI real (mismo
 * espíritu que las validaciones ya existentes de establecimiento/punto de
 * emisión/itemId, ver {@code ComprobanteService}). Firmar no cambia la
 * estructura del documento ({@code ds:Signature} es {@code minOccurs="0"}
 * en las 6 XSD), así que validar antes de firmar es equivalente a validar
 * el XML final que ve el SRI.
 * <p>
 * Los {@link Schema} se compilan una sola vez al arrancar (compilar un XSD
 * no es gratis) y se reutilizan entre requests — {@link Validator} sí es
 * stateful/no es thread-safe, así que se crea uno nuevo por llamada a
 * {@link #validar}.
 */
@ApplicationScoped
public class XsdValidador implements ValidadorXml {

    private final Map<TipoComprobante, Schema> schemasPorTipo = new EnumMap<>(TipoComprobante.class);

    public XsdValidador() {
        schemasPorTipo.put(TipoComprobante.FACTURA, cargarSchema("factura_V2.1.0.xsd"));
        schemasPorTipo.put(TipoComprobante.NOTA_CREDITO, cargarSchema("NotaCredito_V1.1.0.xsd"));
        schemasPorTipo.put(TipoComprobante.NOTA_DEBITO, cargarSchema("NotaDebito_V1.0.0.xsd"));
        schemasPorTipo.put(TipoComprobante.GUIA_REMISION, cargarSchema("GuiaRemision_V1.1.0.xsd"));
        // v2.0.0: wrapper docsSustento/docSustento estilo ATS, con
        // impuestosDocSustento/retenciones/pagos anidados por documento
        // sustento — ver GeneradorXmlRetencion.
        schemasPorTipo.put(TipoComprobante.RETENCION, cargarSchema("ComprobanteRetencion_V2.0.0.xsd"));
        schemasPorTipo.put(TipoComprobante.LIQUIDACION_COMPRA, cargarSchema("LiquidacionCompra_V1.1.0.xsd"));
    }

    /**
     * Trabajo bloqueante (parsing + validación DOM) — el llamador debe
     * correrlo en el worker pool de Mutiny, mismo criterio que
     * {@code CertificadoService.validarYExtraerMetadata}.
     *
     * @throws ErrorNegocioException si el XML no cumple el XSD del SRI para ese tipo —
     *         el mensaje incluye el motivo SAX exacto (línea/columna + descripción).
     */
    public void validar(TipoComprobante tipo, String xml) {
        Schema schema = schemasPorTipo.get(tipo);
        Validator validator = schema.newValidator();
        try {
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException e) {
            throw new ErrorNegocioException(CodigoError.XML_NO_CUMPLE_XSD, tipo.nombre(), e.getMessage());
        } catch (IOException e) {
            throw new ErrorInternoException(e, CodigoError.XSD_ERROR_DE_ENTRADA_SALIDA);
        }
    }

    private Schema cargarSchema(String archivo) {
        URL recurso = getClass().getClassLoader().getResource("xsd/" + archivo);
        if (recurso == null) {
            throw new ErrorInternoException(CodigoError.XSD_NO_ENCONTRADO, archivo);
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return factory.newSchema(recurso);
        } catch (SAXException e) {
            throw new ErrorInternoException(e, CodigoError.XSD_NO_COMPILABLE, archivo);
        }
    }
}
