package com.facturacion.comprobantes.infrastructure.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.domain.model.vo.AnchoTicket;

/** Los anchos del papel salen de la configuración: agregar o cambiar uno no toca el código. */
class AnchosTicketConfiguradosTest {

    private static ConfiguracionTicket configuracion(Map<Integer, Integer> anchos, int porDefecto) {
        return new ConfiguracionTicket() {
            @Override
            public Map<Integer, Integer> anchos() {
                return anchos;
            }

            @Override
            public int anchoPorDefecto() {
                return porDefecto;
            }
        };
    }

    @Test
    void testDeberiaServirLosAnchosConfiguradosYElQueSeUsaPorDefecto() {
        AnchosTicketConfigurados anchos = new AnchosTicketConfigurados(configuracion(Map.of(58, 32, 80, 48), 80));

        assertEquals(new AnchoTicket(80, 48), anchos.obtenerPorDefecto());
        assertEquals(new AnchoTicket(58, 32), anchos.buscar(58).orElseThrow());
        assertTrue(anchos.buscar(70).isEmpty());
        assertEquals(List.of(58, 80), anchos.listarDisponibles());
    }

    @Test
    void testDeberiaAgregarUnPapelNuevoSoloConUnaLineaDeConfiguracion() {
        AnchosTicketConfigurados anchos = new AnchosTicketConfigurados(configuracion(Map.of(58, 32, 112, 64), 58));

        assertEquals(64, anchos.buscar(112).orElseThrow().columnas());
        assertEquals(List.of(58, 112), anchos.listarDisponibles());
    }

    @Test
    void testDeberiaFallarAlArrancarConUnaConfiguracionIncoherente() {
        assertThrows(IllegalStateException.class, () -> new AnchosTicketConfigurados(configuracion(Map.of(), 80)));
        assertThrows(IllegalStateException.class,
            () -> new AnchosTicketConfigurados(configuracion(Map.of(80, 0), 80)));
        assertThrows(IllegalStateException.class,
            () -> new AnchosTicketConfigurados(configuracion(Map.of(58, 32), 80)));
    }
}
