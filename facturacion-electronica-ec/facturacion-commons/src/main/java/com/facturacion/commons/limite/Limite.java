package com.facturacion.commons.limite;

import java.time.Duration;

/**
 * Cuántas veces se permite una operación dentro de cada {@code ventana}: a cada usuario y a la empresa entera. Un máximo
 * de {@code 0} significa sin límite.
 */
public record Limite(int maximoPorUsuario, int maximoPorEmpresa, Duration ventana) {

    /** Para una operación sin límite configurado. */
    public static final Limite NINGUNO = new Limite(0, 0, Duration.ZERO);
}
