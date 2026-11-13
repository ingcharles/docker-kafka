package com.facturacion.comprobantes.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.facturacion.comprobantes.domain.model.enums.ImpuestoSri;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoIdentificacionSri;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.FormaPagoRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.ImpuestoRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.RetencionRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.SustentoRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.TarifaRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.TipoIdentificacionRow;
import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

class CatalogosValidadorTest {

    private static final LocalDate SIN_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 5);
    private static final String IVA = ImpuestoSri.IVA.codigo();
    private static final String ICE = ImpuestoSri.ICE.codigo();
    /** Impuestos a retener (Tabla 19). */
    private static final String RENTA = "1";
    private static final String RETENCION_IVA = "2";
    private static final String ISD = "6";
    private static final String RUC = TipoIdentificacionSri.RUC.codigo();
    private static final String CONSUMIDOR_FINAL = TipoIdentificacionSri.CONSUMIDOR_FINAL.codigo();

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    private static CatalogosSriAFecha catalogos(LocalDate fecha, List<TarifaRow> tarifas,
            List<RetencionRow> retenciones, List<SustentoRow> sustentos) {
        return new CatalogosSriAFecha(fecha,
            List.of(new FormaPagoRow("01", "SIN UTILIZACION DEL SISTEMA FINANCIERO", SIN_INICIO, null, true),
                new FormaPagoRow("19", "TARJETA DE CREDITO", SIN_INICIO, LocalDate.of(2020, 1, 1), true)),
            List.of(new ImpuestoRow(IVA, "IVA", true), new ImpuestoRow(ICE, "ICE", true)),
            tarifas,
            List.of(new TipoIdentificacionRow(RUC, "RUC", false, true),
                new TipoIdentificacionRow(CONSUMIDOR_FINAL, "VENTA A CONSUMIDOR FINAL", true, true)),
            List.of(new ImpuestoRow(RENTA, "RENTA", true), new ImpuestoRow(RETENCION_IVA, "IVA", true),
                new ImpuestoRow(ISD, "ISD", true)),
            retenciones, sustentos);
    }

    private static final List<TarifaRow> TARIFAS_IVA = List.of(
        new TarifaRow(IVA, "4", "IVA 15%", d("15.00"), SIN_INICIO, null, true),
        new TarifaRow(IVA, "3", "IVA 14%", d("14.00"), SIN_INICIO, LocalDate.of(2017, 5, 31), true),
        new TarifaRow(IVA, "8", "IVA diferenciado", d("8.00"), SIN_INICIO, null, false));

    private static final List<RetencionRow> RETENCIONES = List.of(
        new RetencionRow(RETENCION_IVA, "9", SIN_INICIO, "Retención IVA 10%", d("10.00"), null, true),
        new RetencionRow(ISD, "4580", SIN_INICIO, "ISD 5%", d("5.00"), LocalDate.of(2021, 12, 31), true),
        new RetencionRow(ISD, "4580", LocalDate.of(2024, 4, 1), "ISD 5%", d("5.00"), null, true));

    private final CatalogosSriAFecha completo = catalogos(HOY, TARIFAS_IVA, RETENCIONES, List.of());

    /** Debe lanzar un ErrorNegocioException con ese código. */
    private static void rechaza(Executable accion, CodigoError esperado) {
        ErrorNegocioException error = assertThrows(ErrorNegocioException.class, accion);
        assertEquals(esperado, error.getCodigo());
    }

    private static void assertTarifa(String esperada, BigDecimal real) {
        assertEquals(0, d(esperada).compareTo(real), "tarifa esperada " + esperada + " pero fue " + real);
    }

    // ---- formas de pago ----

    @Test
    void formasDePagoVigentesExcluyenLasQueYaTerminaronLaVigencia() {
        assertEquals(Set.of("01"), completo.formasPagoVigentes());
        assertEquals("TARJETA DE CREDITO", completo.descripcionFormaPago("19"));
        assertEquals("FORMA DE PAGO 05", completo.descripcionFormaPago("05"));
    }

    // ---- tarifas ----

    @Test
    void tarifaSeTomaDelCatalogoCuandoElClienteNoLaManda() {
        assertTarifa("15", CatalogosValidador.resolverTarifa(completo, IVA, "4", null));
    }

    @Test
    void tarifaDelClienteDebeCoincidirConElCatalogo() {
        assertTarifa("15", CatalogosValidador.resolverTarifa(completo, IVA, "4", d("15")));
        rechaza(() -> CatalogosValidador.resolverTarifa(completo, IVA, "4", d("12")), CodigoError.TARIFA_NO_COINCIDE);
    }

    @Test
    void rechazaCodigoPorcentajeInexistenteVencidoOInactivo() {
        for (String codigoPorcentaje : List.of("99", "3", "8")) { // inexistente, vencido (2017), inactivo
            rechaza(() -> CatalogosValidador.resolverTarifa(completo, IVA, codigoPorcentaje, null),
                CodigoError.PORCENTAJE_IMPUESTO_NO_VIGENTE);
        }
    }

    @Test
    void vigenciaSeEvaluaALaFechaDeEmision() {
        CatalogosSriAFecha en2016 = catalogos(LocalDate.of(2016, 12, 1), TARIFAS_IVA, RETENCIONES, List.of());
        assertTarifa("14", CatalogosValidador.resolverTarifa(en2016, IVA, "3", null));
    }

    @Test
    void rechazaImpuestoInexistente() {
        rechaza(() -> CatalogosValidador.resolverTarifa(completo, "9", "1", d("1")), CodigoError.IMPUESTO_NO_EXISTE);
    }

    @Test
    void impuestosSinTarifasEnElCatalogoUsanLaDelRequest() {
        // ICE: el catálogo no trae sus tarifas; la tarifa viaja con el request / el ítem
        assertTarifa("75", CatalogosValidador.resolverTarifa(completo, ICE, "3011", d("75")));
        rechaza(() -> CatalogosValidador.resolverTarifa(completo, ICE, "3011", null),
            CodigoError.TARIFA_NO_DISPONIBLE);
    }

    @Test
    void conReplicaVaciaElIvaSinTarifaDelClienteFallaConCodigoPropio() {
        CatalogosSriAFecha vacio = new CatalogosSriAFecha(HOY, List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of());
        rechaza(() -> CatalogosValidador.resolverTarifa(vacio, IVA, "4", null), CodigoError.TARIFA_NO_DISPONIBLE);
        // y si el cliente manda la tarifa, se acepta sin validar (no hay catálogo contra el que validar)
        assertTarifa("15", CatalogosValidador.resolverTarifa(vacio, IVA, "4", d("15")));
    }

    // ---- tipo de identificación ----

    @Test
    void consumidorFinalSoloEnFactura() {
        assertDoesNotThrow(() -> CatalogosValidador.validarTipoIdentificacion(completo, CONSUMIDOR_FINAL,
            TipoComprobante.FACTURA, "x"));
        for (TipoComprobante tipo : List.of(TipoComprobante.NOTA_CREDITO, TipoComprobante.NOTA_DEBITO,
            TipoComprobante.RETENCION, TipoComprobante.LIQUIDACION_COMPRA)) {
            rechaza(() -> CatalogosValidador.validarTipoIdentificacion(completo, CONSUMIDOR_FINAL, tipo, "x"),
                CodigoError.CONSUMIDOR_FINAL_SOLO_FACTURA);
        }
        assertDoesNotThrow(() -> CatalogosValidador.validarTipoIdentificacion(completo, RUC,
            TipoComprobante.NOTA_CREDITO, "x"));
    }

    @Test
    void rechazaTipoDeIdentificacionFueraDeLaTabla6() {
        rechaza(() -> CatalogosValidador.validarTipoIdentificacion(completo, "09", TipoComprobante.FACTURA, "x"),
            CodigoError.TIPO_IDENTIFICACION_NO_EXISTE);
    }

    // ---- retenciones ----

    @Test
    void retencionIvaValidaCodigoYPorcentaje() {
        assertDoesNotThrow(() -> CatalogosValidador.validarRetencion(completo, RETENCION_IVA, "9", d("10")));
        rechaza(() -> CatalogosValidador.validarRetencion(completo, RETENCION_IVA, "9", d("20")),
            CodigoError.PORCENTAJE_RETENER_NO_COINCIDE);
        rechaza(() -> CatalogosValidador.validarRetencion(completo, RETENCION_IVA, "55", d("10")),
            CodigoError.CODIGO_RETENCION_NO_EXISTE);
    }

    @Test
    void retencionDeRentaSinCodigosCargadosSoloValidaElImpuesto() {
        assertDoesNotThrow(() -> CatalogosValidador.validarRetencion(completo, RENTA, "312", d("1.75")));
        rechaza(() -> CatalogosValidador.validarRetencion(completo, "4", "312", d("1")),
            CodigoError.IMPUESTO_RETENER_NO_EXISTE);
    }

    @Test
    void retencionIsdUsaElPorcentajeVigenteALaFechaDeEmision() {
        assertDoesNotThrow(() -> CatalogosValidador.validarRetencion(completo, ISD, "4580", d("5")));
        CatalogosSriAFecha en2022 = catalogos(LocalDate.of(2022, 3, 1), TARIFAS_IVA, RETENCIONES, List.of());
        // en 2022 el catálogo de este test no tiene fila vigente para 4580: existe pero no está vigente
        rechaza(() -> CatalogosValidador.validarRetencion(en2022, ISD, "4580", d("5")),
            CodigoError.CODIGO_RETENCION_NO_VIGENTE);
    }

    // ---- codSustento ----

    @Test
    void sustentoSoloSeValidaSiElCatalogoEstaCargado() {
        assertDoesNotThrow(() -> CatalogosValidador.validarSustento(completo, "99"));
        CatalogosSriAFecha conSustentos = catalogos(HOY, TARIFAS_IVA, RETENCIONES,
            List.of(new SustentoRow("01", "Crédito tributario", true)));
        assertDoesNotThrow(() -> CatalogosValidador.validarSustento(conSustentos, "01"));
        rechaza(() -> CatalogosValidador.validarSustento(conSustentos, "99"), CodigoError.SUSTENTO_NO_EXISTE);
    }
}
