package com.facturacion.comprobantes.domain.repository;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.EstablecimientoLocal;

public interface EstablecimientoLocalRepository {

    /** Upsert por (ruc, codigo) — se llama al recibir EstablecimientoRegistrado/Actualizado desde Kafka. */
    Uni<EstablecimientoLocal> insertarOActualizar(EstablecimientoLocal establecimiento);

    Uni<EstablecimientoLocal> findByRucAndCodigo(String ruc, String codigo);
}
