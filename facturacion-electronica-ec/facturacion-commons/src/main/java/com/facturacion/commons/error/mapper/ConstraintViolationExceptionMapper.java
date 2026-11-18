package com.facturacion.commons.error.mapper;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.facturacion.commons.error.ApiErrorResponse;
import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.error.Mensajes;

/** Bean Validation (@Valid, @Min...) → 400 con el detalle de cada campo. */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toList());
        ApiErrorResponse body = new ApiErrorResponse(400, "Bad Request", Mensajes.obtenerTexto(ErrorComun.VALIDACION_FALLIDA),
            details, ErrorComun.VALIDACION_FALLIDA.codigo());
        return Response.status(Response.Status.BAD_REQUEST).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
