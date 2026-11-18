package com.facturacion.notificacion.domain.service;

/**
 * Lo que este servicio necesita saber de la clave de acceso de 49 dígitos del SRI: la notificación no guarda el
 * RUC del emisor, pero la clave lo lleva incrustado (fecha ddMMyyyy = 8 dígitos, tipo de comprobante = 2, y luego el
 * RUC de 13), así que de ahí se resuelve a qué empresa pertenece.
 */
public final class ClaveAcceso {

    private static final int LONGITUD = 49;
    private static final int INICIO_RUC = 10;
    private static final int FIN_RUC = 23;

    private ClaveAcceso() {
    }

    /** El RUC del emisor, o {@code null} si la clave no tiene el formato de 49 dígitos. */
    public static String obtenerRucDelEmisor(String claveAcceso) {
        if (claveAcceso == null || claveAcceso.length() != LONGITUD || !claveAcceso.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return claveAcceso.substring(INICIO_RUC, FIN_RUC);
    }
}
