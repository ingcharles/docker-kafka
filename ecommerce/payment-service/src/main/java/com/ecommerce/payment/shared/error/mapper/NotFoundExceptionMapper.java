package com.ecommerce.payment.shared.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.ecommerce.payment.shared.error.ApiErrorResponse;
import com.ecommerce.payment.shared.exception.NotFoundException;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        ApiErrorResponse body = new ApiErrorResponse(404, "Not Found", exception.getMessage(), null);
        return Response.status(Response.Status.NOT_FOUND).entity(body).build();
    }
}
