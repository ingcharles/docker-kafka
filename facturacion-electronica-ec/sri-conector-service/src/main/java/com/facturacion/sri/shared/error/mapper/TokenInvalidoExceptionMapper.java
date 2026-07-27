package com.facturacion.sri.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.sri.shared.error.ApiErrorResponse;
import com.facturacion.sri.shared.exception.TokenInvalidoException;

@Provider
public class TokenInvalidoExceptionMapper implements ExceptionMapper<TokenInvalidoException> {

    @Override
    public Response toResponse(TokenInvalidoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(401, "Unauthorized", exception.getMessage(), null);
        return Response.status(Response.Status.UNAUTHORIZED).entity(body).build();
    }
}
