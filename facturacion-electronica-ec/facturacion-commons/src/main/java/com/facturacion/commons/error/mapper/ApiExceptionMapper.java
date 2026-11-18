package com.facturacion.commons.error.mapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.facturacion.commons.error.ApiErrorResponse;
import com.facturacion.commons.error.CodigoLogComun;
import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.error.Mensajes;
import com.facturacion.commons.exception.ApiException;

/**
 * El único mapper de las excepciones del catálogo: cualquier {@link ApiException} (NotFound, SinPermiso,
 * TokenInvalido, Conflicto, ErrorNegocio, ErrorInterno y las propias de cada servicio) se convierte aquí en la
 * respuesta de error. Estado y {@code codigo} salen de su entrada del catálogo; el texto, de la propia excepción.
 * <p>
 * Los errores 5xx son fallas internas: se registran con su código y su causa (GEN-403) y al cliente solo le llega el
 * texto genérico con el código específico, sin detalles internos.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);

    @Override
    public Response toResponse(ApiException exception) {
        if (esFalloInterno(exception)) {
            LogCodificado.error(LOG, exception, CodigoLogComun.FALLO_INTERNO, null, exception.getError().codigo(),
                exception.getMessage());
        }
        ApiErrorResponse cuerpo = construirCuerpo(exception);
        return Response.status(cuerpo.getStatus()).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }

    private static boolean esFalloInterno(ApiException exception) {
        return exception.getError().estadoHttp() >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    /** Cuerpo de la respuesta, separado de {@link Response} para poder probarlo sin runtime JAX-RS. */
    static ApiErrorResponse construirCuerpo(ApiException exception) {
        Response.Status estado = Response.Status.fromStatusCode(exception.getError().estadoHttp());
        if (estado == null) {
            estado = Response.Status.INTERNAL_SERVER_ERROR;
        }
        if (esFalloInterno(exception)) {
            return new ApiErrorResponse(estado.getStatusCode(), estado.getReasonPhrase(),
                Mensajes.obtenerTexto(ErrorComun.ERROR_INTERNO), null, exception.getError().codigo());
        }
        return new ApiErrorResponse(estado.getStatusCode(), estado.getReasonPhrase(), exception.getMessage(),
            exception.getDetalles().isEmpty() ? null : exception.getDetalles(), exception.getError().codigo());
    }
}
