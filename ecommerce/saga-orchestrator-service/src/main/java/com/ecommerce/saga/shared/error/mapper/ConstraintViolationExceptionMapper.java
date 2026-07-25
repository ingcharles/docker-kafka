package com.ecommerce.saga.shared.error.mapper;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.ecommerce.saga.shared.error.ApiErrorResponse;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toList());
        ApiErrorResponse body = new ApiErrorResponse(400, "Bad Request", "Validation failed", details);
        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }
}
