package com.facturacion.identidad.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;
import com.facturacion.identidad.domain.model.enums.Rol;

public interface UsuarioEmpresaRolRepository {

    Uni<UsuarioEmpresaRol> persist(UsuarioEmpresaRol asociacion);

    Uni<UsuarioEmpresaRol> update(UsuarioEmpresaRol asociacion);

    Uni<UsuarioEmpresaRol> findByUsuarioIdAndRuc(UUID usuarioId, String ruc);

    Uni<List<UsuarioEmpresaRol>> findByUsuarioId(UUID usuarioId);

    /** Todos los usuarios asociados a un ruc — miembros de esa empresa. */
    Uni<List<UsuarioEmpresaRol>> findByRuc(String ruc);

    /** Cuántos usuarios tienen ese rol en ese ruc — usado para no dejar una empresa sin ningún ADMIN. */
    Uni<Long> contarPorRucYRol(String ruc, Rol rol);

    Uni<Void> delete(UsuarioEmpresaRol asociacion);
}
