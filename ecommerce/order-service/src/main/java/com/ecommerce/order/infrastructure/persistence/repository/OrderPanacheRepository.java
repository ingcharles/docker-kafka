package com.ecommerce.order.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.ecommerce.order.infrastructure.persistence.entity.OrderEntity;

@ApplicationScoped
public class OrderPanacheRepository implements PanacheRepositoryBase<OrderEntity, UUID> {
}
