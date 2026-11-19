package com.facturacion.notificacion.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.notificacion.infrastructure.persistence.entity.PlantillaCorreoHistorialEntity;

@ApplicationScoped
public class PlantillaCorreoHistorialPanacheRepository
        implements PanacheRepositoryBase<PlantillaCorreoHistorialEntity, UUID> {
}
