package com.ecommerce.shipping.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.ecommerce.shipping.domain.model.entity.Shipment;
import com.ecommerce.shipping.domain.model.enums.ShipmentStatus;
import com.ecommerce.shipping.domain.model.vo.PageResult;

public interface ShipmentRepository {

    Uni<Shipment> persist(Shipment shipment);

    Uni<Shipment> findByOrderId(UUID orderId);

    Uni<PageResult<Shipment>> findPaged(int page, int limit, ShipmentStatus status);

    Uni<Shipment> updateStatus(UUID orderId, ShipmentStatus to, ShipmentStatus... allowedFrom);
}
