package com.facturacion.notificacion.domain.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TipoComprobanteCorreoTest {

    @Test
    void testDeberiaResolverElTipoDeCadaNombreDelEvento() {
        for (String nombre : new String[] {"FACTURA", "NOTA_CREDITO", "NOTA_DEBITO", "GUIA_REMISION", "RETENCION",
            "LIQUIDACION_COMPRA"}) {
            assertEquals(nombre, TipoComprobanteCorreo.obtenerPorNombre(nombre).name());
        }
    }

    @Test
    void testDeberiaAceptarEnLaBusquedaEstrictaSoloElNombreExacto() {
        assertEquals(TipoComprobanteCorreo.RETENCION, TipoComprobanteCorreo.buscarPorNombre("RETENCION").orElseThrow());
        assertEquals(TipoComprobanteCorreo.GENERICO, TipoComprobanteCorreo.buscarPorNombre("GENERICO").orElseThrow());
        for (String invalido : new String[] {null, "", "retencion", " RETENCION", "TODOS"}) {
            assertEquals(true, TipoComprobanteCorreo.buscarPorNombre(invalido).isEmpty(), String.valueOf(invalido));
        }
    }

    @Test
    void testDeberiaDevolverElTipoGenericoParaUnNombreDesconocidoONulo() {
        assertEquals(TipoComprobanteCorreo.GENERICO, TipoComprobanteCorreo.obtenerPorNombre(null));
        assertEquals(TipoComprobanteCorreo.GENERICO, TipoComprobanteCorreo.obtenerPorNombre("factura"));
        assertEquals(TipoComprobanteCorreo.GENERICO, TipoComprobanteCorreo.obtenerPorNombre("OTRO"));
    }
}
