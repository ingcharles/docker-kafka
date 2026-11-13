package com.facturacion.comprobantes.domain.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Los catálogos del SRI que administra este servicio (Tablas 6, 16, 17, 19, 20, 24 y el sustento del ATS).
 * {@code nombre()} es el nombre de la tabla y el segmento de la URL ({@code /catalogos/{nombre}}).
 */
public enum Catalogo {

    FORMA_PAGO("forma_pago"),
    IMPUESTO("impuesto"),
    TARIFA_IMPUESTO("tarifa_impuesto"),
    TIPO_IDENTIFICACION("tipo_identificacion"),
    IMPUESTO_RETENCION("impuesto_retencion"),
    RETENCION("retencion"),
    SUSTENTO_TRIBUTARIO("sustento_tributario");

    private final String nombre;

    Catalogo(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }

    public static List<String> nombres() {
        return Arrays.stream(values()).map(Catalogo::nombre).toList();
    }

    public static Optional<Catalogo> deNombre(String nombre) {
        return Arrays.stream(values()).filter(c -> c.nombre.equals(nombre)).findFirst();
    }
}
