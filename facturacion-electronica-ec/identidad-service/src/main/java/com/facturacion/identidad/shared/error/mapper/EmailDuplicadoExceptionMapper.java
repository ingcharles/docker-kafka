package com.facturacion.identidad.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.identidad.shared.error.ApiErrorResponse;
import com.facturacion.identidad.shared.exception.EmailDuplicadoException;

@Provider
public class EmailDuplicadoExceptionMapper implements ExceptionMapper<EmailDuplicadoException> {

    @Override
    public Response toResponse(EmailDuplicadoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(409, "Conflict", exception.getMessage(), null);
        return Response.status(Response.Status.CONFLICT).entity(body).build();
    }
}
