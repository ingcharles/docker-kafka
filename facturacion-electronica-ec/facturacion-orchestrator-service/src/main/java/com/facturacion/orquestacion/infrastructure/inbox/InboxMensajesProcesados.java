package com.facturacion.orquestacion.infrastructure.inbox;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.orquestacion.domain.port.MensajesProcesados;

/** Adaptador del puerto {@link MensajesProcesados}: tabla de mensajes procesados. */
@ApplicationScoped
public class InboxMensajesProcesados implements MensajesProcesados {

    private final ProcessedMessageRepository repository;

    @Inject
    public InboxMensajesProcesados(ProcessedMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public Uni<Boolean> yaProcesado(UUID mensajeId) {
        return repository.alreadyProcessed(mensajeId);
    }

    @Override
    public Uni<Void> marcarProcesado(UUID mensajeId) {
        return repository.persist(ProcessedMessageEntity.of(mensajeId)).replaceWithVoid();
    }
}
