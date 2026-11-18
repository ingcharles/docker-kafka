package com.facturacion.commons.exception;

import java.util.List;

import com.facturacion.commons.error.ErrorCatalogo;
import com.facturacion.commons.error.Mensajes;

/**
 * Base de toda excepción que termina en una respuesta HTTP de error. Lleva su entrada del catálogo
 * ({@link ErrorCatalogo}: código estable + estado HTTP) y el {@code ApiExceptionMapper} la convierte en la
 * respuesta; por eso el código de negocio solo hace {@code throw new XxxException(...)} y nunca arma un
 * {@code Response}.
 * <p>
 * Se crea siempre con la entrada del catálogo y sus parámetros (el texto sale del {@code .properties}; nunca un texto
 * armado por quien lanza). {@code detalles} lleva los problemas
 * individuales cuando un mismo error agrupa varios.
 */
public class ApiException extends RuntimeException {

    private final transient ErrorCatalogo error;
    private final List<String> detalles;

    /** El texto sale de {@code mensajes*.properties} con los {@code parametros} de la entrada. */
    public ApiException(ErrorCatalogo error, Object... parametros) {
        super(Mensajes.obtenerTexto(error, parametros));
        this.error = error;
        this.detalles = List.of();
    }

    /** Igual que la anterior y conserva la causa original (para el log o para encadenar). */
    protected ApiException(ErrorCatalogo error, Throwable causa, Object[] parametros) {
        super(Mensajes.obtenerTexto(error, parametros), causa);
        this.error = error;
        this.detalles = List.of();
    }

    protected ApiException(ErrorCatalogo error, List<String> detalles, Object[] parametros) {
        super(Mensajes.obtenerTexto(error, parametros));
        this.error = error;
        this.detalles = List.copyOf(detalles);
    }

    public ErrorCatalogo getError() {
        return error;
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
