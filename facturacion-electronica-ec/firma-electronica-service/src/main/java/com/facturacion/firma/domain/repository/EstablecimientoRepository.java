package com.facturacion.firma.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.Establecimiento;

public interface EstablecimientoRepository {

    Uni<Establecimiento> persist(Establecimiento establecimiento);

    Uni<Establecimiento> update(Establecimiento establecimiento);

    Uni<Establecimiento> findById(UUID id);

    Uni<List<Establecimiento>> findByEmpresaId(UUID empresaId);

    Uni<Boolean> existsByEmpresaIdAndCodigo(UUID empresaId, String codigo);
}
