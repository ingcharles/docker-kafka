package com.facturacion.identidad.domain.repository;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.EmisorLocal;

public interface EmisorLocalRepository {

    /** Upsert por id (emisorId) — se llama al recibir EmisorRegistrado/EmisorActualizado desde Kafka. */
    Uni<EmisorLocal> upsert(EmisorLocal emisor);

    /** Null si no existe ninguna copia local para ese ruc todavía. */
    Uni<EmisorLocal> findByRuc(String ruc);
}
