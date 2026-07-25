package com.facturacion.comprobantes.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.EmisorLocal;

public interface EmisorLocalRepository {

    /** Upsert por id — se llama al recibir EmisorRegistrado/EmisorActualizado desde Kafka. */
    Uni<EmisorLocal> upsert(EmisorLocal emisor);

    Uni<EmisorLocal> findById(UUID id);
}
