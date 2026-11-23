package com.facturacion.comprobantes.infrastructure.xml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.RegimenRimpe;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.model.vo.Pago;

/** El XML de la factura lleva lo que exigen los anexos 21, 22, 24 y 26 de la ficha del SRI y sigue siendo válido contra el XSD. */
class GeneradorXmlFacturaTest {

    private static final String CLAVE = "1411202601019007200200120239190001940680194996110";

    private final GeneradorXmlFactura generador = new GeneradorXmlFactura();
    private final XsdValidador validador = new XsdValidador();

    private static Comprobante factura() {
        DetalleComprobante detalle = new DetalleComprobante(UUID.randomUUID(), null, "5JNJ00K", null,
            "CARRITO BASICO", new BigDecimal("5"), new BigDecimal("1.99"), new BigDecimal("0.30"),
            List.of(new Impuesto("2", "4", new BigDecimal("15"), new BigDecimal("9.65"), new BigDecimal("1.45"))));
        Comprobante c = new Comprobante();
        c.setId(UUID.randomUUID());
        c.setTipoComprobante(TipoComprobante.FACTURA);
        c.setClaveAcceso(CLAVE);
        c.setAmbiente(AmbienteSri.PRUEBAS);
        c.setEstablecimiento("023");
        c.setPuntoEmision("919");
        c.setSecuencial("000194068");
        c.setEmpresaRuc("0190072002001");
        c.setEmpresaRazonSocial("EMPRESA DE PRUEBA CIA LTDA");
        c.setEmpresaDireccionMatriz("AV. PRINCIPAL S/N");
        c.setEmpresaObligadoContabilidad(true);
        c.setFechaEmision(Instant.parse("2026-11-14T15:00:00Z"));
        c.setCompradorTipoIdentificacion("05");
        c.setCompradorIdentificacion("1710034065");
        c.setCompradorRazonSocial("CONSUMIDOR DE PRUEBA");
        c.setSubtotalSinImpuestos(new BigDecimal("9.65"));
        c.setTotalDescuento(new BigDecimal("0.30"));
        c.setImporteTotal(new BigDecimal("11.10"));
        c.setMoneda("DOLAR");
        c.setEstado(EstadoComprobante.BORRADOR);
        c.setDetalles(List.of(detalle));
        c.setPagos(List.of(new Pago("01", new BigDecimal("11.10"), null, null)));
        return c;
    }

    @Test
    void testDeberiaElAgenteDeRetencionYElRegimenRimpeVanAlFinalDeInfoTributariaEnEseOrden() {
        Comprobante c = factura();
        c.setEmpresaAgenteRetencion("3257");
        c.setEmpresaRegimenRimpe(RegimenRimpe.RIMPE);

        String xml = generador.generar(c);

        assertTrue(xml.contains("<agenteRetencion>3257</agenteRetencion>"));
        assertTrue(xml.contains("<contribuyenteRimpe>CONTRIBUYENTE RÉGIMEN RIMPE</contribuyenteRimpe>"));
        assertTrue(xml.indexOf("<dirMatriz>") < xml.indexOf("<agenteRetencion>"));
        assertTrue(xml.indexOf("<agenteRetencion>") < xml.indexOf("<contribuyenteRimpe>"));
        assertTrue(xml.indexOf("<contribuyenteRimpe>") < xml.indexOf("</infoTributaria>"));
        assertDoesNotThrow(() -> validador.validar(TipoComprobante.FACTURA, xml));
    }

    @Test
    void testDeberiaNegocioPopularUsaSuPropiaLeyendaYTambienValidaContraElXsd() {
        Comprobante c = factura();
        c.setEmpresaRegimenRimpe(RegimenRimpe.NEGOCIO_POPULAR);

        String xml = generador.generar(c);

        assertTrue(xml.contains("<contribuyenteRimpe>CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE</contribuyenteRimpe>"));
        assertDoesNotThrow(() -> validador.validar(TipoComprobante.FACTURA, xml));
    }

