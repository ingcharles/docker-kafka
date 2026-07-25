package com.ecommerce.payment.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.ecommerce.payment.domain.model.entity.Payment;
import com.ecommerce.payment.domain.model.enums.PaymentStatus;
import com.ecommerce.payment.domain.model.vo.PageResult;

public interface PaymentRepository {

    Uni<Payment> persist(Payment payment);

    Uni<Payment> findByOrderId(UUID orderId);

    Uni<PageResult<Payment>> findPaged(int page, int limit, PaymentStatus status);

    Uni<Payment> updateStatus(UUID orderId, PaymentStatus to, String gatewayReference,
        String declineReason, PaymentStatus... allowedFrom);
}
