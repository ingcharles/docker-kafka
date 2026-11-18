package com.facturacion.orquestacion.infrastructure.inbox;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ProcessedMessageRepository implements PanacheRepositoryBase<ProcessedMessageEntity, UUID> {

    public Uni<Boolean> esMensajeProcesado(UUID messageId) {
        return count("messageId", messageId).map(count -> count > 0);
    }
}
