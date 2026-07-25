package com.ecommerce.saga.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ecommerce.saga.domain.model.enums.SagaState;
import com.ecommerce.saga.domain.model.vo.Address;

/**
 * Agregado de la saga: una fila por cada orden, donde {@code id} es
 * directamente el {@code orderId} (el id de correlación de toda la saga).
 * Guarda justo los datos suficientes de la orden como para poder construir
 * cualquier comando de aquí en adelante sin tener que volver a llamar a
 * order-service — {@code itemsSnapshot} es la lista de ítems del evento
 * OrderCreated, guardada tal cual como JSON, porque ReserveStock la
 * necesita y ningún otro dato de este servicio la requiere.
 */
public class SagaInstance {

    private UUID id;
    private SagaState state;
    private String customerId;
    private BigDecimal totalAmount;
    private String currency;
    private Address shippingAddress;
    private String itemsSnapshot;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public SagaInstance() {
    }

    public static SagaInstance start(UUID orderId, String customerId, BigDecimal totalAmount,
        String currency, Address shippingAddress, String itemsSnapshot) {
        SagaInstance instance = new SagaInstance();
        instance.id = orderId;
        instance.state = SagaState.STARTED;
        instance.customerId = customerId;
        instance.totalAmount = totalAmount;
        instance.currency = currency;
        instance.shippingAddress = shippingAddress;
        instance.itemsSnapshot = itemsSnapshot;
        Instant now = Instant.now();
        instance.createdAt = now;
        instance.updatedAt = now;
        return instance;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SagaState getState() {
        return state;
    }

    public void setState(SagaState state) {
        this.state = state;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getItemsSnapshot() {
        return itemsSnapshot;
    }

    public void setItemsSnapshot(String itemsSnapshot) {
        this.itemsSnapshot = itemsSnapshot;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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
