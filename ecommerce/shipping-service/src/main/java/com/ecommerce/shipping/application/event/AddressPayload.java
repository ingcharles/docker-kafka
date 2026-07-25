package com.ecommerce.shipping.application.event;

public record AddressPayload(String street, String city, String state, String zip, String country) {
}
