package com.facturacion.comprobantes.infrastructure.idempotencia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.service.EmisionIdempotente;

/** Borra cada cierto tiempo las claves de idempotencia que superaron el plazo de retención. */
@ApplicationScoped
public class PurgaIdempotenciaJob {

    private final EmisionIdempotente emisionIdempotente;

    @Inject
    public PurgaIdempotenciaJob(EmisionIdempotente emisionIdempotente) {
        this.emisionIdempotente = emisionIdempotente;
    }

    @Scheduled(every = "${comprobantes.idempotencia.purga-cada}", delayed = "${outbox.relay.inicio-diferido}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @WithTransaction
    public Uni<Void> purgarVencidas() {
        return emisionIdempotente.purgarVencidas().replaceWithVoid();
    }
}
