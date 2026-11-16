package com.facturacion.notificacion.domain.port;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de salida del patrón outbox: registra el evento en la misma transacción de BD que el cambio de negocio. La
 * implementación (tabla outbox + relay a Kafka) es de infraestructura.
 */
public interface EventosSalientes {

    Uni<Void> registrar(EventoSaliente evento);
}
