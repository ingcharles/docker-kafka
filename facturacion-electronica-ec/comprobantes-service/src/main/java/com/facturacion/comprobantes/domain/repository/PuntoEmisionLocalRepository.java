package com.facturacion.comprobantes.domain.repository;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.PuntoEmisionLocal;

public interface PuntoEmisionLocalRepository {

    /** Upsert por (ruc, establecimientoCodigo, codigo) — se llama al recibir PuntoEmisionRegistrado/Actualizado desde Kafka. */
    Uni<PuntoEmisionLocal> upsert(PuntoEmisionLocal puntoEmision);

    Uni<PuntoEmisionLocal> findByRucAndEstablecimientoCodigoAndCodigo(String ruc, String establecimientoCodigo,
            String codigo);
}
