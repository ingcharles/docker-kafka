package com.facturacion.orquestacion.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.orquestacion.shared.error.ApiErrorResponse;
import com.facturacion.orquestacion.shared.exception.TokenInvalidoException;

@Provider
public class TokenInvalidoExceptionMapper implements ExceptionMapper<TokenInvalidoException> {

    @Override
    public Response toResponse(TokenInvalidoException exception) {
        ApiErrorResponse body = new ApiErrorResponse(401, "Unauthorized", exception.getMessage(), null);
        return Response.status(Response.Status.UNAUTHORIZED).entity(body).build();
    }
}
