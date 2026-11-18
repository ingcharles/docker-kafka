package com.facturacion.sri.infrastructure.soap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.reactive.TrabajoBloqueante;
import com.facturacion.sri.domain.model.enums.AmbienteSri;
import com.facturacion.sri.domain.port.RecepcionResultado;
import com.facturacion.sri.domain.port.RecepcionSri;
import com.facturacion.sri.shared.error.CodigoLog;
import com.facturacion.sri.shared.error.SriError;

/**
 * Cliente real (no simulado) del Web Service de Recepción del SRI —
 * {@code RecepcionComprobantesOffline.validarComprobante}, verificado
 * campo por campo contra el WSDL público
 * (celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl,
 * que coincide exactamente con la ficha técnica v2.33).
 * <p>
 * Se llama con {@code java.net.http.HttpClient} en vez de un stack SOAP
 * (CXF/JAX-WS) — el contrato es simple (un solo parámetro base64Binary) y
 * hacerlo a mano evita depender de generación de código desde el WSDL en
 * build time (que necesitaría acceso a internet durante {@code mvn package}).
 * Mismo criterio que la firma XAdES-BES en firma-electronica-service
 * (JDK puro en vez de una librería externa).
 */
@ApplicationScoped
public class SriRecepcionClient implements RecepcionSri {

    private static final Logger LOG = Logger.getLogger(SriRecepcionClient.class);

    @ConfigProperty(name = "sri.timeout.conexion")
    Duration timeoutConexion;

    @ConfigProperty(name = "sri.timeout.respuesta")
    Duration timeoutRespuesta;

    @ConfigProperty(name = "sri.recepcion.url.pruebas")
    String urlPruebas;

    @ConfigProperty(name = "sri.recepcion.url.produccion")
    String urlProduccion;

    @ConfigProperty(name = "sri.url.hosts-permitidos", defaultValue = "*.sri.gob.ec")
    java.util.List<String> hostsPermitidos;

    private HttpClient httpClient;

    @PostConstruct
    void iniciar() {
        httpClient = HttpClient.newBuilder().connectTimeout(timeoutConexion).build();
    }

    /**
     * Offloading a worker thread — es I/O de red bloqueante (HttpClient.send, no sendAsync).
     *
     * @param urlOverride ajuste opcional por empresa (clave {@code sri.recepcion.url} de
     *                    {@code configuracion_empresa}) — {@code null} o vacío usa la URL fija
     *                    por ambiente ({@code urlPruebas}/{@code urlProduccion}).
     */
    public Uni<RecepcionResultado> validarComprobante(String xmlFirmado, AmbienteSri ambiente, String urlOverride) {
        return TrabajoBloqueante.ejecutar(() -> enviarBloqueante(xmlFirmado, ambiente, urlOverride));
    }

    private RecepcionResultado enviarBloqueante(String xmlFirmado, AmbienteSri ambiente, String urlOverride) {
        String url = UrlsSri.elegir(urlOverride, ambiente == AmbienteSri.PRODUCCION ? urlProduccion : urlPruebas,
            hostsPermitidos);
        String base64Xml = Base64.getEncoder().encodeToString(xmlFirmado.getBytes(StandardCharsets.UTF_8));
        String sobre = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "xmlns:rec=\"http://ec.gob.sri.ws.recepcion\">"
            + "<soap:Body><rec:validarComprobante><xml>" + base64Xml + "</xml></rec:validarComprobante></soap:Body>"
            + "</soap:Envelope>";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeoutRespuesta)
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LogCodificado.info(LOG, CodigoLog.HTTP_RECEPCION, null, response.statusCode(), ambiente);
            return parsear(response.body());
        } catch (Exception e) {
            throw new ErrorInternoException(e, SriError.WS_RECEPCION_NO_DISPONIBLE, url);
        }
    }

    /** Identificadores de error de Recepción que significan "el SRI ya tiene este comprobante" (ver RecepcionResultado). */
    private static final java.util.Set<String> IDENTIFICADORES_YA_REGISTRADA = java.util.Set.of("43", "70");

    static RecepcionResultado parsear(String cuerpoXml) throws Exception {
        Document doc = SoapXmlUtil.parsear(cuerpoXml);
        String estado = SoapXmlUtil.obtenerTexto(doc, "estado");

        StringBuilder mensajes = new StringBuilder();
        boolean claveYaRegistrada = false;
        NodeList nodosMensaje = doc.getElementsByTagName("mensaje");
        for (int i = 0; i < nodosMensaje.getLength(); i++) {
            Element mensaje = (Element) nodosMensaje.item(i);
            String texto = SoapXmlUtil.obtenerTextoHijo(mensaje, "mensaje");
            String infoAdicional = SoapXmlUtil.obtenerTextoHijo(mensaje, "informacionAdicional");
            String identificador = SoapXmlUtil.obtenerTextoHijo(mensaje, "identificador");
            // Set.of(...).contains(null) lanza NullPointerException: un mensaje sin identificador es válido.
            claveYaRegistrada |= identificador != null && IDENTIFICADORES_YA_REGISTRADA.contains(identificador);
            if (texto != null) {
                if (mensajes.length() > 0) {
                    mensajes.append(" | ");
                }
                mensajes.append(texto);
                if (infoAdicional != null && !infoAdicional.isBlank()) {
                    mensajes.append(" (").append(infoAdicional).append(")");
                }
            }
        }
        return new RecepcionResultado(estado, mensajes.length() == 0 ? null : mensajes.toString(),
            "DEVUELTA".equalsIgnoreCase(estado) && claveYaRegistrada);
    }
}
