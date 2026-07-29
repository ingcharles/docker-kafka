package com.facturacion.identidad.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.identidad.infrastructure.persistence.entity.EmpresaLocalEntity;

@ApplicationScoped
public class EmpresaLocalPanacheRepository implements PanacheRepositoryBase<EmpresaLocalEntity, UUID> {
}
