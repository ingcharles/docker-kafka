package com.ecommerce.order.domain.model.vo;

import java.util.List;

/**
 * Resultado de paginación a nivel de repositorio. La capa de aplicación lo
 * convierte luego a {@code PagedResponse} (el DTO que sí se expone en la
 * respuesta REST).
 */
public record PageResult<T>(List<T> items, long total) {
}
