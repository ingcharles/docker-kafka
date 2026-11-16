package com.facturacion.inventario.domain.repository;

import java.math.BigDecimal;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.inventario.domain.model.entity.Item;
import com.facturacion.inventario.domain.model.enums.TipoItem;

public interface ItemRepository {

    Uni<Item> persist(Item item);

    Uni<Item> update(Item item);

    Uni<Item> findById(UUID id);

    Uni<PageResult<Item>> findPaged(int page, int limit, TipoItem tipo, UUID categoriaId, String ruc);

    /** UPDATE atómico: solo reserva si hay suficiente disponible. Retorna filas afectadas (0 o 1). */
    Uni<Integer> tryReservar(UUID itemId, BigDecimal cantidad);

    /** La venta se confirmó: el stock ya salió en la reserva, ahora deja de estar "reservado". */
    Uni<Integer> confirmarReserva(UUID itemId, BigDecimal cantidad);

    /** La reserva se deshace: el stock vuelve a estar disponible. */
    Uni<Integer> liberarReserva(UUID itemId, BigDecimal cantidad);

    /** Entradas de kardex y ajustes positivos: suma directamente a disponible. */
    Uni<Integer> incrementarDisponible(UUID itemId, BigDecimal cantidad);

    /** Ajustes negativos (merma, conteo físico): atómico, igual que tryReservar. */
    Uni<Integer> decrementarDisponible(UUID itemId, BigDecimal cantidad);
}
