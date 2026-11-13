package com.facturacion.commons.paging;

import java.util.List;

/** Una página de resultados de un repositorio (puro: lo usan los puertos del dominio). */
public record PageResult<T>(List<T> items, long total) {
}
