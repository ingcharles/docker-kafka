package com.facturacion.inventario.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.application.dto.ActualizarCategoriaRequest;
import com.facturacion.inventario.application.dto.CategoriaResponse;
import com.facturacion.inventario.application.dto.CrearCategoriaRequest;
import com.facturacion.inventario.domain.model.entity.Categoria;
import com.facturacion.inventario.domain.model.enums.TipoCategoria;
import com.facturacion.commons.paging.PageResult;
import com.facturacion.inventario.domain.repository.CategoriaRepository;
import com.facturacion.inventario.infrastructure.persistence.mapper.CategoriaMapper;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.commons.paging.PagedResponse;

@ApplicationScoped
public class CategoriaService {

    private final CategoriaRepository repository;
    private final CategoriaMapper mapper;

    @Inject
    public CategoriaService(CategoriaRepository repository, CategoriaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @WithTransaction
    public Uni<CategoriaResponse> crear(String ruc, @Valid CrearCategoriaRequest request) {
        Categoria categoria = new Categoria(UUID.randomUUID(), ruc, request.getNombre(),
            TipoCategoria.valueOf(request.getTipo()), request.getDescripcion(), true, Instant.now());
        return repository.persist(categoria).map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<CategoriaResponse> actualizar(UUID id, @Valid ActualizarCategoriaRequest request) {
        Categoria categoria = new Categoria(id, null, request.getNombre(), TipoCategoria.valueOf(request.getTipo()),
            request.getDescripcion(), request.isActiva(), null);
        return repository.update(categoria)
            .onItem().ifNull().failWith(() -> new NotFoundException("Categoría no encontrada: " + id))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<CategoriaResponse> obtenerPorId(UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Categoría no encontrada: " + id))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<PagedResponse<CategoriaResponse>> listarPaginado(int page, int limit, String ruc) {
        return repository.findPaged(page, limit, ruc)
            .map((PageResult<Categoria> pageResult) -> {
                List<CategoriaResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }
}
