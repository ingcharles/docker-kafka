package com.ecommerce.payment.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import com.ecommerce.payment.infrastructure.persistence.entity.PaymentEntity;

@ApplicationScoped
public class PaymentPanacheRepository implements PanacheRepositoryBase<PaymentEntity, UUID> {
}
