package com.facturacion.comprobantes.infrastructure.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.FormatoTicket;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.AnchoTicket;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.FormaPagoRow;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.model.vo.Pago;

/** El ticket: contenido mínimo, ajuste al ancho del papel y los dos formatos de salida. */
class TicketGeneradorTest {

    private static final AnchoTicket ANCHO_58 = new AnchoTicket(58, 32);
    private static final AnchoTicket ANCHO_80 = new AnchoTicket(80, 48);
    private static final String CLAVE = "1811202601172203995300110010010000000491234567811";

    private final TicketGenerador generador = new TicketGenerador();
    private final CatalogosSriAFecha catalogos = new CatalogosSriAFecha(LocalDate.of(2026, 11, 18),
        List.of(new FormaPagoRow("01", "SIN UTILIZACION DEL SISTEMA FINANCIERO", LocalDate.of(1900, 1, 1), null, true)),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    private static DetalleComprobante detalle(String descripcion, String cantidad, String precio, Impuesto... impuestos) {
        DetalleComprobante d = new DetalleComprobante();
        d.setDescripcion(descripcion);
        d.setCantidad(new BigDecimal(cantidad));
        d.setPrecioUnitario(new BigDecimal(precio));
        d.setDescuento(BigDecimal.ZERO);
        d.setImpuestos(List.of(impuestos));
        return d;
    }

    /** Factura de la prueba F09: 2 x 10,00 con ICE 75 % e IVA 15 % sobre base + ICE = 40,25. */
    private Comprobante factura(EstadoComprobante estado, AmbienteSri ambiente) {
        Comprobante c = new Comprobante();
        c.setTipoComprobante(TipoComprobante.FACTURA);
        c.setClaveAcceso(CLAVE);
        c.setAmbiente(ambiente);
        c.setEstablecimiento("001");
        c.setPuntoEmision("001");
        c.setSecuencial("000000049");
        c.setEmpresaRuc("1722039953001");
        c.setEmpresaRazonSocial("EMPRESA DE PRUEBA SOCIEDAD ANONIMA");
        c.setEmpresaDireccionMatriz("Llano Chico, Quito");
        c.setFechaEmision(Instant.parse("2026-11-18T14:30:00Z"));
        c.setCompradorTipoIdentificacion("05");
        c.setCompradorIdentificacion("1710034065");
        c.setCompradorRazonSocial("Consumidor de Prueba");
        c.setSubtotalSinImpuestos(new BigDecimal("20.00"));
        c.setTotalDescuento(BigDecimal.ZERO);
        c.setImporteTotal(new BigDecimal("40.25"));
        c.setEstado(estado);
        c.setDetalles(List.of(detalle("Bebida alcoholica con ICE (botella 750 ml)", "2", "10.00",
            new Impuesto("3", "3031", new BigDecimal("75"), new BigDecimal("20.00"), new BigDecimal("15.00")),
            new Impuesto("2", "4", new BigDecimal("15"), new BigDecimal("35.00"), new BigDecimal("5.25")))));
        c.setPagos(List.of(new Pago("01", new BigDecimal("40.25"), null, null)));
        return c;
    }

    private String texto(Comprobante c, AnchoTicket ancho) {
        return new String(generador.generar(c, FormatoTicket.TEXTO, ancho, catalogos), StandardCharsets.UTF_8);
    }

    @Test
    void testDeberiaIncluirLosDatosMinimosParaElCliente() {
        Comprobante c = factura(EstadoComprobante.AUTORIZADO, AmbienteSri.PRODUCCION);
        c.setNumeroAutorizacion(CLAVE);
        c.setFechaAutorizacion(Instant.parse("2026-11-18T14:31:00Z"));

        String ticket = texto(c, ANCHO_80);

        assertTrue(ticket.contains("EMPRESA DE PRUEBA SOCIEDAD ANONIMA"));
        assertTrue(ticket.contains("RUC 1722039953001"));
        assertTrue(ticket.contains("FACTURA"));
        assertTrue(ticket.contains("No. 001-001-000000049"));
        assertTrue(ticket.contains("Fecha: 18/11/2026 09:30"));
        assertTrue(ticket.contains("Cliente: Consumidor de Prueba"));
        assertTrue(ticket.contains("Bebida alcoholica con ICE"));
        assertTrue(ticket.contains("2 x 10.00"));
        assertTrue(ticket.contains("AUTORIZADO POR EL SRI"));
        assertTrue(ticket.contains("SIN UTILIZACION DEL SISTEMA FINANCIERO"));
        assertFalse(ticket.contains("PRUEBAS"));
    }

    @Test
    void testDeberiaImprimirLosTotalesDelResumenConElIceFueraDelSubtotalIva() {
        String ticket = texto(factura(EstadoComprobante.AUTORIZADO, AmbienteSri.PRODUCCION), ANCHO_80);

        assertTrue(ticket.contains("SUBTOTAL 15%"));
        assertTrue(ticket.lines().anyMatch(l -> l.startsWith("SUBTOTAL 15%") && l.endsWith("20.00")));
        assertTrue(ticket.lines().anyMatch(l -> l.startsWith("ICE") && l.endsWith("15.00")));
        assertTrue(ticket.lines().anyMatch(l -> l.startsWith("IVA 15%") && l.endsWith("5.25")));
        assertTrue(ticket.lines().anyMatch(l -> l.startsWith("VALOR TOTAL") && l.endsWith("40.25")));
        assertFalse(ticket.lines().anyMatch(l -> l.startsWith("DESCUENTO")), "las filas en cero no se imprimen");
    }

    @Test
    void testDeberiaNoPasarDelAnchoDelPapelEnNingunaLinea() {
        for (AnchoTicket ancho : List.of(ANCHO_58, ANCHO_80)) {
            String ticket = texto(factura(EstadoComprobante.AUTORIZADO, AmbienteSri.PRODUCCION), ancho);
            ticket.lines().forEach(l -> assertTrue(l.length() <= ancho.columnas(),
                ancho + ": \"" + l + "\" mide " + l.length()));
        }
    }

    @Test
    void testDeberiaAvisarQueNoTieneValidezTributariaEnPruebas() {
        String ticket = texto(factura(EstadoComprobante.AUTORIZADO, AmbienteSri.PRUEBAS), ANCHO_58);

        assertTrue(ticket.contains("AMBIENTE DE PRUEBAS"));
        assertTrue(ticket.contains("SIN VALIDEZ TRIBUTARIA"));
    }

    @Test
    void testDeberiaMostrarLaClaveDeAccesoYElEstadoSiNoEstaAutorizado() {
        String enProceso = texto(factura(EstadoComprobante.FIRMADO, AmbienteSri.PRODUCCION), ANCHO_80);
        String rechazado = texto(factura(EstadoComprobante.NO_AUTORIZADO, AmbienteSri.PRODUCCION), ANCHO_80);

        assertTrue(enProceso.contains("EN PROCESO DE AUTORIZACION"));
        assertTrue(enProceso.replace("\n", "").contains(CLAVE));
        assertTrue(rechazado.contains("NO AUTORIZADO"));
        assertTrue(rechazado.contains("NO VALE COMO COMPROBANTE"));
    }

    @Test
    void testDeberiaOmitirLaIdentificacionDelConsumidorFinal() {
        Comprobante c = factura(EstadoComprobante.AUTORIZADO, AmbienteSri.PRODUCCION);
        c.setCompradorTipoIdentificacion("07");
        c.setCompradorIdentificacion("9999999999999");
        c.setCompradorRazonSocial("CONSUMIDOR FINAL");

        String ticket = texto(c, ANCHO_80);

        assertTrue(ticket.contains("Cliente: CONSUMIDOR FINAL"));
        assertFalse(ticket.contains("9999999999999"));
    }

    @Test
    void testDeberiaGenerarEscposQueInicializaYCortaElPapelUsandoSoloAscii() {
        Comprobante c = factura(EstadoComprobante.AUTORIZADO, AmbienteSri.PRODUCCION);
        c.setEmpresaRazonSocial("CAFETERÍA EL ÁNGEL Ñ");

        byte[] bytes = generador.generar(c, FormatoTicket.ESCPOS, ANCHO_80, catalogos);

        assertEquals(0x1B, bytes[0]);
        assertEquals(0x40, bytes[1]);
        assertEquals(0x1D, bytes[bytes.length - 4]);
        assertEquals(0x56, bytes[bytes.length - 3]);
        String sinComandos = new String(bytes, StandardCharsets.ISO_8859_1);
        assertTrue(sinComandos.contains("CAFETERIA EL ANGEL N"));
        for (byte b : bytes) {
            assertTrue(b >= 0 && b < 127 || b < 32 && b >= 0 || b == 0, "byte no ASCII: " + b);
        }
    }

    @Test
    void testDeberiaPartirLaRazonSocialLargaEnVariasLineas() {
        Comprobante c = factura(EstadoComprobante.AUTORIZADO, AmbienteSri.PRODUCCION);
        c.setEmpresaRazonSocial("SOCIEDAD ANONIMA DE COMERCIALIZACION Y DISTRIBUCION DE PRODUCTOS");

        String ticket = texto(c, ANCHO_58);

        assertTrue(ticket.lines().filter(l -> !l.isBlank()).limit(3).allMatch(l -> l.length() <= 32));
        assertTrue(ticket.contains("COMERCIALIZACION"));
    }

    @Test
    void testDeberiaCortarLasPalabrasMasLargasQueElAnchoAlPartir() {
        assertEquals(List.of("ABCDEFGHIJ", "KLMNOP"), TicketGenerador.partir("ABCDEFGHIJKLMNOP", 10));
        assertEquals(List.of("uno dos", "tres"), TicketGenerador.partir("uno dos tres", 8));
    }
}
