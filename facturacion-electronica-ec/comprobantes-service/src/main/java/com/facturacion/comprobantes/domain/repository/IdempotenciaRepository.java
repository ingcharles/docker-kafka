package com.facturacion.comprobantes.domain.repository;

import java.time.Instant;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.vo.RegistroIdempotencia;

/** Claves de idempotencia de la emisión (cabecera {@code Idempotency-Key}), únicas por RUC. */
public interface IdempotenciaRepository {

    /**
     * Reserva la clave para esta solicitud. Devuelve {@code true} si es nueva (esta solicitud es la que emite) y
     * {@code false} si ya existía. Si otra transacción está creando la misma clave, espera a que termine: si
     * confirmó, devuelve {@code false}; si se deshizo, la reserva es de esta.
     */
    Uni<Boolean> reservar(String ruc, String clave, String huella);

    /** El registro de una clave, o {@code null} si no existe. */
    Uni<RegistroIdempotencia> buscar(String ruc, String clave);

    /** Anota qué comprobante creó la solicitud que reservó la clave. */
    Uni<Void> asociarComprobante(String ruc, String clave, UUID comprobanteId);

    /** Borra las claves creadas antes de {@code limite}; devuelve cuántas. */
    Uni<Long> purgarCreadasAntesDe(Instant limite);
}
