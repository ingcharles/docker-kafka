package com.facturacion.firma.domain.port;

import java.util.UUID;

/** Evento que el servicio quiere publicar en Kafka; el outbox transaccional lo guarda y un relay lo envía. */
public record EventoSaliente(UUID aggregateId, String eventType, String topic, String payload) {
}
