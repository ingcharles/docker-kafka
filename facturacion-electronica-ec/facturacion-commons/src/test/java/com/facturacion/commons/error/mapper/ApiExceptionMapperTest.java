package com.facturacion.commons.error.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.error.ApiErrorResponse;
import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.commons.exception.SinPermisoException;
import com.facturacion.commons.exception.TokenInvalidoException;

/** Cada excepción genérica sale con el estado HTTP y el código de su entrada del catálogo. */
class ApiExceptionMapperTest {

    @Test
    void notFoundConTextoLiteralSale404ConCodigoGenerico() {
        ApiErrorResponse cuerpo = ApiExceptionMapper.construirCuerpo(new NotFoundException("Comprobante no encontrado: 7"));
        assertEquals(404, cuerpo.getStatus());
        assertEquals("Not Found", cuerpo.getError());
        assertEquals("Comprobante no encontrado: 7", cuerpo.getMessage());
        assertEquals("GEN-404-001", cuerpo.getCodigo());
        assertNull(cuerpo.getDetails());
    }

    @Test
    void cadaExcepcionGenericaTraeSuEstado() {
        assertEquals(401, ApiExceptionMapper.construirCuerpo(new TokenInvalidoException("x")).getStatus());
        assertEquals(403, ApiExceptionMapper.construirCuerpo(new SinPermisoException("x")).getStatus());
        assertEquals(409, ApiExceptionMapper.construirCuerpo(new ConflictoException("x")).getStatus());
    }

    @Test
    void errorNegocioUsaLaEntradaDelCatalogoYSusDetalles() {
        ApiErrorResponse cuerpo = ApiExceptionMapper.construirCuerpo(
            ErrorNegocioException.conDetalles(ErrorComun.SOLICITUD_INVALIDA, List.of("a", "b"), "varios problemas"));
        assertEquals(400, cuerpo.getStatus());
        assertEquals("GEN-400-002", cuerpo.getCodigo());
        assertEquals("varios problemas", cuerpo.getMessage());
        assertEquals(List.of("a", "b"), cuerpo.getDetails());
    }
}
