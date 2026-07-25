package com.facturacion.inventario.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.domain.model.entity.ReservaStock;
import com.facturacion.inventario.domain.model.enums.EstadoReserva;

public interface ReservaStockRepository {

    Uni<ReservaStock> persist(ReservaStock reserva);

    Uni<List<ReservaStock>> findByComprobanteIdAndEstado(UUID comprobanteId, EstadoReserva estado);

    Uni<Integer> actualizarEstado(UUID id, EstadoReserva nuevoEstado, EstadoReserva estadoEsperado);
}
