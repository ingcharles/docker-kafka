package com.facturacion.commons.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.commons.error.ApiErrorResponse;
import com.facturacion.commons.exception.ApiException;

/**
 * El único mapper de las excepciones del catálogo: cualquier {@link ApiException} (NotFound, SinPermiso,
 * TokenInvalido, Conflicto, ErrorNegocio y las propias de cada servicio) se convierte aquí en la respuesta de error.
 * Estado y {@code codigo} salen de su entrada del catálogo; el texto, de la propia excepción.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException exception) {
        ApiErrorResponse cuerpo = cuerpo(exception);
        return Response.status(cuerpo.getStatus()).entity(cuerpo).build();
    }

    /** Cuerpo de la respuesta, separado de {@link Response} para poder probarlo sin runtime JAX-RS. */
    static ApiErrorResponse cuerpo(ApiException exception) {
        Response.Status estado = Response.Status.fromStatusCode(exception.getError().estadoHttp());
        if (estado == null) {
            estado = Response.Status.INTERNAL_SERVER_ERROR;
        }
        return new ApiErrorResponse(estado.getStatusCode(), estado.getReasonPhrase(), exception.getMessage(),
            exception.getDetalles().isEmpty() ? null : exception.getDetalles(), exception.getError().codigo());
    }
}
