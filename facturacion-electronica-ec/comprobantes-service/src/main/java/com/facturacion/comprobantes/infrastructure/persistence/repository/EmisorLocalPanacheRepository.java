package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.comprobantes.infrastructure.persistence.entity.EmisorLocalEntity;

@ApplicationScoped
public class EmisorLocalPanacheRepository implements PanacheRepositoryBase<EmisorLocalEntity, UUID> {
}
