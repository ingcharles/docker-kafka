package com.facturacion.inventario.shared.exception;

import com.facturacion.inventario.domain.model.enums.TipoCategoria;
import com.facturacion.inventario.domain.model.enums.TipoItem;

/**
 * Se intentó asignar a un ítem una categoría cuyo {@code tipo} no lo admite
 * (p. ej. un ítem PRODUCTO con una categoría {@code tipo=SERVICIO}). Ver
 * {@code ItemService} para por qué {@code Item.tipo} existe de forma
 * independiente de {@code Categoria.tipo} en primer lugar.
 */
public class CategoriaIncompatibleException extends RuntimeException {

    public CategoriaIncompatibleException(TipoItem tipoItem, TipoCategoria tipoCategoria) {
        super("La categoría es de tipo " + tipoCategoria + " y no admite ítems de tipo " + tipoItem);
    }
}
