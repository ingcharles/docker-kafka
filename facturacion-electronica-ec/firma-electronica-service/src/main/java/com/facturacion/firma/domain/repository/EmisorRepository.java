package com.facturacion.firma.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.Emisor;
import com.facturacion.firma.domain.model.vo.PageResult;

public interface EmisorRepository {

    Uni<Emisor> persist(Emisor emisor);

    Uni<Emisor> update(Emisor emisor);

    Uni<Emisor> findById(UUID id);

    Uni<Emisor> findByRuc(String ruc);

    Uni<Boolean> existsByRuc(String ruc);

    Uni<PageResult<Emisor>> findPaged(int page, int limit);
}
