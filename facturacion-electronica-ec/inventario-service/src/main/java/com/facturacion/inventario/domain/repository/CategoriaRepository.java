package com.facturacion.inventario.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.inventario.domain.model.entity.Categoria;

public interface CategoriaRepository {

    Uni<Categoria> persist(Categoria categoria);

    Uni<Categoria> update(Categoria categoria);

    Uni<Categoria> findById(UUID id);

    /** Categorías de una empresa. */
    Uni<PageResult<Categoria>> findPaged(int page, int limit, String ruc);
}
