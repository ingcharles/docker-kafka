package com.ecommerce.shipping.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.ecommerce.shipping.infrastructure.persistence.entity.ShipmentEntity;

@ApplicationScoped
public class ShipmentPanacheRepository implements PanacheRepositoryBase<ShipmentEntity, UUID> {
}
