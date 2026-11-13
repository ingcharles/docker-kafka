package com.facturacion.commons.error;

import java.time.Instant;
import java.util.List;

/** Cuerpo de TODA respuesta de error de los microservicios. */
public class ApiErrorResponse {

    private Instant timestamp = Instant.now();
    private int status;
    private String error;
    private String message;
    private List<String> details;
    /** Código estable del catálogo (ver {@link ErrorCatalogo}); null solo en errores del framework sin código propio. */
    private String codigo;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(int status, String error, String message, List<String> details) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.details = details;
    }

    public ApiErrorResponse(int status, String error, String message, List<String> details, String codigo) {
        this(status, error, message, details);
        this.codigo = codigo;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
