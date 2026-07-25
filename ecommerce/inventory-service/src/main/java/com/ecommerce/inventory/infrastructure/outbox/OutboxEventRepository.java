package com.ecommerce.inventory.infrastructure.outbox;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepositoryBase<OutboxEventEntity, java.util.UUID> {

    /**
     * Trae, en lotes acotados, las filas pendientes de publicar (las más
     * antiguas primero). El límite de tamaño existe para que una sola
     * ejecución del relay no se quede procesando indefinidamente si se
     * acumula un backlog grande — deja que el siguiente "tick" del
     * {@code @Scheduled} recoja el resto.
     */
    public Uni<List<OutboxEventEntity>> findUnpublishedBatch(int batchSize) {
        return find("publishedAt is null", Sort.ascending("createdAt"))
            .page(Page.ofSize(batchSize))
            .list();
    }
}
