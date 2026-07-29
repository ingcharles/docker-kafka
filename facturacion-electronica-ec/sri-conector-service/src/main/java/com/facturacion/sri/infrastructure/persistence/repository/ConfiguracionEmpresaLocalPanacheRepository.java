package com.facturacion.sri.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.sri.infrastructure.persistence.entity.ConfiguracionEmpresaLocalEntity;

@ApplicationScoped
public class ConfiguracionEmpresaLocalPanacheRepository
        implements PanacheRepositoryBase<ConfiguracionEmpresaLocalEntity, UUID> {
}
