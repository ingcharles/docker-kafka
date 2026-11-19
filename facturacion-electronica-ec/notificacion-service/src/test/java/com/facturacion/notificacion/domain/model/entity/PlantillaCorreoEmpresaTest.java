package com.facturacion.notificacion.domain.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;

class PlantillaCorreoEmpresaTest {

    private static final Instant ANTES = Instant.parse("2026-11-19T10:00:00Z");
    private static final Instant DESPUES = Instant.parse("2026-11-19T11:00:00Z");

    private static PlantillaCorreoEmpresa nueva() {
        return PlantillaCorreoEmpresa.crear("1722039953001", TipoComprobanteCorreo.NOTA_CREDITO, "Asunto", "<p>Hola</p>",
            null, true, ANTES);
    }

    @Test
    void laPrimeraVersionEsLaUno() {
        PlantillaCorreoEmpresa plantilla = nueva();

        assertEquals(1, plantilla.getVersion());
        assertEquals("NOTA_CREDITO", plantilla.getTipoComprobante());
        assertEquals(ANTES, plantilla.getCreadoEn());
        assertEquals(ANTES, plantilla.getActualizadoEn());
        assertTrue(plantilla.isActiva());
    }

    @Test
    void reemplazarSubeLaVersionYConservaLaFechaDeCreacion() {
        PlantillaCorreoEmpresa plantilla = nueva();

        plantilla.reemplazar("Otro", "<p>Chao</p>", "Chao", false, DESPUES);

        assertEquals(2, plantilla.getVersion());
        assertEquals("Otro", plantilla.getAsunto());
        assertEquals("<p>Chao</p>", plantilla.getCuerpoHtml());
        assertEquals("Chao", plantilla.getCuerpoTexto());
        assertEquals(false, plantilla.isActiva());
        assertEquals(ANTES, plantilla.getCreadoEn());
        assertEquals(DESPUES, plantilla.getActualizadoEn());
    }

    @Test
    void laCopiaConservaLaVersionAnteriorAunqueLaOriginalCambie() {
        PlantillaCorreoEmpresa plantilla = nueva();

        PlantillaCorreoEmpresa anterior = plantilla.copiar();
        plantilla.reemplazar("Otro", "<p>Chao</p>", null, true, DESPUES);

        assertNotSame(plantilla, anterior);
        assertEquals(1, anterior.getVersion());
        assertEquals("Asunto", anterior.getAsunto());
        assertEquals(plantilla.getId(), anterior.getId());
        assertEquals(2, plantilla.getVersion());
    }
}
