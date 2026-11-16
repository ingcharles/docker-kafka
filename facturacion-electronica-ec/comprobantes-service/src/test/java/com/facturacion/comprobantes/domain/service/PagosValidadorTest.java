package com.facturacion.comprobantes.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.domain.model.enums.FormaPagoSri;
import com.facturacion.comprobantes.domain.model.vo.Pago;
import com.facturacion.comprobantes.shared.error.CodigoError;

class PagosValidadorTest {

    private static final BigDecimal TOTAL = new BigDecimal("115.00");
    /** Tabla 24 vigente (lo que entrega la réplica del catálogo). */
    private static final Set<String> FORMAS = Set.of("01", "15", "16", "17", "18", "19", "20", "21");
    private static final String SIN_SISTEMA_FINANCIERO = FormaPagoSri.SIN_UTILIZACION_SISTEMA_FINANCIERO.codigo();
    private static final String ENDOSO = FormaPagoSri.ENDOSO_TITULOS.codigo();
    private static final String TARJETA_CREDITO = "19";

    private static Pago pago(String forma, String total) {
        return new Pago(forma, new BigDecimal(total), null, null);
    }

    private static Pago credito(String forma, String total, String plazo, String unidad) {
        return new Pago(forma, new BigDecimal(total), plazo, unidad);
    }

    private static void validar(List<Pago> pagos, BigDecimal total, boolean admitePlazo) {
        PagosValidador.validar(pagos, total, admitePlazo, FORMAS);
    }

    /** Debe lanzar un ErrorNegocioException con ese código. */
    private static void rechaza(Executable accion, CodigoError esperado) {
        ErrorNegocioException error = assertThrows(ErrorNegocioException.class, accion);
        assertEquals(esperado, error.getCodigo());
    }

    @Test
    void aceptaUnPagoQueCuadraConElTotal() {
        assertDoesNotThrow(() -> validar(List.of(pago(SIN_SISTEMA_FINANCIERO, "115.00")), TOTAL, true));
    }

    @Test
    void aceptaVariosPagosQueSumanElTotal() {
        assertDoesNotThrow(() -> validar(
            List.of(pago(SIN_SISTEMA_FINANCIERO, "15.00"), pago(TARJETA_CREDITO, "100.00")), TOTAL, true));
    }

    @Test
    void aceptaTodosLosCodigosDelCatalogo() {
        for (String codigo : FORMAS) {
            assertDoesNotThrow(() -> validar(List.of(pago(codigo, "115.00")), TOTAL, true), "código " + codigo);
        }
    }

    @Test
    void rechazaCodigosFueraDelCatalogoAunqueElXsdLosAcepte() {
        // El XSD del SRI acepta 02..14; la Tabla 24 no los tiene.
        for (String codigo : List.of("02", "09", "14", "22", "00", "99", "1")) {
            rechaza(() -> validar(List.of(pago(codigo, "115.00")), TOTAL, true), CodigoError.PAGO_FORMA_NO_VIGENTE);
        }
    }

    @Test
    void rechazaUnaFormaDePagoQueElCatalogoYaNoTieneVigente() {
        Set<String> sinTarjetaDeCredito = Set.of("01", "15", "16", "17", "18", "20", "21");
        rechaza(() -> PagosValidador.validar(List.of(pago(TARJETA_CREDITO, "115.00")), TOTAL, true,
            sinTarjetaDeCredito), CodigoError.PAGO_FORMA_NO_VIGENTE);
    }

    @Test
    void rechazaCuandoLaSumaNoCuadra() {
        rechaza(() -> validar(List.of(pago(SIN_SISTEMA_FINANCIERO, "100.00")), TOTAL, true),
            CodigoError.PAGOS_NO_CUADRAN);
    }

