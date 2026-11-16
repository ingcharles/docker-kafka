package com.facturacion.comprobantes.infrastructure.outbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.port.EventoSaliente;
import com.facturacion.comprobantes.domain.port.EventosSalientes;

/** Adaptador del puerto {@link EventosSalientes}: guarda el evento en la tabla outbox. */
@ApplicationScoped
public class OutboxEventosSalientes implements EventosSalientes {

    private final OutboxEventRepository repository;

    @Inject
    public OutboxEventosSalientes(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Uni<Void> registrar(EventoSaliente evento) {
        return repository.persist(OutboxEventEntity.of(evento.aggregateId(), evento.eventType(), evento.topic(),
            evento.payload())).replaceWithVoid();
    }
}
