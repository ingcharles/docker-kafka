package com.facturacion.firma.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.commons.paging.PageResult;

public interface EmpresaRepository {

    Uni<Empresa> persist(Empresa empresa);

    Uni<Empresa> update(Empresa empresa);

    Uni<Empresa> findById(UUID id);

    Uni<Empresa> findByRuc(String ruc);

    Uni<Boolean> existsByRuc(String ruc);

    Uni<PageResult<Empresa>> findPaged(int page, int limit);
}
