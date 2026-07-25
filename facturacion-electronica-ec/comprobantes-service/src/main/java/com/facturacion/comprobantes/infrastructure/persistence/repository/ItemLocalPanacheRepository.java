package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.comprobantes.infrastructure.persistence.entity.ItemLocalEntity;

@ApplicationScoped
public class ItemLocalPanacheRepository implements PanacheRepositoryBase<ItemLocalEntity, UUID> {
}
