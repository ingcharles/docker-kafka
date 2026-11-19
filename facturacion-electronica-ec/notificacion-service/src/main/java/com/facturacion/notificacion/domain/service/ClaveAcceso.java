package com.facturacion.notificacion.domain.service;

import java.time.DateTimeException;
import java.time.LocalDate;

import com.facturacion.commons.time.FechasSri;

/**
 * Lo que este servicio necesita saber de la clave de acceso de 49 dígitos del SRI: la notificación no guarda el
 * RUC del emisor, pero la clave lo lleva incrustado (fecha ddMMyyyy = 8 dígitos, tipo de comprobante = 2, RUC de 13,
 * ambiente = 1, establecimiento = 3, punto de emisión = 3 y secuencial = 9), así que de ahí se resuelve a qué empresa
 * pertenece, qué número tiene el comprobante y cuándo se emitió.
 */
public final class ClaveAcceso {

    private static final int LONGITUD = 49;
    private static final int INICIO_DIA = 0;
    private static final int INICIO_MES = 2;
    private static final int INICIO_ANIO = 4;
    private static final int FIN_FECHA = 8;
    private static final int INICIO_RUC = 10;
    private static final int FIN_RUC = 23;
    private static final int INICIO_ESTABLECIMIENTO = 24;
    private static final int INICIO_PUNTO_EMISION = 27;
    private static final int INICIO_SECUENCIAL = 30;
    private static final int FIN_SECUENCIAL = 39;
    private static final String SEPARADOR_NUMERO = "-";

    private ClaveAcceso() {
    }

    /** El RUC del emisor, o {@code null} si la clave no tiene el formato de 49 dígitos. */
    public static String obtenerRucDelEmisor(String claveAcceso) {
        if (!tieneFormatoValido(claveAcceso)) {
            return null;
        }
        return claveAcceso.substring(INICIO_RUC, FIN_RUC);
    }

    /** El número del comprobante ({@code 001-001-000000123}), o {@code null} si la clave no tiene el formato de 49 dígitos. */
    public static String obtenerNumeroDelComprobante(String claveAcceso) {
        if (!tieneFormatoValido(claveAcceso)) {
            return null;
        }
        return claveAcceso.substring(INICIO_ESTABLECIMIENTO, INICIO_PUNTO_EMISION) + SEPARADOR_NUMERO
            + claveAcceso.substring(INICIO_PUNTO_EMISION, INICIO_SECUENCIAL) + SEPARADOR_NUMERO
            + claveAcceso.substring(INICIO_SECUENCIAL, FIN_SECUENCIAL);
    }

    /** La fecha de emisión como dd/MM/aaaa, o {@code null} si la clave no tiene el formato o la fecha no existe. */
    public static String obtenerFechaDeEmision(String claveAcceso) {
        if (!tieneFormatoValido(claveAcceso)) {
            return null;
        }
        try {
            return FechasSri.formatearFecha(LocalDate.of(Integer.parseInt(claveAcceso.substring(INICIO_ANIO, FIN_FECHA)),
                Integer.parseInt(claveAcceso.substring(INICIO_MES, INICIO_ANIO)),
                Integer.parseInt(claveAcceso.substring(INICIO_DIA, INICIO_MES))));
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static boolean tieneFormatoValido(String claveAcceso) {
        return claveAcceso != null && claveAcceso.length() == LONGITUD && claveAcceso.chars().allMatch(Character::isDigit);
    }
}
