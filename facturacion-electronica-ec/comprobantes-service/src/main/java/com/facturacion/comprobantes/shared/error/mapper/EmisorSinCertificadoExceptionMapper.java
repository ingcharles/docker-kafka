package com.facturacion.comprobantes.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.comprobantes.shared.error.ApiErrorResponse;
import com.facturacion.comprobantes.shared.exception.EmisorSinCertificadoException;

@Provider
public class EmisorSinCertificadoExceptionMapper implements ExceptionMapper<EmisorSinCertificadoException> {

    @Override
    public Response toResponse(EmisorSinCertificadoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(404, "Not Found", exception.getMessage(), null);
        return Response.status(Response.Status.NOT_FOUND).entity(body).build();
    }
}
