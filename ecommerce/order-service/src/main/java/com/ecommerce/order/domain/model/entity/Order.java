package com.ecommerce.order.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ecommerce.order.domain.model.enums.OrderStatus;
import com.ecommerce.order.domain.model.vo.Address;

/**
 * Raíz del agregado Order. Es dueña de sus ítems ({@code items}); el monto
 * total siempre se calcula al vuelo ({@link #totalAmount()}) y nunca se
 * guarda como campo redundante en este objeto de dominio (la entidad de
 * persistencia sí lo cachea en una columna, por conveniencia al hacer
 * consultas — ver {@code infrastructure.persistence.entity.OrderEntity}).
 */
public class Order {

    private UUID id;
    private String customerId;
    private OrderStatus status;
    private String currency;
    private Address shippingAddress;
    private List<OrderItem> items = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public Order() {
    }

    public static Order createNew(String customerId, String currency, Address shippingAddress,
        List<OrderItem> items) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.currency = currency;
        order.shippingAddress = shippingAddress;
        order.items = new ArrayList<>(items);
        order.status = OrderStatus.PENDING;
        Instant now = Instant.now();
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    public BigDecimal totalAmount() {
        return items.stream()
            .map(OrderItem::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
