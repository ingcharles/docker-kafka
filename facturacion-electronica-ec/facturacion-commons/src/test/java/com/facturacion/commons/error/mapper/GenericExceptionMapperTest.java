package com.facturacion.commons.error.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.error.ApiErrorResponse;

/** El 503 por plazo vencido en la base de datos conserva el formato de error de siempre. */
class GenericExceptionMapperTest {

    @Test
    void testDeberiaResponderServicioOcupadoConElMismoFormatoQueLosDemasErrores() {
        ApiErrorResponse cuerpo = GenericExceptionMapper.construirCuerpoDeServicioOcupado();

        assertEquals(503, cuerpo.getStatus());
        assertEquals("Service Unavailable", cuerpo.getError());
        assertEquals("GEN-503-001", cuerpo.getCodigo());
        assertEquals("El servicio está ocupado en este momento: reintenta en unos segundos", cuerpo.getMessage());
        assertNull(cuerpo.getDetails());
    }
}
