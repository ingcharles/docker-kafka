package com.facturacion.identidad.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.Usuario;

public interface UsuarioRepository {

    Uni<Usuario> persist(Usuario usuario);

    Uni<Usuario> findById(UUID id);

    Uni<Usuario> findByEmail(String email);

    Uni<Boolean> existsByEmail(String email);
}
