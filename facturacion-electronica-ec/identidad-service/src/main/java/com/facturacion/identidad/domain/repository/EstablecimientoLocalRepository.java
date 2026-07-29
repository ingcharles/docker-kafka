package com.facturacion.identidad.domain.repository;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.EstablecimientoLocal;

public interface EstablecimientoLocalRepository {

    /** Upsert por (ruc, codigo) — se llama al recibir EstablecimientoRegistrado/Actualizado desde Kafka. */
    Uni<Void> upsert(EstablecimientoLocal establecimiento);

    Uni<EstablecimientoLocal> findByRucAndCodigo(String ruc, String codigo);
}
