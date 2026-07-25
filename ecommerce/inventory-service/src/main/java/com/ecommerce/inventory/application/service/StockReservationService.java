package com.ecommerce.inventory.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import com.ecommerce.inventory.application.dto.StockReservationResponse;
import com.ecommerce.inventory.application.event.InventoryEventMessage;
import com.ecommerce.inventory.application.event.InventoryItemPayload;
import com.ecommerce.inventory.domain.model.entity.StockReservation;
import com.ecommerce.inventory.domain.model.enums.ReservationStatus;
import com.ecommerce.inventory.domain.repository.ProductRepository;
import com.ecommerce.inventory.domain.repository.StockReservationRepository;
import com.ecommerce.inventory.infrastructure.inbox.ProcessedMessageEntity;
import com.ecommerce.inventory.infrastructure.inbox.ProcessedMessageRepository;
import com.ecommerce.inventory.infrastructure.outbox.OutboxEventEntity;
import com.ecommerce.inventory.infrastructure.outbox.OutboxEventRepository;
import com.ecommerce.inventory.infrastructure.persistence.mapper.StockReservationMapper;
import com.ecommerce.inventory.shared.constant.MessagingConstants;
import com.ecommerce.inventory.shared.exception.InsufficientStockException;

/**
 * Maneja los dos comandos de la saga en los que participa este servicio.
 * Ambos son idempotentes (protegidos por el inbox) y ambos dejan la base de
 * datos en un estado completamente consistente aunque el proceso falle a
 * mitad de camino — ver {@link #tryReserveAll} para el detalle de cómo se
 * logra esto en el caso de la reserva.
 */
@ApplicationScoped
public class StockReservationService {

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final StockReservationMapper reservationMapper;
    private final OutboxEventRepository outboxRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public StockReservationService(ProductRepository productRepository,
        StockReservationRepository reservationRepository, StockReservationMapper reservationMapper,
        OutboxEventRepository outboxRepository, ProcessedMessageRepository processedMessageRepository,
        ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.reservationMapper = reservationMapper;
        this.outboxRepository = outboxRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<Void> reserveStock(UUID eventId, UUID orderId, List<InventoryItemPayload> items) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    return Uni.createFrom().voidItem();
                }
                return tryReserveAll(items)
                    .onItem().transformToUni(succeeded -> persistReservationRows(orderId, succeeded)
                        .chain(() -> recordOutcome(eventId, orderId,
                            MessagingConstants.EVENT_TYPE_STOCK_RESERVED, null)))
                    .onFailure(InsufficientStockException.class).recoverWithUni(failure ->
                        recordOutcome(eventId, orderId,
                            MessagingConstants.EVENT_TYPE_STOCK_RESERVATION_FAILED, failure.getMessage()));
            });
    }

    @WithTransaction
    public Uni<Void> releaseStock(UUID eventId, UUID orderId) {
        return processedMessageRepository.alreadyProcessed(eventId)
            .onItem().transformToUni(processed -> {
                if (Boolean.TRUE.equals(processed)) {
                    return Uni.createFrom().voidItem();
                }
                return reservationRepository.findByOrderId(orderId)
                    .onItem().transformToUni(reservations -> {
                        List<StockReservation> active = reservations.stream()
                            .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                            .collect(Collectors.toList());
                        return releaseProductQuantities(active)
                            .chain(() -> reservationRepository.releaseAllForOrder(orderId))
                            .chain(() -> recordOutcome(eventId, orderId,
                                MessagingConstants.EVENT_TYPE_STOCK_RELEASED, null));
                    });
            });
    }

    @WithSession
    public Uni<List<StockReservationResponse>> listReservationsForProduct(UUID productId) {
        return reservationRepository.findByProductId(productId).map(reservationMapper::toResponses);
    }

    /**
     * Reserva, uno por uno y en orden, la cantidad de cada ítem, usando un
     * {@code UPDATE} condicional atómico (ver {@link ProductRepository#tryReserve}).
     * En el momento en que un ítem no se puede satisfacer, se libera todo lo
     * que ya se había reservado *durante este mismo intento* antes de
     * reportar el fallo — así nunca queda una reserva a medias, sin importar
     * si la transacción que envuelve todo esto termina confirmándose o no.
     */
    private Uni<List<InventoryItemPayload>> tryReserveAll(List<InventoryItemPayload> items) {
        List<InventoryItemPayload> succeeded = new ArrayList<>();
        return Multi.createFrom().iterable(items)
            .onItem().transformToUniAndConcatenate(item ->
                productRepository.tryReserve(item.productId(), item.quantity())
                    .onItem().transformToUni(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            succeeded.add(item);
                            return Uni.createFrom().voidItem();
                        }
                        return releaseAll(succeeded)
                            .chain(() -> Uni.createFrom()
                                .<Void>failure(new InsufficientStockException(item.productId())));
                    }))
            .collect().last()
            .replaceWith(succeeded);
    }

    private Uni<Void> releaseAll(List<InventoryItemPayload> items) {
        return Multi.createFrom().iterable(items)
            .onItem().transformToUniAndConcatenate(item ->
                productRepository.release(item.productId(), item.quantity()))
            .collect().last()
            .replaceWithVoid();
    }

    private Uni<Void> releaseProductQuantities(List<StockReservation> reservations) {
        return Multi.createFrom().iterable(reservations)
            .onItem().transformToUniAndConcatenate(r ->
                productRepository.release(r.getProductId(), r.getQuantity()))
            .collect().last()
            .replaceWithVoid();
    }

    private Uni<Void> persistReservationRows(UUID orderId, List<InventoryItemPayload> items) {
        return Multi.createFrom().iterable(items)
            .onItem().transformToUniAndConcatenate(item -> reservationRepository.persist(
                StockReservation.reservedFor(orderId, item.productId(), item.quantity())))
            .collect().last()
            .replaceWithVoid();
    }

    private Uni<Void> recordOutcome(UUID eventId, UUID orderId, String eventType, String reason) {
        return processedMessageRepository.persist(ProcessedMessageEntity.of(eventId))
            .chain(() -> outboxRepository.persist(buildEvent(orderId, eventType, reason)))
            .replaceWithVoid();
    }

    private OutboxEventEntity buildEvent(UUID orderId, String eventType, String reason) {
        InventoryEventMessage message = new InventoryEventMessage(
            UUID.randomUUID(), orderId, eventType, reason, Instant.now());
        return OutboxEventEntity.of(orderId, eventType, MessagingConstants.INVENTORY_EVENTS_TOPIC,
            writeValueAsString(message));
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
