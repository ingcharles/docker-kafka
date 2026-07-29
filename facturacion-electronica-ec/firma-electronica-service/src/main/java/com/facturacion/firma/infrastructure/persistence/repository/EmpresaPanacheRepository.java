package com.facturacion.firma.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.firma.infrastructure.persistence.entity.EmpresaEntity;

@ApplicationScoped
public class EmpresaPanacheRepository implements PanacheRepositoryBase<EmpresaEntity, UUID> {
}
