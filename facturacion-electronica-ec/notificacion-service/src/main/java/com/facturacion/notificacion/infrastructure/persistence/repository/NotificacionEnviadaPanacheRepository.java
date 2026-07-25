package com.facturacion.notificacion.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.facturacion.notificacion.infrastructure.persistence.entity.NotificacionEnviadaEntity;

@ApplicationScoped
public class NotificacionEnviadaPanacheRepository implements PanacheRepositoryBase<NotificacionEnviadaEntity, UUID> {
}
