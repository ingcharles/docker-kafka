package com.ecommerce.order.application.service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.ecommerce.order.application.dto.CreateOrderRequest;
import com.ecommerce.order.application.dto.OrderItemRequest;
import com.ecommerce.order.application.dto.OrderResponse;
import com.ecommerce.order.application.event.AddressPayload;
import com.ecommerce.order.application.event.OrderEventMessage;
import com.ecommerce.order.application.event.OrderItemPayload;
import com.ecommerce.order.domain.model.entity.Order;
import com.ecommerce.order.domain.model.entity.OrderItem;
import com.ecommerce.order.domain.model.enums.OrderStatus;
import com.ecommerce.order.domain.model.vo.Address;
import com.ecommerce.order.domain.model.vo.PageResult;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.infrastructure.outbox.OutboxEventEntity;
import com.ecommerce.order.infrastructure.outbox.OutboxEventRepository;
import com.ecommerce.order.infrastructure.persistence.mapper.OrderMapper;
import com.ecommerce.order.shared.constant.MessagingConstants;
import com.ecommerce.order.shared.exception.NotFoundException;
import com.ecommerce.order.shared.responses.PagedResponse;

import java.util.UUID;

@ApplicationScoped
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public OrderService(OrderRepository repository, OrderMapper mapper,
        OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea la orden y deja su evento {@code OrderCreated} listo en la tabla
     * outbox, dentro de la misma transacción. Es decir: la escritura en
     * {@code orders} y la escritura en {@code outbox_event} se confirman
     * juntas, o se revierten juntas — nunca puede pasar que la saga arranque
     * a partir de un evento cuya orden en realidad no llegó a guardarse (ni
     * al revés, que la orden se guarde pero el evento nunca se publique).
     */
    @WithTransaction
    public Uni<OrderResponse> createOrder(@Valid CreateOrderRequest request) {
        List<OrderItem> items = request.getItems().stream()
            .map(this::toDomainItem)
            .collect(Collectors.toList());
        Address address = new Address(
            request.getShippingAddress().getStreet(),
            request.getShippingAddress().getCity(),
            request.getShippingAddress().getState(),
            request.getShippingAddress().getZip(),
            request.getShippingAddress().getCountry());
        Order order = Order.createNew(request.getCustomerId(), request.getCurrency(), address, items);

        return repository.persist(order)
            .chain(saved -> outboxRepository.persist(buildOrderCreatedOutboxEvent(saved))
                .replaceWith(saved))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<OrderResponse> getById(UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Order not found: " + id))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<PagedResponse<OrderResponse>> listPaged(int page, int limit, String customerId,
        OrderStatus status) {
        return repository.findPaged(page, limit, customerId, status)
            .map((PageResult<Order> pageResult) -> {
                List<OrderResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }

    private OrderItem toDomainItem(OrderItemRequest request) {
        return new OrderItem(request.getProductId(), request.getProductName(),
            request.getQuantity(), request.getUnitPrice());
    }

    private OutboxEventEntity buildOrderCreatedOutboxEvent(Order order) {
        OrderEventMessage message = new OrderEventMessage(
            UUID.randomUUID(),
            order.getId(),
            MessagingConstants.EVENT_TYPE_ORDER_CREATED,
            order.getCustomerId(),
            order.totalAmount(),
            order.getCurrency(),
            order.getItems().stream()
                .map(item -> new OrderItemPayload(item.getProductId(), item.getProductName(),
                    item.getQuantity(), item.getUnitPrice()))
                .collect(Collectors.toList()),
            new AddressPayload(order.getShippingAddress().street(), order.getShippingAddress().city(),
                order.getShippingAddress().state(), order.getShippingAddress().zip(),
                order.getShippingAddress().country()),
            order.getCreatedAt());
        return OutboxEventEntity.of(order.getId(), MessagingConstants.EVENT_TYPE_ORDER_CREATED,
            MessagingConstants.ORDER_EVENTS_TOPIC, writeValueAsString(message));
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
