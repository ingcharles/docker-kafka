package com.facturacion.commons.error.mapper;

import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.facturacion.commons.error.ApiErrorResponse;
import com.facturacion.commons.error.CodigoLogComun;
import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.EstadoSql;
import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.error.Mensajes;

/**
 * Manejador de respaldo (catch-all): las excepciones propias del framework ({@link WebApplicationException}, p. ej.
 * un 404 de ruta inexistente) conservan su código de estado. Si la base de datos venció un plazo (bloqueo, sentencia o
 * transacción inactiva: ver {@link EstadoSql}) el cliente recibe un 503 con el mismo formato de error y
 * {@code Retry-After}: la operación no se hizo y puede reintentar. Cualquier otra excepción no mapeada se convierte en
 * un 500 genérico sin exponer el stack trace al cliente.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);
    /** Segundos que se le sugiere esperar al cliente antes de reintentar (cabecera Retry-After). */
    static final String ESPERA_REINTENTO_SEGUNDOS = "5";

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webEx) {
            return webEx.getResponse();
        }
        Optional<String> plazoVencido = EstadoSql.buscarEn(exception).filter(EstadoSql::esPlazoVencido);
        if (plazoVencido.isPresent()) {
            LogCodificado.warn(LOG, CodigoLogComun.BASE_DE_DATOS_OCUPADA, null, plazoVencido.get());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(construirCuerpoDeServicioOcupado())
                .header(HttpHeaders.RETRY_AFTER, ESPERA_REINTENTO_SEGUNDOS).type(MediaType.APPLICATION_JSON).build();
        }
        LogCodificado.error(LOG, exception, CodigoLogComun.EXCEPCION_NO_CONTROLADA, null);
        ApiErrorResponse body = new ApiErrorResponse(500, "Internal Server Error",
            Mensajes.obtenerTexto(ErrorComun.ERROR_INTERNO), null, ErrorComun.ERROR_INTERNO.codigo());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(body).type(MediaType.APPLICATION_JSON).build();
    }

    /** Cuerpo del 503 por plazo vencido en la base de datos, separado de {@link Response} para probarlo sin JAX-RS. */
    static ApiErrorResponse construirCuerpoDeServicioOcupado() {
        return new ApiErrorResponse(ErrorComun.SERVICIO_OCUPADO.estadoHttp(),
            Response.Status.SERVICE_UNAVAILABLE.getReasonPhrase(), Mensajes.obtenerTexto(ErrorComun.SERVICIO_OCUPADO), null,
            ErrorComun.SERVICIO_OCUPADO.codigo());
    }
}
