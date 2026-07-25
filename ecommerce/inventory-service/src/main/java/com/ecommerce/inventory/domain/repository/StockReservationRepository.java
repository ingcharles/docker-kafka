package com.ecommerce.inventory.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.ecommerce.inventory.domain.model.entity.StockReservation;

/**
 * Puerto (interfaz) para la persistencia de StockReservation, siguiendo
 * arquitectura hexagonal. La implementación concreta (el adaptador) vive en
 * {@code infrastructure.persistence}, usando Panache.
 */
public interface StockReservationRepository {

    Uni<StockReservation> persist(StockReservation reservation);

    Uni<List<StockReservation>> findByOrderId(UUID orderId);

    Uni<List<StockReservation>> findByProductId(UUID productId);

    /** Marca como RELEASED (liberada) cada fila que esté en estado RESERVED para esta orden. */
    Uni<Void> releaseAllForOrder(UUID orderId);
}
