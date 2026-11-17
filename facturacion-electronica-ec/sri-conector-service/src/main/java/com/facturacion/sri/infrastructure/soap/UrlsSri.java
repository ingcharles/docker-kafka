package com.facturacion.sri.infrastructure.soap;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.sri.shared.error.CodigoLog;

import org.jboss.logging.Logger;

/**
 * Valida el override de URL del SRI que una empresa puede guardar en {@code configuracion_empresa}
 * ({@code sri.recepcion.url} / {@code sri.autorizacion.url}). Sin esto, cualquiera con rol ADMIN sobre una empresa
 * podía hacer que el XML firmado (con datos personales de sus clientes) se enviara a un host arbitrario — o a la red
 * interna (SSRF) — y que un servidor falso respondiera "AUTORIZADO".
 * <p>
 * Un override solo se respeta si es {@code https} y su host coincide con alguno de {@code sri.url.hosts-permitidos}
 * (por defecto {@code *.sri.gob.ec}; un patrón {@code *.dominio} acepta el dominio y sus subdominios, {@code *} lo
 * acepta todo y es solo para desarrollo/pruebas con un mock). Si no pasa, se IGNORA con un WARN y se usa la URL
 * oficial del ambiente: el comprobante nunca va a un destino no autorizado.
 */
final class UrlsSri {

    private static final Logger LOG = Logger.getLogger(UrlsSri.class);

    private UrlsSri() {
    }

    /** @return el override si es válido y está permitido; si no (o si no hay), {@code porDefecto}. */
    static String elegir(String override, String porDefecto, List<String> hostsPermitidos) {
        if (override == null || override.isBlank()) {
            return porDefecto;
        }
        if (esPermitida(override.trim(), hostsPermitidos)) {
            return override.trim();
        }
        LogCodificado.warn(LOG, CodigoLog.URL_SRI_IGNORADA, null, override);
        return porDefecto;
    }

    static boolean esPermitida(String url, List<String> hostsPermitidos) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (hostsPermitidos.contains("*")) {
            return uri.getHost() != null && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        for (String patron : hostsPermitidos) {
            String p = patron.trim().toLowerCase(Locale.ROOT);
            if (p.startsWith("*.")) {
                String dominio = p.substring(2);
                if (host.equals(dominio) || host.endsWith("." + dominio)) {
                    return true;
                }
            } else if (host.equals(p)) {
                return true;
            }
        }
        return false;
    }
}
