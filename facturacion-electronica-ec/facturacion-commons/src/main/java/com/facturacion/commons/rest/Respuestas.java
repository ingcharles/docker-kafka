package com.facturacion.commons.rest;

import java.net.URI;

import jakarta.ws.rs.core.Response;

/**
 * Único lugar donde se arma la respuesta de un endpoint que escribe, para que todos los servicios respondan igual:
 * <ul>
 *   <li><b>Crear</b> ({@code POST} que crea un recurso): {@code 201} + el recurso creado en el cuerpo +
 *   {@code Location} cuando ese recurso se puede consultar con un {@code GET} ({@link #creado(URI, Object)}); si no
 *   existe ese {@code GET}, {@code 201} + cuerpo sin {@code Location} ({@link #creado(Object)}).</li>
 *   <li><b>Actualizar</b> ({@code PUT}/{@code PATCH}): {@code 200} + el recurso ya actualizado
 *   ({@link #actualizado(Object)}).</li>
 *   <li><b>Borrar o ejecutar una orden sin resultado que mostrar</b> ({@code DELETE}, cambiar contraseña, revocar):
 *   {@code 204} sin cuerpo ({@link #sinContenido()}).</li>
 *   <li><b>Orden que se atiende de forma asíncrona</b> (p. ej. reintentar una saga): {@code 202} sin cuerpo
 *   ({@link #aceptado()}).</li>
 *   <li><b>Errores</b>: siempre {@code ApiErrorResponse} (lanzando una {@code ApiException}), nunca un código a mano.</li>
 * </ul>
 * Los endpoints que escriben devuelven {@code Uni<Response>} armado con estos métodos (ver ARQUITECTURA.md, 14.1).
 */
public final class Respuestas {

    private Respuestas() {
    }

    /** 201 con el recurso creado y su {@code Location}. */
    public static Response creado(URI ubicacion, Object recurso) {
        return Response.created(ubicacion).entity(recurso).build();
    }

    /** 201 con el recurso creado, para los que no tienen un GET propio al que apuntar. */
    public static Response creado(Object recurso) {
        return Response.status(Response.Status.CREATED).entity(recurso).build();
    }

    /** 200 con el recurso tal como quedó después de actualizarlo. */
    public static Response actualizado(Object recurso) {
        return Response.ok(recurso).build();
    }

    /** 204: se hizo y no hay nada que devolver. */
    public static Response sinContenido() {
        return Response.noContent().build();
    }

    /** 202: la orden se aceptó y se atiende de forma asíncrona. */
    public static Response aceptado() {
        return Response.accepted().build();
    }

    /** 202 con cuerpo: la orden se aceptó y se atiende de forma asíncrona, y el cuerpo dice cuánto se pidió (p. ej. un lote). */
    public static Response aceptado(Object cuerpo) {
        return Response.accepted(cuerpo).build();
    }
}
