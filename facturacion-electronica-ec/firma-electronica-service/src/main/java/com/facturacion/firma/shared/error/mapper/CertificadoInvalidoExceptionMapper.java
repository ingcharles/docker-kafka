package com.facturacion.firma.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.firma.shared.error.ApiErrorResponse;
import com.facturacion.firma.shared.exception.CertificadoInvalidoException;

@Provider
public class CertificadoInvalidoExceptionMapper implements ExceptionMapper<CertificadoInvalidoException> {

    @Override
    public Response toResponse(CertificadoInvalidoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(400, "Bad Request", exception.getMessage(), null);
        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }
}
