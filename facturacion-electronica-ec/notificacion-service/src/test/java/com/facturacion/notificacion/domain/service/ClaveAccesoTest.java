package com.facturacion.notificacion.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ClaveAccesoTest {

    // 09112026 | 01 | 1722039953001 | 1 | 001001 | 000000019 | 91023981 | 4  (49 dígitos)
    private static final String CLAVE = "0911202601172203995300110010010000000019910239814";

    @Test
    void extraeElRucDelEmisorDeLaClave() {
        assertEquals("1722039953001", ClaveAcceso.rucDelEmisor(CLAVE));
    }

    @Test
    void unaClaveMalFormadaNoDaRuc() {
        assertNull(ClaveAcceso.rucDelEmisor(null));
        assertNull(ClaveAcceso.rucDelEmisor(""));
        assertNull(ClaveAcceso.rucDelEmisor(CLAVE.substring(1)));
        assertNull(ClaveAcceso.rucDelEmisor(CLAVE.substring(0, 48) + "X"));
    }
}
