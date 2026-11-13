package com.facturacion.comprobantes.application.service.construccion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

class ConstructorComunTest {

    private final ConstructorComun comun = new ConstructorComun();

    @Test
    void unaFechaValidaSeConvierteAlInicioDelDiaEnEcuador() {
        // 29/07/2026 00:00 en UTC-5 = 05:00 UTC
        assertEquals(Instant.parse("2026-07-29T05:00:00Z"), comun.parseFecha("29/07/2026"));
    }

    @Test
    void unaFechaVaciaONulaDaNulo() {
        assertNull(comun.parseFecha(null));
        assertNull(comun.parseFecha("  "));
    }

    @Test
    void unaFechaQueNoExisteEnElCalendarioSeRechazaEnVezDeCorregirla() {
        // antes "31/02/2026" se aceptaba y quedaba como 28/02/2026 sin avisar
        ErrorNegocioException e = assertThrows(ErrorNegocioException.class, () -> comun.parseFecha("31/02/2026"));
        assertEquals(CodigoError.FECHA_DOCUMENTO_INVALIDA, e.getCodigo());
    }

    @Test
    void unTextoQueNoEsFechaDaUn400EnVezDeUn500() {
        assertThrows(ErrorNegocioException.class, () -> comun.parseFecha("mañana"));
        assertThrows(ErrorNegocioException.class, () -> comun.parseFecha("2026-07-29"));
    }
}
