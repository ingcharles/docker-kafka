package com.facturacion.inventario.shared.exception;

import com.facturacion.commons.exception.ApiException;
import com.facturacion.inventario.domain.model.enums.TipoCategoria;
import com.facturacion.inventario.domain.model.enums.TipoItem;
import com.facturacion.inventario.shared.error.CodigoError;

/**
 * Se intentó asignar a un ítem una categoría cuyo {@code tipo} no lo admite (p. ej. un ítem PRODUCTO con una categoría
 * {@code tipo=SERVICIO}). Ver {@code ItemService} para por qué {@code Item.tipo} existe de forma independiente de
 * {@code Categoria.tipo} en primer lugar. 400, {@link CodigoError#CATEGORIA_INCOMPATIBLE}.
 */
public class CategoriaIncompatibleException extends ApiException {

    public CategoriaIncompatibleException(TipoItem tipoItem, TipoCategoria tipoCategoria) {
        super(CodigoError.CATEGORIA_INCOMPATIBLE, tipoCategoria, tipoItem);
    }
}
