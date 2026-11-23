package com.facturacion.sri.infrastructure.soap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
import com.facturacion.sri.domain.port.AutorizacionResultado;
import com.facturacion.sri.domain.port.AutorizacionSri;
import com.facturacion.sri.shared.error.CodigoLog;
import com.facturacion.sri.shared.error.SriError;

/**
 * Cliente real del Web Service de Autorización del SRI —
 * {@code AutorizacionComprobantesOffline.autorizacionComprobante}, verificado
 * campo por campo contra el WSDL público y con una llamada real de prueba
 * (con {@code numeroComprobantes=0}, ya que la clave usada nunca pasó por
 * Recepción — confirma que el contrato coincide exactamente con el
 * documentado en la ficha técnica v2.33).
 */
@ApplicationScoped
public class SriAutorizacionClient implements AutorizacionSri {

    private static final Logger LOG = Logger.getLogger(SriAutorizacionClient.class);

    @ConfigProperty(name = "sri.timeout.conexion")
    Duration timeoutConexion;

    @ConfigProperty(name = "sri.timeout.respuesta")
    Duration timeoutRespuesta;

    @ConfigProperty(name = "sri.autorizacion.url.pruebas")
    String urlPruebas;

    @ConfigProperty(name = "sri.autorizacion.url.produccion")
    String urlProduccion;

    @ConfigProperty(name = "sri.url.hosts-permitidos", defaultValue = "*.sri.gob.ec")
    List<String> hostsPermitidos;

    private HttpClient httpClient;

    @PostConstruct
    void iniciar() {
        httpClient = HttpClient.newBuilder().connectTimeout(timeoutConexion).build();
    }

    /**
     * @param urlOverride ajuste opcional por empresa (clave {@code sri.autorizacion.url} de
     *                    {@code configuracion_empresa}) — {@code null} o vacío usa la URL fija
     *                    por ambiente ({@code urlPruebas}/{@code urlProduccion}).
     */
    public Uni<AutorizacionResultado> consultar(String claveAcceso, AmbienteSri ambiente, String urlOverride) {
        return TrabajoBloqueante.ejecutar(() -> consultarBloqueante(claveAcceso, ambiente, urlOverride));
    }

    private AutorizacionResultado consultarBloqueante(String claveAcceso, AmbienteSri ambiente, String urlOverride) {
        String url = UrlsSri.elegir(urlOverride, ambiente == AmbienteSri.PRODUCCION ? urlProduccion : urlPruebas,
            hostsPermitidos);
        String sobre = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\">"
            + "<soap:Body><aut:autorizacionComprobante><claveAccesoComprobante>" + claveAcceso
            + "</claveAccesoComprobante></aut:autorizacionComprobante></soap:Body></soap:Envelope>";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeoutRespuesta)
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LogCodificado.info(LOG, CodigoLog.HTTP_AUTORIZACION, "claveAcceso=" + claveAcceso, response.statusCode());
            return parsear(response.body());
        } catch (Exception e) {
            throw new ErrorInternoException(e, SriError.WS_AUTORIZACION_NO_DISPONIBLE, url);
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
        String estado = SoapXmlUtil.obtenerTextoHijo(autorizacion, "estado");
        String numeroAutorizacion = SoapXmlUtil.obtenerTextoHijo(autorizacion, "numeroAutorizacion");
        Instant fechaAutorizacion = SoapXmlUtil.parsearFechaSri(SoapXmlUtil.obtenerTextoHijo(autorizacion, "fechaAutorizacion"));
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
                String texto = SoapXmlUtil.obtenerTextoHijo(mensaje, "mensaje");
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
