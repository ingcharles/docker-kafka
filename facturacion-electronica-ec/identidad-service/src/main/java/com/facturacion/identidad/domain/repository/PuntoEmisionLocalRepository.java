package com.facturacion.identidad.domain.repository;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.PuntoEmisionLocal;

public interface PuntoEmisionLocalRepository {

    /** Upsert por (ruc, establecimientoCodigo, codigo) — se llama al recibir PuntoEmisionRegistrado/Actualizado desde Kafka. */
    Uni<Void> insertarOActualizar(PuntoEmisionLocal puntoEmision);

    Uni<PuntoEmisionLocal> findByRucAndEstablecimientoCodigoAndCodigo(String ruc, String establecimientoCodigo,
            String codigo);
}
