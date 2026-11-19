package com.facturacion.comprobantes.domain.model.enums;

/**
 * BORRADOR: creado, {@code xml_generado} guardado, sin firmar todavía.
 * FIRMADO: {@code xml_firmado} guardado (respuesta de firma-electronica-service).
 * FIRMA_FALLIDA: la empresa no tiene certificado activo, o algo falló al firmar — estado terminal de error: para volver a emitir se crea un comprobante nuevo.
 * AUTORIZADO: {@code xml_autorizado} + número/fecha de autorización + el RIDE en PDF guardados — el SRI aceptó el comprobante.
 * <p>
 * IMPORTANTE para sri-conector-service (quien manda el comando {@code MARCAR_AUTORIZADO}):
 * {@code xml_autorizado} <b>no</b> debe ser una copia de {@code xml_firmado}. Debe ser el
 * bloque {@code <autorizacion>} completo tal como lo devuelve el WSDL real de Autorización
 * del SRI (verificado contra {@code AutorizacionComprobantesOffline?wsdl} en celcer.sri.gob.ec):
 * {@code <estado>}, {@code <numeroAutorizacion>}, {@code <fechaAutorizacion>}, {@code <ambiente>}
 * y {@code <comprobante>} (CDATA con el mismo XML firmado — el SRI no lo modifica, solo lo
 * envuelve). Que el contenido del {@code <comprobante>} interno coincida byte a byte con
 * {@code xml_firmado} es correcto y esperado; lo que sería un bug es guardar solo eso sin el
 * envoltorio {@code <autorizacion>} con sus metadatos.
 * NO_AUTORIZADO: {@code motivo_rechazo} guardado — terminal; cubre tanto la no autorización como el rechazo en recepción del SRI (la saga sí los distingue). Se corrige emitiendo un comprobante nuevo.
 * STOCK_INSUFICIENTE: con el descuento de inventario habilitado, el orquestador no pudo reservar el stock de alguna línea; {@code motivo_rechazo} dice cuáles. Terminal: se emite un comprobante nuevo cuando haya stock.
 * NOTIFICADO: ya se envió por correo al receptor — estado terminal feliz.
 * ANULADO: fuera de alcance de esta versión (requiere el flujo de anulación formal del SRI, ver ARQUITECTURA.md) — el valor existe para no tener que migrar el enum más adelante.
 */
public enum EstadoComprobante {
    BORRADOR,
    FIRMADO,
    FIRMA_FALLIDA,
    AUTORIZADO,
    NO_AUTORIZADO,
    NOTIFICADO,
    ANULADO,
    /** El inventario no tenía stock para alguna línea (solo si el descuento de inventario está habilitado): terminal. */
    STOCK_INSUFICIENTE;

    /**
     * Si el comprobante puede pasar de este estado a {@code destino}. El ciclo de vida solo avanza: BORRADOR →
     * FIRMADO o FIRMA_FALLIDA; FIRMADO → AUTORIZADO o NO_AUTORIZADO; AUTORIZADO → NOTIFICADO. Los demás son finales.
     * Un comando repetido o fuera de orden del orquestador no debe retroceder un comprobante.
     */
    public boolean puedePasarA(EstadoComprobante destino) {
        return switch (this) {
            case BORRADOR -> destino == FIRMADO || destino == FIRMA_FALLIDA || destino == STOCK_INSUFICIENTE;
            case FIRMADO -> destino == AUTORIZADO || destino == NO_AUTORIZADO;
            case AUTORIZADO -> destino == NOTIFICADO;
            case FIRMA_FALLIDA, NO_AUTORIZADO, NOTIFICADO, ANULADO, STOCK_INSUFICIENTE -> false;
        };
    }

    /**
     * Si un comprobante en este estado cuenta como emitido a efectos de lo que ya se aplicó sobre un documento (las notas de
     * crédito suman contra el valor del documento): los que van en camino o se autorizaron cuentan; los que fallaron o se
     * rechazaron no.
     */
    public boolean cuentaComoEmitido() {
        return this == BORRADOR || this == FIRMADO || this == AUTORIZADO || this == NOTIFICADO;
    }

    /**
     * Si {@code destino} está ADELANTE en el ciclo de vida pero no es el siguiente paso válido (p. ej. llega
     * {@code AUTORIZADO} con el comprobante todavía en {@code BORRADOR}): un comando anterior (el de firmado) no se
     * aplicó. A diferencia de un comando repetido o atrasado —que se ignora sin más— este no puede descartarse en
     * silencio: el SRI ya autorizó el comprobante y el servicio debe reflejarlo, así que quien aplica el comando
     * falla de forma visible (DLQ) para poder reprocesarlo en orden.
     */
    public boolean esSaltoDePasos(EstadoComprobante destino) {
        return switch (this) {
            case BORRADOR -> destino == AUTORIZADO || destino == NO_AUTORIZADO || destino == NOTIFICADO;
            case FIRMADO -> destino == NOTIFICADO;
            default -> false;
        };
    }
}
