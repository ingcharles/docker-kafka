package com.facturacion.firma.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.firma.domain.model.entity.Empresa;

public interface EmpresaRepository {

    Uni<Empresa> persist(Empresa empresa);

    Uni<Empresa> update(Empresa empresa);

    /** Guarda (o quita, con {@code null}) el logo; no toca ningún otro dato de la empresa. */
    Uni<Void> actualizarLogo(UUID id, byte[] logo, String tipo);

    Uni<Empresa> findById(UUID id);

    Uni<Empresa> findByRuc(String ruc);

    Uni<Boolean> existsByRuc(String ruc);

    Uni<PageResult<Empresa>> findPaged(int page, int limit);
}
