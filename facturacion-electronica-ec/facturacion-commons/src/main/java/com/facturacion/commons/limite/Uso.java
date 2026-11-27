package com.facturacion.commons.limite;

/** Lo consumido en la ventana en curso y los segundos que faltan para que empiece una nueva. */
public record Uso(int usos, long segundosRestantes) {
}
