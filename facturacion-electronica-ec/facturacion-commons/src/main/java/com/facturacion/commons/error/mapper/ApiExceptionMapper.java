package com.facturacion.commons.error.mapper;

import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
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
import com.facturacion.commons.exception.LimiteExcedidoException;

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

    private static final int HTTP_ENTIDAD_NO_PROCESABLE = 422;
    private static final int HTTP_DEMASIADAS_PETICIONES = 429;

    /** Estados HTTP que JAX-RS no trae en {@link Response.Status}, con su frase estándar. */
    private static final Map<Integer, String> FRASES_NO_ESTANDAR = Map.of(
        HTTP_ENTIDAD_NO_PROCESABLE, "Unprocessable Entity",
        HTTP_DEMASIADAS_PETICIONES, "Too Many Requests");

    @Override
    public Response toResponse(ApiException exception) {
        if (esFalloInterno(exception)) {
            LogCodificado.error(LOG, exception, CodigoLogComun.FALLO_INTERNO, null, exception.getError().codigo(),
                exception.getMessage());
        }
        ApiErrorResponse cuerpo = construirCuerpo(exception);
        Response.ResponseBuilder respuesta = Response.status(cuerpo.getStatus()).entity(cuerpo)
            .type(MediaType.APPLICATION_JSON);
        if (exception instanceof LimiteExcedidoException limite) {
            respuesta.header(HttpHeaders.RETRY_AFTER, limite.getSegundosEspera());
        }
        return respuesta.build();
    }

    private static boolean esFalloInterno(ApiException exception) {
        return exception.getError().estadoHttp() >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    /** Cuerpo de la respuesta, separado de {@link Response} para poder probarlo sin runtime JAX-RS. */
    static ApiErrorResponse construirCuerpo(ApiException exception) {
        int codigoHttp = exception.getError().estadoHttp();
        String frase = obtenerFrase(codigoHttp);
        if (frase == null) {
            codigoHttp = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            frase = Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
        }
        if (esFalloInterno(exception)) {
            return new ApiErrorResponse(codigoHttp, frase,
                Mensajes.obtenerTexto(ErrorComun.ERROR_INTERNO), null, exception.getError().codigo());
        }
        return new ApiErrorResponse(codigoHttp, frase, exception.getMessage(),
            exception.getDetalles().isEmpty() ? null : exception.getDetalles(), exception.getError().codigo());
    }

    /** La frase estándar del estado, o {@code null} si no es un estado conocido. */
    private static String obtenerFrase(int codigoHttp) {
        Response.Status conocido = Response.Status.fromStatusCode(codigoHttp);
        return conocido != null ? conocido.getReasonPhrase() : FRASES_NO_ESTANDAR.get(codigoHttp);
    }
}
