package com.ecommerce.shipping.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import com.ecommerce.shipping.application.dto.ShipmentResponse;
import com.ecommerce.shipping.domain.model.entity.Shipment;
import com.ecommerce.shipping.domain.model.enums.ShipmentStatus;
import com.ecommerce.shipping.domain.model.vo.PageResult;
import com.ecommerce.shipping.domain.repository.ShipmentRepository;
import com.ecommerce.shipping.infrastructure.persistence.mapper.ShipmentMapper;
import com.ecommerce.shipping.shared.exception.NotFoundException;
import com.ecommerce.shipping.shared.responses.PagedResponse;

@ApplicationScoped
public class ShipmentService {

    private final ShipmentRepository repository;
    private final ShipmentMapper mapper;

    @Inject
    public ShipmentService(ShipmentRepository repository, ShipmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @WithSession
    public Uni<ShipmentResponse> getByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Shipment not found for order: " + orderId))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<PagedResponse<ShipmentResponse>> listPaged(int page, int limit, ShipmentStatus status) {
        return repository.findPaged(page, limit, status)
            .map((PageResult<Shipment> pageResult) -> {
                List<ShipmentResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }
}
