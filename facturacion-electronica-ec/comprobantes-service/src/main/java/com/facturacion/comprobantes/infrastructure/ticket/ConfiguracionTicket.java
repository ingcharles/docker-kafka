package com.facturacion.comprobantes.infrastructure.ticket;

import java.util.Map;

import io.smallrye.config.ConfigMapping;

/**
 * Configuración del ticket ({@code application.properties}): {@code ticket.anchos.<milimetros>=<columnas>} por cada papel
 * soportado y {@code ticket.ancho-por-defecto=<milimetros>} para cuando el cliente no pide uno. Para agregar un papel
 * (p. ej. 112 mm) basta una línea nueva, sin tocar código.
 */
@ConfigMapping(prefix = "ticket")
public interface ConfiguracionTicket {

    /** Caracteres por línea de cada ancho de papel, por milímetros. */
    Map<Integer, Integer> anchos();

    int anchoPorDefecto();
}
