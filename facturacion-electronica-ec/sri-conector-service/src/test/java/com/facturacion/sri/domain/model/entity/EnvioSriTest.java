package com.facturacion.sri.domain.model.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;

class EnvioSriTest {

    private static EnvioSri envio(EstadoEnvioSri estado, int intentos) {
        EnvioSri envio = new EnvioSri();
        envio.setEstado(estado);
        envio.setIntentosAutorizacion(intentos);
        return envio;
    }

    @Test
    void testDeberiaAplicarLaConsultaSiElEnvioSigueRecibidoYSinNuevosIntentos() {
        assertTrue(envio(EstadoEnvioSri.RECIBIDO, 3).esConsultaVigente(3));
    }

    @Test
    void testDeberiaDescartarLaConsultaSiOtraInstanciaYaSumoUnIntento() {
        assertFalse(envio(EstadoEnvioSri.RECIBIDO, 4).esConsultaVigente(3));
    }

    @Test
    void testDeberiaDescartarLaConsultaSiOtraInstanciaYaResolvioElEnvio() {
        assertFalse(envio(EstadoEnvioSri.AUTORIZADO, 4).esConsultaVigente(3));
        assertFalse(envio(EstadoEnvioSri.NO_AUTORIZADO, 4).esConsultaVigente(3));
        assertFalse(envio(EstadoEnvioSri.ERROR_CONSULTA, 20).esConsultaVigente(19));
    }
}
