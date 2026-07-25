package com.ecommerce.payment.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import com.ecommerce.payment.application.dto.PaymentResponse;
import com.ecommerce.payment.domain.model.entity.Payment;
import com.ecommerce.payment.domain.model.enums.PaymentStatus;
import com.ecommerce.payment.domain.model.vo.PageResult;
import com.ecommerce.payment.domain.repository.PaymentRepository;
import com.ecommerce.payment.infrastructure.persistence.mapper.PaymentMapper;
import com.ecommerce.payment.shared.exception.NotFoundException;
import com.ecommerce.payment.shared.responses.PagedResponse;

@ApplicationScoped
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;

    @Inject
    public PaymentService(PaymentRepository repository, PaymentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @WithSession
    public Uni<PaymentResponse> getByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Payment not found for order: " + orderId))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<PagedResponse<PaymentResponse>> listPaged(int page, int limit, PaymentStatus status) {
        return repository.findPaged(page, limit, status)
            .map((PageResult<Payment> pageResult) -> {
                List<PaymentResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }
}
