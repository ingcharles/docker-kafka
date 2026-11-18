package com.facturacion.comprobantes.application.service.construccion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.application.dto.CrearGuiaRemisionRequest;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.shared.error.CodigoError;

/** El SRI toma el inicio del transporte como la fecha de emisión de la guía: otra fecha se rechaza antes de emitir. */
class ConstructorGuiaRemisionTest {

    private final ConstructorGuiaRemision constructor = new ConstructorGuiaRemision(new ConstructorComun());
    private final CatalogosSriAFecha catalogos = new CatalogosSriAFecha(FechasSri.obtenerHoy(), List.of(), List.of(),
        List.of(), List.of(), List.of(), List.of(), List.of());

    private CrearGuiaRemisionRequest guiaConInicio(LocalDate inicio) {
        CrearGuiaRemisionRequest request = new CrearGuiaRemisionRequest();
        request.setFechaIniTransporte(FechasSri.formatearFecha(inicio));
        return request;
    }

    @Test
    void unInicioDeTransporteDeAyerOMananaSeRechazaConLasDosFechas() {
        for (LocalDate otraFecha : List.of(FechasSri.obtenerHoy().minusDays(1), FechasSri.obtenerHoy().plusDays(1))) {
            ErrorNegocioException error = assertThrows(ErrorNegocioException.class,
                () -> constructor.construir(guiaConInicio(otraFecha), 1L, catalogos));

            assertEquals(CodigoError.GUIA_INICIO_TRANSPORTE_NO_ES_LA_EMISION, error.getCodigo());
            assertEquals(true, error.getMessage().contains(FechasSri.formatearFecha(otraFecha)));
            assertEquals(true, error.getMessage().contains(FechasSri.formatearFecha(FechasSri.obtenerHoy())));
        }
    }
}
