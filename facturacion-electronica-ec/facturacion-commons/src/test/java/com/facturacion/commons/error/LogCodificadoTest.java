package com.facturacion.commons.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LogCodificadoTest {

    private static final MensajeCatalogo EVENTO = new MensajeCatalogo() {
        @Override
        public String codigo() {
            return "GEN-401";
        }

        @Override
        public String claveMensaje() {
            return ErrorComun.VALIDACION_FALLIDA.claveMensaje();
        }
    };

    @Test
    void testDeberiaRegistrarElLogConCodigoTextoDelCatalogoYContexto() {
        assertEquals("[GEN-401] Falló la validación (comprobanteId=abc)",
            LogCodificado.formatear(EVENTO, "comprobanteId=abc"));
    }

    @Test
    void testDeberiaOmitirLosParentesisSiNoHayContexto() {
        assertEquals("[GEN-401] Falló la validación", LogCodificado.formatear(EVENTO, null));
        assertEquals("[GEN-401] Falló la validación", LogCodificado.formatear(EVENTO, " "));
    }
}
