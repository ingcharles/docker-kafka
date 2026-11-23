package com.facturacion.commons.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PagedResponseTest {

    @Test
    void testDeberiaCalcularElTotalDePaginasRedondeandoHaciaArriba() {
        PagedResponse<String> r = PagedResponse.of(List.of("a"), 0, 20, 41);
        assertEquals(3, r.getTotalPages());
        assertEquals(41, r.getTotal());
    }

    @Test
    void testDeberiaNoDividirEntreCeroSiElLimiteEsCero() {
        assertEquals(0, PagedResponse.of(List.of(), 0, 0, 10).getTotalPages());
    }
}
