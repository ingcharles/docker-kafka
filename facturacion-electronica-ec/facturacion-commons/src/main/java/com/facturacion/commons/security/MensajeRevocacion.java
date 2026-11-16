package com.facturacion.commons.security;

import java.time.Instant;

/**
 * Contenido del mensaje de {@code facturacion.apikey.revocaciones.v1}: texto plano {@code jti|vencimientoEnSegundos}
 * (clave Kafka = jti, para que el tópico se pueda compactar). Texto y no JSON para no atar a todos los servicios a un
 * serializador compartido por dos campos.
 */
public record MensajeRevocacion(String jti, Instant venceEn) {

    public String serializar() {
        return jti + "|" + venceEn.getEpochSecond();
    }

    public static MensajeRevocacion parsear(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Mensaje de revocación vacío");
        }
        int corte = texto.lastIndexOf('|');
        if (corte <= 0 || corte == texto.length() - 1) {
            throw new IllegalArgumentException("Mensaje de revocación mal formado: " + texto);
        }
        try {
            return new MensajeRevocacion(texto.substring(0, corte),
                Instant.ofEpochSecond(Long.parseLong(texto.substring(corte + 1))));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Vencimiento inválido en el mensaje de revocación: " + texto, e);
        }
    }
}
