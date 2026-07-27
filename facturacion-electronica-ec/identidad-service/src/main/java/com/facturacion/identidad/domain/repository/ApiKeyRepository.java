package com.facturacion.identidad.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.ApiKey;

public interface ApiKeyRepository {

    Uni<ApiKey> persist(ApiKey apiKey);

    Uni<ApiKey> findById(UUID id);

    Uni<List<ApiKey>> findByUsuarioId(UUID usuarioId);

    Uni<Boolean> existsByUsuarioIdAndNombre(UUID usuarioId, String nombre);

    /** UPDATE atómico: marca revocada=true solo si pertenece al usuario dueño. Retorna filas afectadas (0 o 1). */
    Uni<Integer> revocar(UUID id, UUID usuarioId);
}
