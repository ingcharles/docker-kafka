package com.facturacion.inventario.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.domain.model.entity.ImagenItem;
import com.facturacion.inventario.domain.repository.ImagenItemRepository;
import com.facturacion.inventario.infrastructure.persistence.entity.ImagenItemEntity;
import com.facturacion.inventario.infrastructure.persistence.mapper.ImagenItemMapper;

@ApplicationScoped
public class ImagenItemRepositoryImpl implements ImagenItemRepository {

    private final ImagenItemPanacheRepository repository;
    private final ImagenItemMapper mapper;

    @Inject
    public ImagenItemRepositoryImpl(ImagenItemPanacheRepository repository, ImagenItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<ImagenItem> persist(ImagenItem domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<ImagenItem> findById(UUID id) {
        return repository.<ImagenItemEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<List<ImagenItem>> findByItemId(UUID itemId) {
        return repository.find("itemId = ?1", Sort.ascending("orden"), itemId)
            .<ImagenItemEntity>list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<Integer> desmarcarPrincipal(UUID itemId) {
        return repository.update("esPrincipal = false where itemId = ?1 and esPrincipal = true", itemId);
    }

    @Override
    public Uni<Boolean> deleteById(UUID id) {
        return repository.deleteById(id);
    }
}
