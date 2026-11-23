package com.facturacion.sri.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

import com.facturacion.sri.infrastructure.persistence.entity.EnvioSriEntity;

@ApplicationScoped
public class EnvioSriPanacheRepository implements PanacheRepositoryBase<EnvioSriEntity, UUID> {

    /**
     * Envíos en estado RECIBIDO sin consultar desde {@code consultadosAntesDe}, con {@code FOR UPDATE SKIP LOCKED} (mismo
     * esquema que el relay del outbox): consulta nativa porque Hibernate Reactive Panache no soporta hints de bloqueo en
     * consultas. Exige transacción.
     */
    public Uni<List<EnvioSriEntity>> findRecibidosParaConsultar(String estado, Instant consultadosAntesDe, int batchSize) {
        return Panache.getSession().chain(session -> session
            .createNativeQuery("select * from envio_sri where estado = ?1 "
                + "and (ultima_consulta_en is null or ultima_consulta_en < ?2) "
                + "order by ultima_consulta_en nulls first, creado_en limit ?3 for update skip locked", EnvioSriEntity.class)
            .setParameter(1, estado)
            .setParameter(2, consultadosAntesDe)
            .setParameter(3, batchSize)
            .getResultList());
    }
}
