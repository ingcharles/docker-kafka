package com.facturacion.firma.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.firma.infrastructure.persistence.entity.PuntoEmisionEntity;

@ApplicationScoped
public class PuntoEmisionPanacheRepository implements PanacheRepositoryBase<PuntoEmisionEntity, UUID> {
}
