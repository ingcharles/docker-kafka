package com.facturacion.inventario.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.domain.model.entity.MovimientoKardex;
import com.facturacion.commons.paging.PageResult;

public interface MovimientoKardexRepository {

    Uni<MovimientoKardex> persist(MovimientoKardex movimiento);

    /** El último movimiento registrado es la fuente de verdad del saldo actual (cantidad y costo promedio). */
    Uni<MovimientoKardex> findUltimoByItemId(UUID itemId);

    Uni<PageResult<MovimientoKardex>> findPagedByItemId(UUID itemId, int page, int limit);
}
