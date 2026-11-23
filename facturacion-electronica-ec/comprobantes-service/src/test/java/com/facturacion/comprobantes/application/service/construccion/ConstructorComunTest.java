package com.facturacion.comprobantes.application.service.construccion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.shared.error.CodigoError;

class ConstructorComunTest {

    private final ConstructorComun comun = new ConstructorComun();

    @Test
    void testDeberiaConvertirUnaFechaValidaAlInicioDelDiaEnEcuador() {
        // 29/07/2026 00:00 en UTC-5 = 05:00 UTC
        assertEquals(Instant.parse("2026-07-29T05:00:00Z"), comun.convertirFecha("29/07/2026"));
    }

    @Test
    void testDeberiaDevolverNuloSiLaFechaEstaVaciaOEsNula() {
        assertNull(comun.convertirFecha(null));
        assertNull(comun.convertirFecha("  "));
    }

    @Test
    void testDeberiaAceptarElCorreoHastaElMaximoPorDefectoYNormalizarLaLista() {
        assertEquals("a@x.com,b@x.com,c@x.com", comun.armarCorreos("a@x.com, b@x.com ,c@x.com", "compradorCorreo"));
        ErrorNegocioException e = assertThrows(ErrorNegocioException.class,
            () -> comun.armarCorreos("a@x.com,b@x.com,c@x.com,d@x.com", "compradorCorreo"));
        assertEquals(CodigoError.CORREOS_EXCEDEN_MAXIMO, e.getCodigo());
        assertNull(comun.armarCorreos(null, "compradorCorreo"));
    }

    @Test
    void testDeberiaRechazarUnaFechaQueNoExisteEnElCalendarioEnVezDeCorregirla() {
        // antes "31/02/2026" se aceptaba y quedaba como 28/02/2026 sin avisar
        ErrorNegocioException e = assertThrows(ErrorNegocioException.class, () -> comun.convertirFecha("31/02/2026"));
        assertEquals(CodigoError.FECHA_DOCUMENTO_INVALIDA, e.getCodigo());
    }

    @Test
    void testDeberiaResponderUn400EnVezDeUn500SiElTextoNoEsUnaFecha() {
        assertThrows(ErrorNegocioException.class, () -> comun.convertirFecha("mañana"));
        assertThrows(ErrorNegocioException.class, () -> comun.convertirFecha("2026-07-29"));
    }
}
