package com.facturacion.identidad.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.Usuario;

public interface UsuarioRepository {

    Uni<Usuario> persist(Usuario usuario);

    /** Actualiza los campos mutables de un usuario ya existente (perfil, contraseña) — no crea uno nuevo. */
    Uni<Usuario> update(Usuario usuario);

    Uni<Usuario> findById(UUID id);

    Uni<Usuario> findByEmail(String email);

    Uni<Boolean> existsByEmail(String email);

    /** Una sola consulta para varios ids — usado al enriquecer un listado de miembros de una empresa, evita N+1. */
    Uni<List<Usuario>> findByIds(List<UUID> ids);
}
