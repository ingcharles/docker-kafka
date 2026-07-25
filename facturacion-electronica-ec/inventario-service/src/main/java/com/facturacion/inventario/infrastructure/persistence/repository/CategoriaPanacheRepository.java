package com.facturacion.inventario.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.inventario.infrastructure.persistence.entity.CategoriaEntity;

@ApplicationScoped
public class CategoriaPanacheRepository implements PanacheRepositoryBase<CategoriaEntity, UUID> {
}
