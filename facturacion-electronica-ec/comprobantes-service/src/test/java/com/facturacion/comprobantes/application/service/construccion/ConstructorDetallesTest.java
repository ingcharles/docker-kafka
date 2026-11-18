package com.facturacion.comprobantes.application.service.construccion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.application.dto.CampoAdicionalRequest;
import com.facturacion.comprobantes.application.dto.DetalleComprobanteRequest;
import com.facturacion.comprobantes.application.dto.ImpuestoRequest;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.entity.ItemLocal;
import com.facturacion.comprobantes.domain.model.enums.TipoTarifa;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.shared.error.CodigoError;

/** Los detalles adicionales de la línea: los del llamador (hasta 3) y la "Medida" que sale de la unidad del ítem. */
class ConstructorDetallesTest {

    private final ConstructorDetalles constructor = new ConstructorDetalles(null);
    private final CatalogosSriAFecha catalogos = new CatalogosSriAFecha(LocalDate.of(2026, 11, 14), List.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    private static ItemLocal item(UUID id, String unidad) {
        ItemLocal item = new ItemLocal();
        item.setId(id);
        item.setUnidadMedida(unidad);
        return item;
    }

    private static CampoAdicionalRequest campo(String nombre, String valor) {
        CampoAdicionalRequest campo = new CampoAdicionalRequest();
        campo.setNombre(nombre);
        campo.setValor(valor);
        return campo;
    }

    private static DetalleComprobanteRequest linea(UUID itemId, List<CampoAdicionalRequest> adicionales) {
        ImpuestoRequest iva = new ImpuestoRequest();
        iva.setCodigo("2");
        iva.setCodigoPorcentaje("4");
        iva.setTarifa(new BigDecimal("15"));
        DetalleComprobanteRequest request = new DetalleComprobanteRequest();
        request.setItemId(itemId);
        request.setCodigoPrincipal("A");
        request.setDescripcion("Producto");
        request.setCantidad(BigDecimal.ONE);
        request.setPrecioUnitario(BigDecimal.TEN);
        request.setImpuestos(List.of(iva));
        request.setDetallesAdicionales(adicionales);
        return request;
    }

    @Test
    void laUnidadDelItemSaleComoMedidaPrimero() {
        UUID id = UUID.randomUUID();

        DetalleComprobante detalle = constructor.construir(
            linea(id, List.of(campo("Centro Logistico", "23"))), Map.of(id, item(id, "UN")), catalogos);

        assertEquals(List.of(new CampoAdicional("Medida", "UN"), new CampoAdicional("Centro Logistico", "23")),
            detalle.getDetallesAdicionales());
    }

    @Test
    void unaMedidaMandadaPorElLlamadorNoSeDuplica() {
        UUID id = UUID.randomUUID();

        DetalleComprobante detalle = constructor.construir(
            linea(id, List.of(campo("medida", "KG"))), Map.of(id, item(id, "UN")), catalogos);

        assertEquals(List.of(new CampoAdicional("medida", "KG")), detalle.getDetallesAdicionales());
    }

    @Test
    void conTresDetallesDelLlamadorNoSeAgregaLaMedidaPorqueElXsdAdmiteTres() {
        UUID id = UUID.randomUUID();

        DetalleComprobante detalle = constructor.construir(
            linea(id, List.of(campo("a", "1"), campo("b", "2"), campo("c", "3"))), Map.of(id, item(id, "UN")),
            catalogos);

        assertEquals(3, detalle.getDetallesAdicionales().size());
    }

    private static ConstructorDetalles.TarifaDeImpuesto tarifa(String codigo, String codigoPorcentaje, String tarifa) {
        return tarifa(codigo, codigoPorcentaje, tarifa, TipoTarifa.PORCENTAJE, null);
    }

    private static ConstructorDetalles.TarifaDeImpuesto tarifa(String codigo, String codigoPorcentaje, String tarifa,
            TipoTarifa tipo, String baseIndicada) {
        return new ConstructorDetalles.TarifaDeImpuesto(codigo, codigoPorcentaje, new BigDecimal(tarifa), tipo,
            baseIndicada == null ? null : new BigDecimal(baseIndicada));
    }

    @Test
    void elIvaDeUnaLineaConIceSeCalculaSobreBasePorMasIce() {
        // Caso F09 rechazado por el SRI: 2 x 10,00 con ICE 75% e IVA 15% -> ICE 15,00; base IVA 35,00; IVA 5,25.
        List<Impuesto> impuestos = ConstructorDetalles.calcularImpuestos(new BigDecimal("20.00"), new BigDecimal("2"),
            List.of(tarifa("3", "3031", "75"), tarifa("2", "4", "15")));

        Impuesto ice = impuestos.get(0);
        Impuesto iva = impuestos.get(1);
        assertEquals(new BigDecimal("20.00"), ice.baseImponible());
        assertEquals(new BigDecimal("15.00"), ice.valor());
        assertEquals(new BigDecimal("35.00"), iva.baseImponible());
        assertEquals(new BigDecimal("5.25"), iva.valor());
    }

    @Test
    void elIvaIgualSumaElIceAunqueElIvaVengaPrimeroEnLaLista() {
        List<Impuesto> impuestos = ConstructorDetalles.calcularImpuestos(new BigDecimal("50.00"), BigDecimal.ONE,
            List.of(tarifa("2", "4", "15"), tarifa("3", "3660", "35")));

        assertEquals(new BigDecimal("67.50"), impuestos.get(0).baseImponible());
        assertEquals(new BigDecimal("10.13"), impuestos.get(0).valor());
        assertEquals(new BigDecimal("17.50"), impuestos.get(1).valor());
    }

    @Test
    void elIrbpnrEsTarifaEspecificaPorUnidadYNoEntraEnLaBaseDelIva() {
        // Ejemplo de la ficha: IRBPNR 5001, tarifa 0.02, base = unidades, valor = unidades x tarifa.
        List<Impuesto> impuestos = ConstructorDetalles.calcularImpuestos(new BigDecimal("12.00"), new BigDecimal("12"),
            List.of(tarifa("5", "5001", "0.02", TipoTarifa.ESPECIFICA, null), tarifa("2", "4", "15")));

        assertEquals(new BigDecimal("12"), impuestos.get(0).baseImponible());
        assertEquals(new BigDecimal("0.24"), impuestos.get(0).valor());
        assertEquals(new BigDecimal("12.00"), impuestos.get(1).baseImponible());
        assertEquals(new BigDecimal("1.80"), impuestos.get(1).valor());
    }

    @Test
    void elIceEspecificoUsaLasUnidadesYEntraEnLaBaseDelIva() {
        // Cigarrillos 3011: USD 0,16 por unidad; 10 cajetillas a 5,00 -> ICE 1,60; IVA 15% sobre 50,00 + 1,60.
        List<Impuesto> impuestos = ConstructorDetalles.calcularImpuestos(new BigDecimal("50.00"), new BigDecimal("10"),
            List.of(tarifa("3", "3011", "0.16", TipoTarifa.ESPECIFICA, null), tarifa("2", "4", "15")));

        assertEquals(new BigDecimal("10"), impuestos.get(0).baseImponible());
        assertEquals(new BigDecimal("1.60"), impuestos.get(0).valor());
        assertEquals(new BigDecimal("51.60"), impuestos.get(1).baseImponible());
        assertEquals(new BigDecimal("7.74"), impuestos.get(1).valor());
    }

    @Test
    void laBaseIndicadaPorElEmisorReemplazaALaDeLaLinea() {
        // Base por PVP (art. 76 LRTI): el emisor indica la base imponible del ICE ya determinada.
        List<Impuesto> porcentual = ConstructorDetalles.calcularImpuestos(new BigDecimal("100.00"), BigDecimal.ONE,
            List.of(tarifa("3", "3610", "20", TipoTarifa.PORCENTAJE, "60.00"), tarifa("2", "4", "15")));
        assertEquals(new BigDecimal("60.00"), porcentual.get(0).baseImponible());
        assertEquals(new BigDecimal("12.00"), porcentual.get(0).valor());
        assertEquals(new BigDecimal("112.00"), porcentual.get(1).baseImponible());

        // Tarifa específica por 100 g de azúcar: 4,5 (cada 100 g) x 0,18.
        List<Impuesto> especifica = ConstructorDetalles.calcularImpuestos(new BigDecimal("2.00"), BigDecimal.ONE,
            List.of(tarifa("3", "3053", "0.18", TipoTarifa.ESPECIFICA, "4.5"), tarifa("2", "4", "15")));
        assertEquals(new BigDecimal("0.81"), especifica.get(0).valor());
    }

    @Test
    void sinIceElIvaSeCalculaSobreLaBaseDeLaLinea() {
        List<Impuesto> impuestos = ConstructorDetalles.calcularImpuestos(new BigDecimal("20.00"), new BigDecimal("2"),
            List.of(tarifa("2", "4", "15")));

        assertEquals(new BigDecimal("20.00"), impuestos.get(0).baseImponible());
        assertEquals(new BigDecimal("3.00"), impuestos.get(0).valor());
    }

    private static final CatalogosSriAFecha CATALOGO_CON_ICE = new CatalogosSriAFecha(LocalDate.of(2026, 11, 14),
        List.of(), List.of(),
        List.of(
            new CatalogosSriAFecha.TarifaRow("2", "4", "IVA 15%", new BigDecimal("15"), LocalDate.of(2024, 4, 1),
                null, true),
            new CatalogosSriAFecha.TarifaRow("3", "3031", "ICE bebidas alcoholicas", new BigDecimal("75"),
                TipoTarifa.PORCENTAJE, LocalDate.of(1900, 1, 1), null, true),
            new CatalogosSriAFecha.TarifaRow("3", "3031", "ICE bebidas alcoholicas", new BigDecimal("10.00"),
                TipoTarifa.ESPECIFICA, LocalDate.of(2023, 2, 1), null, true),
            new CatalogosSriAFecha.TarifaRow("5", "5001", "IRBPNR", new BigDecimal("0.02"), TipoTarifa.ESPECIFICA,
                LocalDate.of(1900, 1, 1), null, true)),
        List.of(), List.of(), List.of(), List.of());

    private static ItemLocal itemConImpuestos(UUID id, String ice, String irbpnr) {
        ItemLocal item = item(id, "UN");
        item.setActivo(true);
        item.setCodigoPorcentajeIva("4");
        item.setCodigoIce(ice);
        item.setCodigoIrbpnr(irbpnr);
        return item;
    }

    @Test
    void elItemSoloGuardaCodigosYLaTarifaSaleDelCatalogo() {
        UUID id = UUID.randomUUID();
        DetalleComprobanteRequest request = linea(id, null);
        request.setImpuestos(null);
        request.setCantidad(new BigDecimal("2"));
        request.setPrecioUnitario(new BigDecimal("10.00"));

        DetalleComprobante detalle = constructor.construir(request, Map.of(id, itemConImpuestos(id, "3031", "5001")),
            CATALOGO_CON_ICE);

        Impuesto iva = detalle.getImpuestos().get(0);
        Impuesto ice = detalle.getImpuestos().get(1);
        Impuesto irbpnr = detalle.getImpuestos().get(2);
        assertEquals(new BigDecimal("15.00"), ice.valor());            // porcentual preferido en 3031: 75 % de 20,00
        assertEquals(new BigDecimal("35.00"), iva.baseImponible());    // 20,00 + ICE
        assertEquals(new BigDecimal("5.25"), iva.valor());
        assertEquals(new BigDecimal("0.04"), irbpnr.valor());          // 2 botellas x 0,02
    }

    @Test
    void sinIceNiIrbpnrElItemSoloLlevaIva() {
        UUID id = UUID.randomUUID();
        DetalleComprobanteRequest request = linea(id, null);
        request.setImpuestos(null);

        DetalleComprobante detalle = constructor.construir(request, Map.of(id, itemConImpuestos(id, null, null)),
            CATALOGO_CON_ICE);

        assertEquals(1, detalle.getImpuestos().size());
    }

    @Test
    void unRequestPuedePedirLaModalidadEspecificaDeUnCodigoConDos() {
        ImpuestoRequest ice = new ImpuestoRequest();
        ice.setCodigo("3");
        ice.setCodigoPorcentaje("3031");
        ice.setTipoTarifa(TipoTarifa.ESPECIFICA);
        ice.setBaseImponible(new BigDecimal("3"));   // litros de alcohol puro
        DetalleComprobanteRequest request = linea(null, null);
        request.setImpuestos(List.of(ice));

        DetalleComprobante detalle = constructor.construir(request, Map.of(), CATALOGO_CON_ICE);

        assertEquals(new BigDecimal("30.00"), detalle.getImpuestos().get(0).valor());
        assertEquals(new BigDecimal("3"), detalle.getImpuestos().get(0).baseImponible());
    }

    @Test
    void laBaseImponibleNoSePuedeIndicarParaElIva() {
        ImpuestoRequest iva = new ImpuestoRequest();
        iva.setCodigo("2");
        iva.setCodigoPorcentaje("4");
        iva.setBaseImponible(new BigDecimal("5"));
        DetalleComprobanteRequest request = linea(null, null);
        request.setImpuestos(List.of(iva));

        ErrorNegocioException error = assertThrows(ErrorNegocioException.class,
            () -> constructor.construir(request, Map.of(), CATALOGO_CON_ICE));

        assertEquals(CodigoError.BASE_IMPONIBLE_NO_APLICA, error.getCodigo());
    }

    @Test
    void unCodigoIceQueNoEstaEnElCatalogoSeRechaza() {
        ImpuestoRequest ice = new ImpuestoRequest();
        ice.setCodigo("3");
        ice.setCodigoPorcentaje("9999");
        DetalleComprobanteRequest request = linea(null, null);
        request.setImpuestos(List.of(ice));

        ErrorNegocioException error = assertThrows(ErrorNegocioException.class,
            () -> constructor.construir(request, Map.of(), CATALOGO_CON_ICE));

        assertEquals(CodigoError.PORCENTAJE_IMPUESTO_NO_VIGENTE, error.getCodigo());
    }

    @Test
    void sinItemOSinUnidadSoloQuedanLosDelLlamador() {
        UUID id = UUID.randomUUID();

        assertTrue(constructor.construir(linea(null, null), Map.of(), catalogos).getDetallesAdicionales().isEmpty());
        assertTrue(constructor.construir(linea(id, null), Map.of(id, item(id, " ")), catalogos)
            .getDetallesAdicionales().isEmpty());
    }
}
