package com.facturacion.inventario.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.domain.model.entity.Categoria;
import com.facturacion.inventario.domain.model.vo.PageResult;

public interface CategoriaRepository {

    Uni<Categoria> persist(Categoria categoria);

    Uni<Categoria> update(Categoria categoria);

    Uni<Categoria> findById(UUID id);

    Uni<PageResult<Categoria>> findPaged(int page, int limit);
}
