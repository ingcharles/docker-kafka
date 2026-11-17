package com.facturacion.comprobantes.domain.model.enums;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

class CombustibleSriTest {

    private static DetalleComprobante linea(String codigoPrincipal) {
        DetalleComprobante detalle = new DetalleComprobante();
        detalle.setCodigoPrincipal(codigoPrincipal);
        return detalle;
    }

    @Test
    void reconoceLosCincoCodigosDeLaTabla30() {
        for (String codigo : List.of("0103", "0101", "0174", "0121", "0104")) {
            assertTrue(CombustibleSri.esCodigo(codigo), codigo);
        }
        assertFalse(CombustibleSri.esCodigo("TABLET02"));
        assertFalse(CombustibleSri.esCodigo(null));
    }

    @Test
    void ventaDeCombustibleSinPlacaSeRechazaConElCodigoDeLaLinea() {
        for (String placa : new String[] {null, "", "  "}) {
            ErrorNegocioException e = assertThrows(ErrorNegocioException.class, () ->
                CombustibleSri.exigirPlacaSiHayCombustible(List.of(linea("TABLET02"), linea("0103")), placa));

            assertEquals(CodigoError.COMBUSTIBLE_SIN_PLACA, e.getError());
        }
    }

    @Test
    void conPlacaOSinCombustibleNoHayNadaQueExigir() {
        assertDoesNotThrow(() -> CombustibleSri.exigirPlacaSiHayCombustible(List.of(linea("0103")), "ABC1234"));
        assertDoesNotThrow(() -> CombustibleSri.exigirPlacaSiHayCombustible(List.of(linea("TABLET02")), null));
    }
}
