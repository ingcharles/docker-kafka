package com.ecommerce.order.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.ecommerce.order.domain.model.entity.Order;
import com.ecommerce.order.domain.model.enums.OrderStatus;
import com.ecommerce.order.domain.model.vo.PageResult;

/**
 * Puerto (interfaz) para la persistencia de Order, siguiendo arquitectura
 * hexagonal. La implementación concreta (el adaptador) vive en
 * {@code infrastructure.persistence}, usando Panache.
 */
public interface OrderRepository {

    Uni<Order> persist(Order order);

    Uni<Order> findById(UUID id);

    Uni<PageResult<Order>> findPaged(int page, int limit, String customerId, OrderStatus status);

    /**
     * Aplica una transición de estado únicamente si la orden se encuentra
     * actualmente en uno de los estados indicados en {@code allowedFrom} —
     * es un "compare-and-set" atómico resuelto directamente a nivel de SQL
     * (una sola sentencia {@code UPDATE ... WHERE status IN (...)}), en vez
     * de hacerlo con un patrón "leer, verificar en Java, y luego escribir",
     * que sería vulnerable a condiciones de carrera si dos solicitudes
     * llegan casi al mismo tiempo.
     *
     * @return la orden ya actualizada, o vacío si la orden no estaba en
     * ninguno de los estados de origen permitidos
     */
    Uni<Order> updateStatus(UUID id, OrderStatus to, OrderStatus... allowedFrom);
}
