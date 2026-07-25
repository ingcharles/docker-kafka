package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.event.EmisorEventMessage;
import com.facturacion.comprobantes.domain.model.entity.EmisorLocal;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.repository.EmisorLocalRepository;

/**
 * Mantiene al día la vista materializada {@code emisor_local} — no hay
 * inbox de idempotencia aquí a propósito: un upsert por id es naturalmente
 * idempotente (reprocesar el mismo evento dos veces dejaría el mismo
 * resultado), así que agregar processed_message solo sería overhead extra.
 */
@ApplicationScoped
public class EmisorEventConsumer {

    private final EmisorLocalRepository repository;

    @Inject
    public EmisorEventConsumer(EmisorLocalRepository repository) {
        this.repository = repository;
    }

    @Incoming("emisor-events-in")
    @WithTransaction
    public Uni<Void> onEvent(EmisorEventMessage evento) {
        EmisorLocal emisor = new EmisorLocal(
            evento.emisorId(),
            evento.ruc(),
            evento.razonSocial(),
            evento.nombreComercial(),
            evento.direccionMatriz(),
            evento.obligadoContabilidad(),
            evento.contribuyenteEspecial(),
            evento.numeroContribuyenteEspecial(),
            evento.esGranContribuyente(),
            evento.numeroResolucionGranContribuyente(),
            evento.vendeCombustibles(),
            AmbienteSri.valueOf(evento.ambiente()),
            evento.establecimientoDefecto(),
            evento.puntoEmisionDefecto(),
            evento.activo(),
            evento.occurredAt());
        return repository.upsert(emisor).replaceWithVoid();
    }
}
