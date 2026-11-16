package com.facturacion.commons.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Carga en {@link RevocacionApiKeys} cada revocación que publica identidad-service. Cada servicio declara el canal
 * {@code apikey-revocaciones-in} en su application.properties con un {@code group.id} propio de la instancia y
 * {@code auto.offset.reset=earliest}: al arrancar relee el tópico completo, así que una instancia nueva también
 * conoce las revocaciones anteriores.
 */
@ApplicationScoped
public class RevocacionApiKeysConsumer {

    private static final Logger LOG = Logger.getLogger(RevocacionApiKeysConsumer.class);

    private final RevocacionApiKeys revocaciones;

    @Inject
    public RevocacionApiKeysConsumer(RevocacionApiKeys revocaciones) {
        this.revocaciones = revocaciones;
    }

    @Incoming("apikey-revocaciones-in")
    public void registrarRevocacion(String mensaje) {
        try {
            MensajeRevocacion revocacion = MensajeRevocacion.parsear(mensaje);
            revocaciones.revocar(revocacion.jti(), revocacion.venceEn());
        } catch (IllegalArgumentException e) {
            // Un mensaje ilegible no debe detener el canal: se descarta y queda en el log.
            LOG.warnf("Revocación ignorada: %s", e.getMessage());
        }
    }
}
