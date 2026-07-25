package com.facturacion.sri.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.sri.infrastructure.persistence.entity.EnvioSriEntity;

@ApplicationScoped
public class EnvioSriPanacheRepository implements PanacheRepositoryBase<EnvioSriEntity, UUID> {
}
