package com.facturacion.identidad.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.application.event.EmisorEventMessage;
import com.facturacion.identidad.domain.model.entity.EmisorLocal;
import com.facturacion.identidad.domain.repository.EmisorLocalRepository;

/**
 * Mantiene al día la vista materializada {@code emisor_local} — sin inbox
 * de idempotencia porque un upsert por id ya es naturalmente idempotente
 * (mismo criterio que {@code ItemEventConsumer} en comprobantes-service).
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
        EmisorLocal emisor = new EmisorLocal();
        emisor.setId(evento.emisorId());
        emisor.setRuc(evento.ruc());
        emisor.setRazonSocial(evento.razonSocial());
        emisor.setActivo(evento.activo());
        emisor.setActualizadoEn(evento.occurredAt());
        return repository.upsert(emisor).replaceWithVoid();
    }
}
