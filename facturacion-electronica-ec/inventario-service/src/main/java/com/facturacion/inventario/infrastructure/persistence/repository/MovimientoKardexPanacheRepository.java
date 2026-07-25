package com.facturacion.inventario.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.inventario.infrastructure.persistence.entity.MovimientoKardexEntity;

@ApplicationScoped
public class MovimientoKardexPanacheRepository implements PanacheRepositoryBase<MovimientoKardexEntity, UUID> {
}
