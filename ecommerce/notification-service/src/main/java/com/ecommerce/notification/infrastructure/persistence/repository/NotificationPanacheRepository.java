package com.ecommerce.notification.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.ecommerce.notification.infrastructure.persistence.entity.NotificationEntity;

@ApplicationScoped
public class NotificationPanacheRepository implements PanacheRepositoryBase<NotificationEntity, UUID> {
}
