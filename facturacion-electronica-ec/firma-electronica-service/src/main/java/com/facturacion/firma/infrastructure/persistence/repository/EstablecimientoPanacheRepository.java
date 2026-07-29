package com.facturacion.firma.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.firma.infrastructure.persistence.entity.EstablecimientoEntity;

@ApplicationScoped
public class EstablecimientoPanacheRepository implements PanacheRepositoryBase<EstablecimientoEntity, UUID> {
}