    @Test
    void comparaLaSumaADosDecimalesComoSeImprimeEnElXml() {
        // 3 x 38.333 se imprime 3 x 38.33 = 114.99, pero el importeTotal impreso es 115.00: no cuadra.
        rechaza(() -> validar(List.of(pago(SIN_SISTEMA_FINANCIERO, "38.333"), pago(SIN_SISTEMA_FINANCIERO, "38.333"),
            pago(SIN_SISTEMA_FINANCIERO, "38.333")), TOTAL, true), CodigoError.PAGOS_NO_CUADRAN);
        // un importeTotal sin redondear de 115.004 se imprime 115.00
        assertDoesNotThrow(() -> validar(List.of(pago(SIN_SISTEMA_FINANCIERO, "115.00")),
            new BigDecimal("115.004"), true));
    }

    @Test
    void plazoYUnidadTiempoVanJuntos() {
        assertDoesNotThrow(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", "30", "dias")), TOTAL, true));
        rechaza(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", "30", null)), TOTAL, true),
            CodigoError.PAGO_PLAZO_SIN_UNIDAD);
        rechaza(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", null, "dias")), TOTAL, true),
            CodigoError.PAGO_UNIDAD_SIN_PLAZO);
        // una cadena en blanco cuenta como ausente
        assertDoesNotThrow(() -> validar(List.of(credito(SIN_SISTEMA_FINANCIERO, "115.00", " ", "")), TOTAL, true));
    }

    @Test
    void plazoDebeSerNumericoNoNegativoConHastaDosDecimales() {
        rechaza(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", "abc", "dias")), TOTAL, true),
            CodigoError.PAGO_PLAZO_NO_NUMERICO);
        rechaza(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", "30 dias", "dias")), TOTAL, true),
            CodigoError.PAGO_PLAZO_NO_NUMERICO);
        for (String fuera : List.of("-1", "1.234")) {
            rechaza(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", fuera, "dias")), TOTAL, true),
                CodigoError.PAGO_PLAZO_FUERA_DE_RANGO);
        }
        assertDoesNotThrow(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", "0", "dias")), TOTAL, true));
        assertDoesNotThrow(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", "30.50", "dias")), TOTAL, true));
    }

    @Test
    void unidadTiempoMaximoDiezCaracteres() {
        rechaza(() -> validar(List.of(credito(TARJETA_CREDITO, "115.00", "30", "undiaymuchomas")), TOTAL, true),
            CodigoError.PAGO_UNIDAD_TIEMPO_LARGA);
    }

    @Test
    void retencionNoAdmitePlazoNiUnidadTiempo() {
        assertDoesNotThrow(() -> validar(List.of(pago(SIN_SISTEMA_FINANCIERO, "115.00")), TOTAL, false));
        rechaza(() -> validar(List.of(credito(SIN_SISTEMA_FINANCIERO, "115.00", "30", "dias")), TOTAL, false),
            CodigoError.PAGO_RETENCION_SIN_PLAZO);
    }

    @Test
    void facturaNegociableExigePlazoUnidadYDireccionDelComprador() {
        List<Pago> completo = List.of(credito(ENDOSO, "115.00", "30", "dias"));
        assertDoesNotThrow(() -> PagosValidador.validarFacturaNegociable(completo, "Salinas y Santiago"));

        rechaza(() -> PagosValidador.validarFacturaNegociable(completo, null), CodigoError.NEGOCIABLE_SIN_DIRECCION);
        rechaza(() -> PagosValidador.validarFacturaNegociable(completo, "  "), CodigoError.NEGOCIABLE_SIN_DIRECCION);
        rechaza(() -> PagosValidador.validarFacturaNegociable(List.of(pago(ENDOSO, "115.00")), "Salinas y Santiago"),
            CodigoError.NEGOCIABLE_SIN_PLAZO);
    }

    @Test
    void facturaSinEndosoNoNecesitaDireccion() {
        assertDoesNotThrow(() -> PagosValidador.validarFacturaNegociable(
            List.of(pago(SIN_SISTEMA_FINANCIERO, "115.00")), null));
    }
}
