package com.facturacion.orquestacion.domain.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.error.Mensajes;
import com.facturacion.orquestacion.shared.error.CodigoError;

/**
 * Bug real que ya ocurrió: el mensaje largo de una nota del watchdog se guardaba en step_name (varchar(100)) y
 * la base lo rechazaba, con lo que el watchdog nunca avanzaba. El nombre del paso es el código corto y el detalle va
 * en payload.
 */
class SagaStepTest {

    private static final int LONGITUD_MAXIMA_STEP_NAME = 100;

    @Test
    void testDeberiaLaNotaGuardaElCodigoCortoEnElNombreYElMensajeLargoEnElPayload() {
        String mensaje = Mensajes.obtenerTexto(CodigoError.SAGA_EXPIRADA, "FIRMADO", 3);
        SagaStep nota = SagaStep.crearNota(UUID.randomUUID(), CodigoError.SAGA_EXPIRADA.codigo(), mensaje);

        assertEquals("NOTA", nota.getStatus());
        assertEquals(CodigoError.SAGA_EXPIRADA.codigo(), nota.getStepName());
        assertEquals(mensaje, nota.getPayload());
        assertTrue(mensaje.length() > LONGITUD_MAXIMA_STEP_NAME, "el mensaje real supera el límite de step_name");
        assertTrue(nota.getStepName().length() <= LONGITUD_MAXIMA_STEP_NAME);
    }

    @Test
    void testDeberiaNingunCodigoDeErrorSuperaElLimiteDeStepName() {
        for (CodigoError codigo : CodigoError.values()) {
            assertTrue(codigo.codigo().length() <= LONGITUD_MAXIMA_STEP_NAME, codigo.name());
        }
    }
}
