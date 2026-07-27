package com.facturacion.identidad.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;

public interface UsuarioEmpresaRolRepository {

    Uni<UsuarioEmpresaRol> persist(UsuarioEmpresaRol asociacion);

    Uni<UsuarioEmpresaRol> update(UsuarioEmpresaRol asociacion);

    Uni<UsuarioEmpresaRol> findByUsuarioIdAndRuc(UUID usuarioId, String ruc);

    Uni<List<UsuarioEmpresaRol>> findByUsuarioId(UUID usuarioId);
}
