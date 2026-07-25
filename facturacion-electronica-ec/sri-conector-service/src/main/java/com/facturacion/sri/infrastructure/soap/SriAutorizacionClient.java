package com.facturacion.sri.infrastructure.soap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import com.facturacion.sri.domain.model.enums.AmbienteSri;

/**
 * Cliente real del Web Service de Autorización del SRI —
 * {@code AutorizacionComprobantesOffline.autorizacionComprobante}, verificado
 * campo por campo contra el WSDL público y con una llamada real de prueba
 * (con {@code numeroComprobantes=0}, ya que la clave usada nunca pasó por
 * Recepción — confirma que el contrato coincide exactamente con el
 * documentado en la ficha técnica v2.33).
 */
@ApplicationScoped
public class SriAutorizacionClient {

    private static final Logger LOG = Logger.getLogger(SriAutorizacionClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @ConfigProperty(name = "sri.autorizacion.url.pruebas")
    String urlPruebas;

    @ConfigProperty(name = "sri.autorizacion.url.produccion")
    String urlProduccion;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public Uni<AutorizacionResultado> consultar(String claveAcceso, AmbienteSri ambiente) {
        return Uni.createFrom().item(() -> consultarBloqueante(claveAcceso, ambiente))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private AutorizacionResultado consultarBloqueante(String claveAcceso, AmbienteSri ambiente) {
        String url = ambiente == AmbienteSri.PRODUCCION ? urlProduccion : urlPruebas;
        String sobre = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\">"
            + "<soap:Body><aut:autorizacionComprobante><claveAccesoComprobante>" + claveAcceso
            + "</claveAccesoComprobante></aut:autorizacionComprobante></soap:Body></soap:Envelope>";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.infof("SRI Autorización respondió HTTP %d para clave %s", response.statusCode(), claveAcceso);
            return parsear(response.body());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo contactar al WS de Autorización del SRI (" + url + ")", e);
        }
    }

    private AutorizacionResultado parsear(String cuerpoXml) throws Exception {
        Document doc = SoapXmlUtil.parsear(cuerpoXml);
        NodeList autorizaciones = doc.getElementsByTagName("autorizacion");
        if (autorizaciones.getLength() == 0) {
            // numeroComprobantes=0: el SRI todavía no tiene nada que decir de esta
            // clave (no llegó a procesarla, o Recepción todavía no la propagó
            // internamente) — se interpreta como "seguir esperando", no como error.
            return new AutorizacionResultado(null, null, null, null, null);
        }

        Element autorizacion = (Element) autorizaciones.item(0);
        String estado = SoapXmlUtil.hijoTexto(autorizacion, "estado");
        String numeroAutorizacion = SoapXmlUtil.hijoTexto(autorizacion, "numeroAutorizacion");
        Instant fechaAutorizacion = SoapXmlUtil.parsearFechaSri(SoapXmlUtil.hijoTexto(autorizacion, "fechaAutorizacion"));
        // El <comprobante> interno llega como texto (ver el porqué en
        // desanidarComprobante) — se desanida antes de guardar para que
        // xml_autorizado quede como XML legible, sin &lt;/&gt;.
        SoapXmlUtil.desanidarComprobante(doc, autorizacion);
        String xmlAutorizacionCompleto = SoapXmlUtil.serializarElemento(autorizacion);

        String motivoRechazo = null;
        if (!"AUTORIZADO".equalsIgnoreCase(estado)) {
            StringBuilder mensajes = new StringBuilder();
            NodeList nodosMensaje = autorizacion.getElementsByTagName("mensaje");
            for (int i = 0; i < nodosMensaje.getLength(); i++) {
                Element mensaje = (Element) nodosMensaje.item(i);
                String texto = SoapXmlUtil.hijoTexto(mensaje, "mensaje");
                if (texto != null) {
                    if (mensajes.length() > 0) {
                        mensajes.append(" | ");
                    }
                    mensajes.append(texto);
                }
            }
            motivoRechazo = mensajes.length() == 0 ? null : mensajes.toString();
        }

        return new AutorizacionResultado(estado, numeroAutorizacion, fechaAutorizacion, xmlAutorizacionCompleto,
            motivoRechazo);
    }
}
