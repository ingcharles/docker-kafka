package com.ecommerce.saga.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.ecommerce.saga.infrastructure.persistence.entity.SagaInstanceEntity;

@ApplicationScoped
public class SagaInstancePanacheRepository implements PanacheRepositoryBase<SagaInstanceEntity, UUID> {
}
