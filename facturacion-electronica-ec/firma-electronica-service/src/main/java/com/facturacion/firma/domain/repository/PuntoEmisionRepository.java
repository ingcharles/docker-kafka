package com.facturacion.firma.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.PuntoEmision;

public interface PuntoEmisionRepository {

    Uni<PuntoEmision> persist(PuntoEmision puntoEmision);

    Uni<PuntoEmision> update(PuntoEmision puntoEmision);

    Uni<PuntoEmision> findById(UUID id);

    Uni<List<PuntoEmision>> findByEstablecimientoId(UUID establecimientoId);

    Uni<Boolean> existsByEstablecimientoIdAndCodigo(UUID establecimientoId, String codigo);
}
