package com.facturacion.commons.error.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.error.ApiErrorResponse;
import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.commons.exception.SinPermisoException;
import com.facturacion.commons.exception.TokenInvalidoException;

/** Cada excepción genérica sale con el estado HTTP y el código de su entrada del catálogo. */
class ApiExceptionMapperTest {

    @Test
    void testDeberiaNotFoundConTextoLiteralSale404ConCodigoGenerico() {
        ApiErrorResponse cuerpo = ApiExceptionMapper.construirCuerpo(new NotFoundException(ErrorComun.RECURSO_NO_ENCONTRADO, "Comprobante no encontrado: 7"));
        assertEquals(404, cuerpo.getStatus());
        assertEquals("Not Found", cuerpo.getError());
        assertEquals("Comprobante no encontrado: 7", cuerpo.getMessage());
        assertEquals("GEN-404-001", cuerpo.getCodigo());
        assertNull(cuerpo.getDetails());
    }

    @Test
    void testDeberiaCadaExcepcionGenericaTraeSuEstado() {
        assertEquals(401, ApiExceptionMapper.construirCuerpo(new TokenInvalidoException(ErrorComun.TOKEN_INVALIDO, "x")).getStatus());
        assertEquals(403, ApiExceptionMapper.construirCuerpo(new SinPermisoException(ErrorComun.SIN_PERMISO, "x")).getStatus());
        assertEquals(409, ApiExceptionMapper.construirCuerpo(new ConflictoException(ErrorComun.CONFLICTO, "x")).getStatus());
    }

    /** Un estado que JAX-RS no trae en {@code Response.Status} (422) sale con su código y frase, no como un 500. */
    private enum ErrorDePrueba implements com.facturacion.commons.error.ErrorCatalogo {
        ENTIDAD_NO_PROCESABLE;

        @Override
        public String codigo() {
            return "PRUEBA-422-001";
        }

        @Override
        public int estadoHttp() {
            return 422;
        }

        @Override
        public String claveMensaje() {
            return "comun.conflicto";
        }
    }

    @Test
    void testDeberiaUnEstadoNoEstandarComoElCuatrocientosVeintidosSeConserva() {
        ApiErrorResponse cuerpo = ApiExceptionMapper.construirCuerpo(
            new ErrorNegocioException(ErrorDePrueba.ENTIDAD_NO_PROCESABLE));

        assertEquals(422, cuerpo.getStatus());
        assertEquals("Unprocessable Entity", cuerpo.getError());
        assertEquals("PRUEBA-422-001", cuerpo.getCodigo());
    }

    @Test
    void testDeberiaErrorNegocioUsaLaEntradaDelCatalogoYSusDetalles() {
        ApiErrorResponse cuerpo = ApiExceptionMapper.construirCuerpo(
            ErrorNegocioException.crearConDetalles(ErrorComun.SOLICITUD_INVALIDA, List.of("a", "b"), "varios problemas"));
        assertEquals(400, cuerpo.getStatus());
        assertEquals("GEN-400-002", cuerpo.getCodigo());
        assertEquals("varios problemas", cuerpo.getMessage());
        assertEquals(List.of("a", "b"), cuerpo.getDetails());
    }

    @Test
    void testDeberiaUnFalloInternoNoExponeSuDetalleYConservaSuCodigo() {
        ApiErrorResponse cuerpo = ApiExceptionMapper.construirCuerpo(
            new ErrorInternoException(new RuntimeException("detalle interno"), ErrorComun.PAYLOAD_NO_SERIALIZABLE));

        assertEquals(500, cuerpo.getStatus());
        assertEquals("GEN-500-002", cuerpo.getCodigo());
        assertEquals("Ocurrió un error inesperado", cuerpo.getMessage());
    }
}
