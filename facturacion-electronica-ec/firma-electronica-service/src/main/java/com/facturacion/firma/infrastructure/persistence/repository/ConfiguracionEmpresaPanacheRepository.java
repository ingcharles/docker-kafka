package com.facturacion.firma.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.firma.infrastructure.persistence.entity.ConfiguracionEmpresaEntity;

@ApplicationScoped
public class ConfiguracionEmpresaPanacheRepository
        implements PanacheRepositoryBase<ConfiguracionEmpresaEntity, UUID> {
}
