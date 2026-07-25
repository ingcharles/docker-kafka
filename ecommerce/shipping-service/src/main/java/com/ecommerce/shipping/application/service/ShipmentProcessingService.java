package com.ecommerce.shipping.application.service;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.ecommerce.shipping.application.event.AddressPayload;
import com.ecommerce.shipping.application.event.ShippingEventMessage;
import com.ecommerce.shipping.domain.model.entity.Shipment;
import com.ecommerce.shipping.domain.model.enums.ShipmentStatus;
import com.ecommerce.shipping.domain.model.vo.Address;
import com.ecommerce.shipping.domain.repository.ShipmentRepository;
import com.ecommerce.shipping.infrastructure.inbox.ProcessedMessageEntity;
import com.ecommerce.shipping.infrastructure.inbox.ProcessedMessageRepository;
import com.ecommerce.shipping.infrastructure.outbox.OutboxEventEntity;
import com.ecommerce.shipping.infrastructure.outbox.OutboxEventRepository;
import com.ecommerce.shipping.shared.constant.MessagingConstants;

/**
 * Integración simulada con un transportista: falla de forma determinista
 * cuando la ciudad de la dirección de envío es exactamente el valor
 * centinela {@code "FAIL"} (sin distinguir mayúsculas/minúsculas), para que
 * el camino de compensación de la saga se pueda reproducir con una simple
 * llamada a la API pública. Reemplázala por una llamada real a un
 * transportista el día que esto deje de ser una implementación de
 * referencia/demo.
 */
@ApplicationScoped
public class ShipmentProcessingService {

    private final ShipmentRepository shipmentRepository;
    private final OutboxEventRepository outboxRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public ShipmentProcessingService(ShipmentRepository shipmentRepository,
        OutboxEventRepository outboxRepository, ProcessedMessageRepository processedMessageRepository,
        ObjectMapper objectMapper) {
        this.shipmentRepository = shipmentRepository;
        this.outboxRepository = outboxRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<Void> createShipment(UUID eventId, UUID orderId, Address address) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    return Uni.createFrom().voidItem();
                }
                boolean fails = MessagingConstants.FAILURE_SENTINEL_CITY.equalsIgnoreCase(address.city());
                Shipment shipment = Shipment.createNew(orderId, address);
                String eventType;
                String reason = null;
                String trackingNumber = null;
                if (fails) {
                    shipment.setStatus(ShipmentStatus.FAILED);
                    reason = "Carrier rejected destination city '" + address.city() + "'";
                    shipment.setFailureReason(reason);
                    eventType = MessagingConstants.EVENT_TYPE_SHIPMENT_FAILED;
                } else {
                    shipment.setStatus(ShipmentStatus.CREATED);
                    trackingNumber = "TRK-" + UUID.randomUUID();
                    shipment.setTrackingNumber(trackingNumber);
                    eventType = MessagingConstants.EVENT_TYPE_SHIPMENT_CREATED;
                }
                String finalReason = reason;
                String finalTrackingNumber = trackingNumber;
                return shipmentRepository.persist(shipment)
                    .chain(() -> recordOutcome(eventId, orderId, eventType, finalReason, finalTrackingNumber));
            });
    }

    @WithTransaction
    public Uni<Void> cancelShipment(UUID eventId, UUID orderId) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    return Uni.createFrom().voidItem();
                }
                return shipmentRepository.updateStatus(orderId, ShipmentStatus.CANCELLED,
                        ShipmentStatus.CREATED)
                    .chain(() -> recordOutcome(eventId, orderId,
                        MessagingConstants.EVENT_TYPE_SHIPMENT_CANCELLED, null, null));
            });
    }

    private Uni<Void> recordOutcome(UUID eventId, UUID orderId, String eventType, String reason,
        String trackingNumber) {
        ShippingEventMessage message = new ShippingEventMessage(
            UUID.randomUUID(), orderId, eventType, reason, trackingNumber, Instant.now());
        OutboxEventEntity outboxEvent = OutboxEventEntity.of(orderId, eventType,
            MessagingConstants.SHIPPING_EVENTS_TOPIC, writeValueAsString(message));
        return processedMessageRepository.persist(ProcessedMessageEntity.of(eventId))
            .chain(() -> outboxRepository.persist(outboxEvent))
            .replaceWithVoid();
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
