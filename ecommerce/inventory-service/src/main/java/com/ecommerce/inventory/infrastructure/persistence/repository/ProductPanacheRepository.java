package com.ecommerce.inventory.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.ecommerce.inventory.infrastructure.persistence.entity.ProductEntity;

@ApplicationScoped
public class ProductPanacheRepository implements PanacheRepositoryBase<ProductEntity, UUID> {
}
