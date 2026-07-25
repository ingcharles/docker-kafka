package com.facturacion.inventario.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.domain.model.entity.ImagenItem;

public interface ImagenItemRepository {

    Uni<ImagenItem> persist(ImagenItem imagen);

    Uni<ImagenItem> findById(UUID id);

    Uni<List<ImagenItem>> findByItemId(UUID itemId);

    /** Antes de marcar una imagen como principal, desmarca cualquier otra del mismo ítem. */
    Uni<Integer> desmarcarPrincipal(UUID itemId);

    Uni<Boolean> deleteById(UUID id);
}
