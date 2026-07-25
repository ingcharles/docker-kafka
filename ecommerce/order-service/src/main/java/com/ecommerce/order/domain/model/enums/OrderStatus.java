package com.ecommerce.order.domain.model.enums;

/**
 * Ciclo de vida de una {@link com.ecommerce.order.domain.model.entity.Order}.
 * Las transiciones ocurren únicamente a través de los comandos de la saga
 * (ConfirmOrder / CancelOrder, consumidos desde el tópico
 * {@code ecommerce.order.commands.v1}), nunca por una mutación directa vía
 * REST — el orquestador de la saga es el único que escribe este campo
 * después de que la orden fue creada.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
