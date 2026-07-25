package com.facturacion.inventario.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Costeo por <b>promedio ponderado</b> (el método de kardex más usado en la
 * práctica contable en Ecuador para PYMES, y el que exige el propio SRI
 * como referencia por defecto salvo que el contribuyente declare otro
 * método). Es lógica de dominio pura — sin persistencia, sin Panache — para
 * poder razonar sobre ella y probarla de forma aislada.
 * <p>
 * Regla del promedio ponderado:
 * <ul>
 *   <li><b>Entrada</b>: el costo promedio se recalcula — {@code nuevoCosto =
 *       (valorSaldoAnterior + valorDeLaEntrada) / nuevaCantidadSaldo}.</li>
 *   <li><b>Salida</b>: el costo unitario usado es siempre el promedio
 *       ponderado <i>vigente</i> — una salida nunca cambia el promedio, solo
 *       reduce la cantidad y el valor del saldo proporcionalmente.</li>
 * </ul>
 */
public final class KardexCalculador {

    private static final int ESCALA = 4;

    private KardexCalculador() {
    }

    public static ResultadoMovimiento calcularEntrada(BigDecimal cantidadSaldoAnterior,
            BigDecimal costoPromedioAnterior, BigDecimal cantidadEntrada, BigDecimal costoUnitarioEntrada) {
        BigDecimal valorAnterior = cantidadSaldoAnterior.multiply(costoPromedioAnterior);
        BigDecimal valorEntrada = cantidadEntrada.multiply(costoUnitarioEntrada);
        BigDecimal nuevaCantidadSaldo = cantidadSaldoAnterior.add(cantidadEntrada);
        BigDecimal nuevoValorSaldo = valorAnterior.add(valorEntrada);
        BigDecimal nuevoCostoPromedio = nuevaCantidadSaldo.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : nuevoValorSaldo.divide(nuevaCantidadSaldo, ESCALA, RoundingMode.HALF_UP);
        return new ResultadoMovimiento(nuevaCantidadSaldo, nuevoCostoPromedio, nuevoValorSaldo, costoUnitarioEntrada);
    }

    /**
     * El llamador debe garantizar {@code cantidadSalida <= cantidadSaldoAnterior}
     * (en la práctica, esa validación ya ocurrió como parte del UPDATE
     * atómico que descuenta {@code cantidadDisponible} en la base de datos
     * — ver {@code ItemRepository.tryReservar} — así que aquí no hace falta
     * repetirla).
     */
    public static ResultadoMovimiento calcularSalida(BigDecimal cantidadSaldoAnterior,
            BigDecimal costoPromedioAnterior, BigDecimal cantidadSalida) {
        BigDecimal nuevaCantidadSaldo = cantidadSaldoAnterior.subtract(cantidadSalida);
        BigDecimal nuevoValorSaldo = nuevaCantidadSaldo.multiply(costoPromedioAnterior);
        return new ResultadoMovimiento(nuevaCantidadSaldo, costoPromedioAnterior, nuevoValorSaldo,
            costoPromedioAnterior);
    }

    public record ResultadoMovimiento(
        BigDecimal cantidadSaldo,
        BigDecimal costoPromedioSaldo,
        BigDecimal valorSaldo,
        BigDecimal costoUnitarioAplicado) {
    }
}