    @Test
    void testDeberiaLaPlacaVaEntreMonedaYPagosYValidaContraElXsd() {
        Comprobante c = factura();
        c.setPlaca("PCM4567");

        String xml = generador.generar(c);

        assertTrue(xml.contains("<placa>PCM4567</placa>"));
        assertTrue(xml.indexOf("<moneda>") < xml.indexOf("<placa>"));
        assertTrue(xml.indexOf("<placa>") < xml.indexOf("<pagos>"));
        assertDoesNotThrow(() -> validador.validar(TipoComprobante.FACTURA, xml));
    }

    @Test
    void testDeberiaElPrecioSinSubsidioVaEntrePrecioUnitarioYDescuentoYValidaContraElXsd() {
        Comprobante c = factura();
        c.getDetalles().get(0).setPrecioSinSubsidio(new BigDecimal("2.50"));

        String xml = generador.generar(c);

        assertTrue(xml.contains("<precioSinSubsidio>2.50</precioSinSubsidio>"));
        assertTrue(xml.indexOf("<precioUnitario>") < xml.indexOf("<precioSinSubsidio>"));
        assertTrue(xml.indexOf("<precioSinSubsidio>") < xml.indexOf("<descuento>"));
        assertDoesNotThrow(() -> validador.validar(TipoComprobante.FACTURA, xml));
    }

    @Test
    void testDeberiaSinPrecioSinSubsidioNoSeEscribeLaEtiqueta() {
        assertFalse(generador.generar(factura()).contains("precioSinSubsidio"));
    }

    @Test
    void testDeberiaSinPlacaNoSeEscribeLaEtiqueta() {
        String xml = generador.generar(factura());

        assertFalse(xml.contains("<placa>"));
    }

    @Test
    void testDeberiaSinAgenteNiRimpeNoSeEscribeNinguna() {
        String xml = generador.generar(factura());

        assertFalse(xml.contains("agenteRetencion"));
        assertFalse(xml.contains("contribuyenteRimpe"));
        assertDoesNotThrow(() -> validador.validar(TipoComprobante.FACTURA, xml));
    }

    @Test
    void testDeberiaGranContribuyenteEsUnCampoAdicionalConLaLeyendaComoNombreYLaResolucionComoValor() {
        Comprobante c = factura();
        c.setEmpresaEsGranContribuyente(true);
        c.setEmpresaNumeroResolucionGranContribuyente("NAC-GCFOIOC21-00000868-E");
        c.setProveedorRuc("1719369561001");
        c.setCamposAdicionales(List.of(new CampoAdicional("Lugar Venta", "CORAL")));

        String xml = generador.generar(c);

        assertTrue(xml.contains("<campoAdicional nombre=\"Gran Contribuyente\">NAC-GCFOIOC21-00000868-E</campoAdicional>"));
        assertTrue(xml.contains("<campoAdicional nombre=\"RUC Proveedor\">1719369561001</campoAdicional>"));
        assertTrue(xml.indexOf("Gran Contribuyente") < xml.indexOf("RUC Proveedor"));
        assertTrue(xml.indexOf("RUC Proveedor") < xml.indexOf("Lugar Venta"));
        assertDoesNotThrow(() -> validador.validar(TipoComprobante.FACTURA, xml));
    }

    @Test
    void testDeberiaLosDetallesAdicionalesDeLaLineaVanEntreElPrecioTotalYLosImpuestos() {
        Comprobante c = factura();
        c.getDetalles().get(0).setDetallesAdicionales(
            List.of(new CampoAdicional("Medida", "UN"), new CampoAdicional("Centro Logistico", "23")));

        String xml = generador.generar(c);

        assertTrue(xml.contains("<detAdicional nombre=\"Medida\" valor=\"UN\"/>"));
        assertTrue(xml.contains("<detAdicional nombre=\"Centro Logistico\" valor=\"23\"/>"));
        assertTrue(xml.indexOf("<precioTotalSinImpuesto>") < xml.indexOf("<detallesAdicionales>"));
        assertTrue(xml.indexOf("</detallesAdicionales>") < xml.indexOf("<impuestos>"));
        assertDoesNotThrow(() -> validador.validar(TipoComprobante.FACTURA, xml));
    }
}
