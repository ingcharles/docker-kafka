package com.ecommerce.order.application.event;

/** Forma de una dirección de envío dentro del mensaje de Kafka, incrustada en {@link OrderEventMessage}. */
public record AddressPayload(String street, String city, String state, String zip, String country) {
}
