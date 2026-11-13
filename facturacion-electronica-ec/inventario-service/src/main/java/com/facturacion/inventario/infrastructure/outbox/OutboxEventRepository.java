package com.facturacion.inventario.infrastructure.outbox;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepositoryBase<OutboxEventEntity, UUID> {

    /**
     * Próximo lote de eventos sin publicar, con {@code FOR UPDATE SKIP LOCKED}: con 2 o más instancias del servicio,
     * cada una toma lotes distintos en vez de publicar los mismos eventos. Exige transacción (los relays son
     * {@code @WithTransaction}): el bloqueo dura hasta el commit. Es consulta nativa porque Hibernate Reactive Panache
     * no soporta hints de bloqueo ("Hints not supported yet": {@code withHint} rompía el relay en tiempo de ejecución).
     */
    public Uni<List<OutboxEventEntity>> findUnpublishedBatch(int batchSize) {
        return Panache.getSession().chain(session -> session
            .createNativeQuery("select * from outbox_event where published_at is null "
                + "order by created_at limit ?1 for update skip locked", OutboxEventEntity.class)
            .setParameter(1, batchSize)
            .getResultList());
    }
}
