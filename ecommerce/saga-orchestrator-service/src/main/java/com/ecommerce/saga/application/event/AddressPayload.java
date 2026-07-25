package com.ecommerce.saga.application.event;

public record AddressPayload(String street, String city, String state, String zip, String country) {
}
