package com.facturacion.firma.infrastructure.outbox;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepositoryBase<OutboxEventEntity, UUID> {

    public Uni<List<OutboxEventEntity>> findUnpublishedBatch(int batchSize) {
        return find("publishedAt is null", Sort.ascending("createdAt"))
            .page(Page.ofSize(batchSize))
            .list();
    }
}
