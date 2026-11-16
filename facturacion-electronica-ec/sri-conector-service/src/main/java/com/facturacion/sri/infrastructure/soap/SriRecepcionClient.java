package com.facturacion.sri.infrastructure.soap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import com.facturacion.sri.domain.model.enums.AmbienteSri;
import com.facturacion.sri.domain.port.RecepcionResultado;
import com.facturacion.sri.domain.port.RecepcionSri;

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
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @ConfigProperty(name = "sri.recepcion.url.pruebas")
    String urlPruebas;

    @ConfigProperty(name = "sri.recepcion.url.produccion")
    String urlProduccion;

    @ConfigProperty(name = "sri.url.hosts-permitidos", defaultValue = "*.sri.gob.ec")
    java.util.List<String> hostsPermitidos;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    /**
     * Offloading a worker thread — es I/O de red bloqueante (HttpClient.send, no sendAsync).
     *
     * @param urlOverride ajuste opcional por empresa (clave {@code sri.recepcion.url} de
     *                    {@code configuracion_empresa}) — {@code null} o vacío usa la URL fija
     *                    por ambiente ({@code urlPruebas}/{@code urlProduccion}).
     */
    public Uni<RecepcionResultado> validarComprobante(String xmlFirmado, AmbienteSri ambiente, String urlOverride) {
        return Uni.createFrom().item(() -> enviarBloqueante(xmlFirmado, ambiente, urlOverride))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
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
                .timeout(TIMEOUT)
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.infof("SRI Recepción respondió HTTP %d para ambiente %s", response.statusCode(), ambiente);
            return parsear(response.body());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo contactar al WS de Recepción del SRI (" + url + ")", e);
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
