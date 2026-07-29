package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.comprobantes.infrastructure.persistence.entity.EstablecimientoLocalEntity;

@ApplicationScoped
public class EstablecimientoLocalPanacheRepository
        implements PanacheRepositoryBase<EstablecimientoLocalEntity, UUID> {
}
