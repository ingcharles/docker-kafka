package com.facturacion.inventario.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.inventario.domain.model.entity.Item;
import com.facturacion.inventario.domain.model.enums.TipoItem;
import com.facturacion.inventario.domain.repository.ItemRepository;
import com.facturacion.inventario.infrastructure.persistence.entity.ItemEntity;
import com.facturacion.inventario.infrastructure.persistence.mapper.ItemMapper;

@ApplicationScoped
public class ItemRepositoryImpl implements ItemRepository {

    private final ItemPanacheRepository repository;
    private final ItemMapper mapper;

    @Inject
    public ItemRepositoryImpl(ItemPanacheRepository repository, ItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Item> persist(Item domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Item> update(Item domain) {
        return repository.<ItemEntity>findById(domain.getId())
            .onItem().ifNotNull().transformToUni(entity -> {
                entity.categoriaId = domain.getCategoriaId();
                entity.descripcion = domain.getDescripcion();
                entity.precioUnitario = domain.getPrecioUnitario();
                entity.unidadMedida = domain.getUnidadMedida();
                entity.stockMinimo = domain.getStockMinimo();
                entity.activo = domain.isActivo();
                entity.actualizadoEn = Instant.now();
                return Uni.createFrom().item(mapper.toDomain(entity));
            });
    }

    @Override
    public Uni<Item> findById(UUID id) {
        return repository.<ItemEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<Item>> findPaged(int page, int limit, TipoItem tipo, UUID categoriaId, String ruc) {
        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (tipo != null) {
            query.append(" and tipo = :tipo");
            params.put("tipo", tipo.name());
        }
        if (categoriaId != null) {
            query.append(" and categoriaId = :categoriaId");
            params.put("categoriaId", categoriaId);
        }
        if (ruc != null) {
            query.append(" and ruc = :ruc");
            params.put("ruc", ruc);
        }

        var panacheQuery = repository.find(query.toString(), params).page(Page.of(page, limit));
        Uni<List<ItemEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();

        return Uni.combine().all().unis(itemsUni, countUni)
            .asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }

    @Override
    public Uni<Integer> intentarReservar(UUID itemId, BigDecimal cantidad) {
        return repository.update(
            "cantidadDisponible = cantidadDisponible - ?1, cantidadReservada = cantidadReservada + ?1, "
                + "actualizadoEn = ?2 where id = ?3 and tipo = 'PRODUCTO' and cantidadDisponible >= ?1",
            cantidad, Instant.now(), itemId);
    }

    @Override
    public Uni<Integer> confirmarReserva(UUID itemId, BigDecimal cantidad) {
        return repository.update(
            "cantidadReservada = cantidadReservada - ?1, actualizadoEn = ?2 where id = ?3",
            cantidad, Instant.now(), itemId);
    }

    @Override
    public Uni<Integer> liberarReserva(UUID itemId, BigDecimal cantidad) {
        return repository.update(
            "cantidadDisponible = cantidadDisponible + ?1, cantidadReservada = cantidadReservada - ?1, "
                + "actualizadoEn = ?2 where id = ?3",
            cantidad, Instant.now(), itemId);
    }

    @Override
    public Uni<Integer> incrementarDisponible(UUID itemId, BigDecimal cantidad) {
        return repository.update(
            "cantidadDisponible = cantidadDisponible + ?1, actualizadoEn = ?2 where id = ?3",
            cantidad, Instant.now(), itemId);
    }

    @Override
    public Uni<Integer> decrementarDisponible(UUID itemId, BigDecimal cantidad) {
        return repository.update(
            "cantidadDisponible = cantidadDisponible - ?1, actualizadoEn = ?2 "
                + "where id = ?3 and cantidadDisponible >= ?1",
            cantidad, Instant.now(), itemId);
    }
}
