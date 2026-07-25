package com.facturacion.orquestacion.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.orquestacion.infrastructure.persistence.entity.SagaStepEntity;

@ApplicationScoped
public class SagaStepPanacheRepository implements PanacheRepositoryBase<SagaStepEntity, UUID> {
}
