package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.model.vo.Pago;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ComprobantePagoEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.DetalleComprobanteEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.DetalleImpuestoEntity;

/** Ida y vuelta dominio <-> filas de comprobante_pago / detalle_impuesto (sin base de datos). */
class MapeoFilasTest {

    private final ComprobanteMapper comprobanteMapper = new ComprobanteMapperImpl();
    private final DetalleComprobanteMapper detalleMapper = new DetalleComprobanteMapperImpl();

    @Test
    void testDeberiaConservarElOrdenYLosValoresDeLosPagos() {
        List<Pago> pagos = List.of(
            new Pago("01", new BigDecimal("15.00"), null, null),
            new Pago("19", new BigDecimal("100.00"), "30", "dias"),
            new Pago("21", new BigDecimal("5.50"), "100", "dias"));

        List<ComprobantePagoEntity> filas = comprobanteMapper.escribirPagos(pagos);

        assertEquals(List.of(0, 1, 2), filas.stream().map(f -> f.orden).toList());
        assertTrue(filas.stream().allMatch(f -> f.id != null), "todas las filas tienen id");
        assertEquals(3, new HashSet<>(filas.stream().map(f -> f.id).toList()).size(), "ids únicos");
        assertEquals(0, new BigDecimal("30").compareTo(filas.get(1).plazo));
        assertNull(filas.get(0).plazo);
        assertEquals(pagos, comprobanteMapper.leerPagos(filas));
    }

    @Test
    void testDeberiaDevolverElPlazoSinCerosSobrantesAunqueLaColumnaSeaNumeric142() {
        ComprobantePagoEntity fila = new ComprobantePagoEntity();
        fila.formaPago = "19";
        fila.total = new BigDecimal("10.00");
        fila.plazo = new BigDecimal("30.00"); // así lo devuelve Postgres
        fila.unidadTiempo = "dias";
        fila.orden = 0;
        assertEquals("30", comprobanteMapper.leerPagos(List.of(fila)).get(0).plazo());

        fila.plazo = new BigDecimal("100.00");
        assertEquals("100", comprobanteMapper.leerPagos(List.of(fila)).get(0).plazo());
        fila.plazo = new BigDecimal("30.50");
        assertEquals("30.5", comprobanteMapper.leerPagos(List.of(fila)).get(0).plazo());
    }

    @Test
    void testDeberiaDevolverListasVaciasPorListasNulas() {
        assertTrue(comprobanteMapper.escribirPagos(null).isEmpty());
        assertTrue(comprobanteMapper.leerPagos(null).isEmpty());
        assertTrue(detalleMapper.escribirImpuestos(null).isEmpty());
        assertTrue(detalleMapper.leerImpuestos(null).isEmpty());
    }

    @Test
    void testDeberiaConvertirElImpuestoDeFacturaDeIdaYVuelta() {
        List<Impuesto> impuestos = List.of(
            new Impuesto("2", "4", new BigDecimal("15"), new BigDecimal("100.12345678"), new BigDecimal("15.02")),
            new Impuesto("3", "3011", new BigDecimal("10"), new BigDecimal("100.12345678"), new BigDecimal("10.01")));

        List<DetalleImpuestoEntity> filas = detalleMapper.escribirImpuestos(impuestos);

        assertEquals(List.of(0, 1), filas.stream().map(f -> f.orden).toList());
        // la base se conserva sin redondear (numeric(22,8) en la base)
        assertEquals(0, new BigDecimal("100.12345678").compareTo(filas.get(0).baseImponible));
        assertNull(filas.get(0).docSustentoFecha);
        assertEquals(impuestos, detalleMapper.leerImpuestos(filas));
    }

    @Test
    void testDeberiaConvertirLaFechaDelDocumentoSustentoDeLaLineaDeRetencion() {
        Impuesto retencion = new Impuesto("1", null, new BigDecimal("10"), new BigDecimal("300.00"),
            new BigDecimal("30.00"), "312", "01", "001001000000123", "20/07/2026");

        List<DetalleImpuestoEntity> filas = detalleMapper.escribirImpuestos(List.of(retencion));

        assertEquals(LocalDate.of(2026, 7, 20), filas.get(0).docSustentoFecha);
        assertNull(filas.get(0).codigoPorcentaje);
        assertEquals("312", filas.get(0).codigoRetencion);
        assertEquals(List.of(retencion), detalleMapper.leerImpuestos(filas));
    }

    @Test
    void testDeberiaEnlazarLosImpuestosDelDetalleConLaFilaPadre() {
        DetalleComprobante detalle = new DetalleComprobante(UUID.randomUUID(), null, "P-1", null, "Producto",
            new BigDecimal("2"), new BigDecimal("50.00"), BigDecimal.ZERO,
            List.of(new Impuesto("2", "4", new BigDecimal("15"), new BigDecimal("100.00"), new BigDecimal("15.00"))));

        DetalleComprobanteEntity entidad = detalleMapper.toEntity(detalle);

        assertEquals(1, entidad.impuestos.size());
        assertNotNull(entidad.impuestos.get(0).detalle);
        assertSame(entidad, entidad.impuestos.get(0).detalle);
        assertEquals(detalle.getImpuestos(), detalleMapper.toDomain(entidad).getImpuestos());
    }
}
