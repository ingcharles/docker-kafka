package com.ecommerce.order.domain.model.vo;

import java.util.Objects;

/**
 * Value object de la dirección de envío. Si {@code city} es exactamente el
 * valor centinela {@code "FAIL"} (sin distinguir mayúsculas/minúsculas), es
 * el disparador determinista que usa shipping-service para simular un envío
 * fallido y así poder probar el camino de compensación de la saga — se deja
 * documentado aquí, en Address, porque este es el lugar donde ese valor se
 * origina (viaja desde aquí hasta shipping-service dentro del comando
 * CreateShipment).
 */
public record Address(String street, String city, String state, String zip, String country) {

    public Address {
        Objects.requireNonNull(street, "street is required");
        Objects.requireNonNull(city, "city is required");
        Objects.requireNonNull(country, "country is required");
    }
}
