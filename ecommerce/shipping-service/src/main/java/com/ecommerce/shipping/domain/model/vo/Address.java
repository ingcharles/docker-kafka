package com.ecommerce.shipping.domain.model.vo;

/**
 * Value object de la dirección de envío. Si {@code city} es exactamente el
 * valor centinela {@code "FAIL"} (sin distinguir mayúsculas/minúsculas), es
 * el disparador determinista usado para simular un envío fallido y así
 * probar el camino de compensación de la saga.
 */
public record Address(String street, String city, String state, String zip, String country) {
}
