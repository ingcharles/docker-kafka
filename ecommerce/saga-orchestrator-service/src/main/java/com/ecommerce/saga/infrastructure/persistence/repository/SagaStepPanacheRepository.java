package com.ecommerce.saga.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.ecommerce.saga.infrastructure.persistence.entity.SagaStepEntity;

@ApplicationScoped
public class SagaStepPanacheRepository implements PanacheRepositoryBase<SagaStepEntity, UUID> {
}
