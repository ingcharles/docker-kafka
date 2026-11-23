package com.facturacion.comprobantes.infrastructure.ride;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.RegimenRimpe;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.FormaPagoRow;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.model.vo.Pago;

/** El RIDE sigue el formato de factura del SRI: encabezado en recuadros, tabla con bordes, información y totales. */
class RideGeneradorTest {

    private static final String CLAVE = "1411202601019007200200120239190001940680194996110";

    private final RideGenerador generador = new RideGenerador();

    private static DetalleComprobante detalle(String codigo, String descripcion, String cantidad, String precio,
            String descuento, String base, String tarifa, String codigoPorcentaje, String iva) {
        return new DetalleComprobante(UUID.randomUUID(), null, codigo, null, descripcion, new BigDecimal(cantidad),
            new BigDecimal(precio), new BigDecimal(descuento),
            List.of(new Impuesto("2", codigoPorcentaje, new BigDecimal(tarifa), new BigDecimal(base),
                new BigDecimal(iva))));
    }

    private static Comprobante factura(List<DetalleComprobante> detalles) {
        Comprobante c = new Comprobante();
        c.setId(UUID.randomUUID());
        c.setTipoComprobante(TipoComprobante.FACTURA);
        c.setClaveAcceso(CLAVE);
        c.setAmbiente(AmbienteSri.PRODUCCION);
        c.setEstablecimiento("023");
        c.setPuntoEmision("919");
        c.setSecuencial("000194068");
        c.setEmpresaRuc("0190072002001");
        c.setEmpresaRazonSocial("EMPRESA DE PRUEBA E HIJOS CIA LTDA");
        c.setEmpresaDireccionMatriz("AV. PRINCIPAL S/N Y SECUNDARIA");
        c.setDireccionEstablecimiento("AV 10 DE AGOSTO S/N");
        c.setEmpresaObligadoContabilidad(true);
        c.setEmpresaContribuyenteEspecial(true);
        c.setEmpresaNumeroContribuyenteEspecial("3517");
        c.setFechaEmision(Instant.parse("2026-11-14T15:00:00Z"));
        c.setCompradorIdentificacion("1722039953");
        c.setCompradorRazonSocial("ANCHUNDIA VALENCIA CARLOS EDUARDO");
        c.setNumeroAutorizacion(CLAVE);
        c.setFechaAutorizacion(Instant.parse("2026-11-14T22:42:40Z"));
        c.setProveedorRuc("0190072002001");
        c.setSubtotalSinImpuestos(new BigDecimal("13.25"));
        c.setTotalDescuento(new BigDecimal("0.41"));
        c.setImporteTotal(new BigDecimal("15.24"));
        c.setMoneda("DOLAR");
        c.setEstado(EstadoComprobante.AUTORIZADO);
        c.setDetalles(detalles);
        c.setPagos(List.of(new Pago("19", new BigDecimal("15.24"), null, null)));
        c.setCamposAdicionales(List.of(new CampoAdicional("Lugar Venta", "CORAL 10 DE AGOSTO")));
        return c;
    }

    private static CatalogosSriAFecha catalogos() {
        LocalDate fecha = LocalDate.of(2026, 11, 14);
        return new CatalogosSriAFecha(fecha,
            List.of(new FormaPagoRow("19", "TARJETA DE CREDITO", LocalDate.of(2020, 1, 1), null, true)),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static String extraerTexto(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    private static int contarPaginas(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return documento.getNumberOfPages();
        }
    }

    @Test
    void testDeberiaLaFacturaTieneLosBloquesDelFormatoDelSri() throws IOException {
        Comprobante c = factura(List.of(
            detalle("5JNJ00K", "CARRITO BASICO HOT WHEELS C4982", "5", "1.99", "0.30", "9.65", "15", "4", "1.45"),
            detalle("X0E0447", "POWERADE FRUTOS TROPICALES 600ML", "1", "0.5704", "0.02", "0.55", "15", "4", "0.08")));

        byte[] pdf = generador.generar(c, null, null, catalogos());
        String texto = extraerTexto(pdf);

        assertEquals(1, contarPaginas(pdf));
        for (String esperado : new String[] {"R.U.C.:", "0190072002001", "FACTURA", "No. 023-919-000194068",
            "NÚMERO DE AUTORIZACIÓN", "FECHA Y HORA DE", "AMBIENTE:", "PRODUCCION", "EMISIÓN:", "NORMAL",
            "CLAVE DE ACCESO", CLAVE, "EMPRESA DE PRUEBA E HIJOS CIA LTDA", "Dirección Matriz:",
            "Dirección Sucursal:", "Contribuyente Especial Nro:", "Obligado A Llevar Contabilidad:",
            "Fecha Emisión:", "Cédula/RUC:", "ANCHUNDIA VALENCIA CARLOS EDUARDO", "Cod. Principal", "Precio Unit.",
            "Precio Total", "5JNJ00K", "5.0000", "1.9900", "Información Adicional", "Forma de Pago:",
            "TARJETA DE CREDITO 15.24", "RUC Proveedor:", "Lugar Venta:", "SUBTOTAL SIN IMPUESTOS",
            "VALOR TOTAL"}) {
            assertTrue(texto.contains(esperado), "Falta en el RIDE: " + esperado + "\n" + texto);
        }
    }

    @Test
    void testDeberiaElRideMuestraAgenteDeRetencionRimpeGranContribuyenteYLosDetallesAdicionalesDeLaLinea() throws IOException {
        DetalleComprobante linea = detalle("5JNJ00K", "CARRITO BASICO", "5", "1.99", "0.30", "9.65", "15", "4", "1.45");
        linea.setDetallesAdicionales(List.of(new CampoAdicional("Medida", "UN"),
            new CampoAdicional("Centro Logistico", "23")));
        Comprobante c = factura(List.of(linea));
        c.setEmpresaAgenteRetencion("3257");
        c.setEmpresaRegimenRimpe(RegimenRimpe.NEGOCIO_POPULAR);
        c.setEmpresaEsGranContribuyente(true);
        c.setEmpresaNumeroResolucionGranContribuyente("NAC-GCFOIOC21-00000868-E");

        String texto = extraerTexto(generador.generar(c, null, null, catalogos()));

        for (String esperado : new String[] {"Agente de Retención Resolución No. 3257",
            "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", "Gran Contribuyente:", "NAC-GCFOIOC21-00000868-E",
            "Medida: UN, Centro Logistico:", "Contribuyente Especial Nro:", "RUC Proveedor:"}) {
            assertTrue(texto.contains(esperado), "Falta en el RIDE: " + esperado + "\n" + texto);
        }
    }

    @Test
    void testDeberiaSinAgenteNiRimpeElRideNoLosMuestra() throws IOException {
        String texto = extraerTexto(generador.generar(factura(List.of(
            detalle("A", "Linea", "1", "1.00", "0", "1.00", "15", "4", "0.15"))), null, null, catalogos()));

        assertFalse(texto.contains("Agente de Retención"));
        assertFalse(texto.contains("RIMPE"));
        assertFalse(texto.contains("Gran Contribuyente"));
    }

    @Test
    void testDeberiaLosTotalesSeSeparanPorTarifaDeIva() throws IOException {
        Comprobante c = factura(List.of(
            detalle("A", "Con IVA 15", "1", "10.00", "0", "10.00", "15", "4", "1.50"),
            detalle("B", "Con IVA 5", "1", "2.00", "0", "2.00", "5", "5", "0.10"),
            detalle("C", "Tarifa cero", "1", "1.00", "0", "1.00", "0", "0", "0.00")));

        String texto = extraerTexto(generador.generar(c, null, null, catalogos()));

        for (String esperado : new String[] {"SUBTOTAL 15%", "SUBTOTAL 5%", "SUBTOTAL 0%", "IVA 15%", "IVA 5%", "ICE",
            "PROPINA"}) {
            assertTrue(texto.contains(esperado), "Falta el total: " + esperado);
        }
    }

    @Test
    void testDeberiaUnDetalleLargoContinuaEnOtraPaginaRepitiendoElEncabezadoDeLaTabla() throws IOException {
        List<DetalleComprobante> detalles = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            detalles.add(detalle("COD" + i, "Producto numero " + i + " con una descripcion bastante larga para que "
                + "ocupe mas de una linea dentro de la celda", "1", "1.00", "0", "1.00", "15", "4", "0.15"));
        }

        byte[] pdf = generador.generar(factura(detalles), null, null, catalogos());
        String texto = extraerTexto(pdf);

        assertTrue(contarPaginas(pdf) > 1, "80 líneas no caben en una página");
        assertTrue(texto.indexOf("Cod. Principal") != texto.lastIndexOf("Cod. Principal"),
            "El encabezado de la tabla debe repetirse en cada página");
        assertTrue(texto.contains("COD79") && texto.contains("VALOR TOTAL"));
    }

    @Test
    void testDeberiaElColorDelNumeroEsOpcionalYUnValorInvalidoNoRompeElPdf() {
        Comprobante c = factura(List.of(detalle("A", "Producto", "1", "1.00", "0", "1.00", "15", "4", "0.15")));
        assertTrue(generador.generar(c, "#1A73E8", null, catalogos()).length > 0);
        assertTrue(generador.generar(c, "no-es-un-color", null, catalogos()).length > 0);
    }

    private static byte[] logoPng() throws IOException {
        java.awt.image.BufferedImage imagen = new java.awt.image.BufferedImage(120, 60, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = imagen.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, 120, 60);
        g.dispose();
        java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private static int imagenesEnLaPrimeraPagina(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            int cantidad = 0;
            for (var nombre : documento.getPage(0).getResources().getXObjectNames()) {
                cantidad++;
            }
            return cantidad;
        }
    }

    @Test
    void testDeberiaConLogoLaImagenSeDibujaEnElRecuadroDelEmisorYElNombreComercialYaNoOcupaSuLugar() throws IOException {
        Comprobante c = factura(List.of(detalle("A", "Producto", "1", "1.00", "0", "1.00", "15", "4", "0.15")));
        c.setEmpresaNombreComercial("MARCA COMERCIAL");

        byte[] conLogo = generador.generar(c, null, logoPng(), catalogos());
        byte[] sinLogo = generador.generar(c, null, null, catalogos());

        assertEquals(1, imagenesEnLaPrimeraPagina(conLogo));
        assertEquals(0, imagenesEnLaPrimeraPagina(sinLogo));
        assertFalse(extraerTexto(conLogo).contains("MARCA COMERCIAL"));
        assertTrue(extraerTexto(sinLogo).contains("MARCA COMERCIAL"));
    }

    @Test
    void testDeberiaUnLogoQueNoEsUnaImagenNoRompeElRideYSaleSinLogo() throws IOException {
        Comprobante c = factura(List.of(detalle("A", "Producto", "1", "1.00", "0", "1.00", "15", "4", "0.15")));

        byte[] pdf = generador.generar(c, null, "esto no es una imagen".getBytes(), catalogos());

        assertEquals(0, imagenesEnLaPrimeraPagina(pdf));
        assertTrue(extraerTexto(pdf).contains("VALOR TOTAL"));
    }

    @Test
    void testDeberiaUnComprobanteSinDetalleNoDibujaTablaNiTotales() throws IOException {
        String texto = extraerTexto(generador.generar(factura(List.of()), null, null, catalogos()));
        assertTrue(texto.contains("FACTURA"));
        assertFalse(texto.contains("VALOR TOTAL"));
    }
    /** Y (desde abajo de la página) de la línea de texto que empieza con {@code inicio}, en la primera página. */
    private static float baselineDe(byte[] pdf, String inicio) throws IOException {
        float[] encontrada = {Float.NaN};
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String linea, List<TextPosition> posiciones) {
                    if (Float.isNaN(encontrada[0]) && linea.startsWith(inicio)) {
                        encontrada[0] = documento.getPage(0).getMediaBox().getHeight() - posiciones.get(0).getYDirAdj();
                    }
                }
            };
            stripper.setEndPage(1);
            stripper.getText(documento);
        }
        return encontrada[0];
    }

    private static Comprobante notaSobreFactura(TipoComprobante tipo, String motivo) {
        Comprobante c = factura(List.of(
            detalle("5JNJ00K", "CARRITO BASICO", "5", "1.99", "0.30", "9.65", "15", "4", "1.45")));
        c.setTipoComprobante(tipo);
        c.setCompradorRazonSocial("CONSUMIDOR DE PRUEBA");
        c.setDocModificadoTipo(TipoComprobante.FACTURA.codigo());
        c.setDocModificadoNumero("001-001-000000015");
        c.setDocModificadoFecha(Instant.parse("2026-11-13T15:00:00Z"));
        c.setMotivo(motivo);
        return c;
    }

    /** La última fila del recuadro del comprador queda dentro del borde, con holgura, aunque haya filas de más. */
    private void verificarUltimaFilaDentroDelBorde(Comprobante c, int filas, String etiquetaUltimaFila)
            throws IOException {
        byte[] pdf = generador.generar(c, null, null, catalogos());

        assertTrue(extraerTexto(pdf).contains(etiquetaUltimaFila), "Falta la fila " + etiquetaUltimaFila);
        float baselineUltimaFila = baselineDe(pdf, etiquetaUltimaFila);
        List<List<String>> lineasPorFila = new ArrayList<>();
        for (int i = 0; i < filas; i++) {
            lineasPorFila.add(List.of("")); // una línea por fila: los valores de la prueba son cortos
        }
        float bordeInferior = RideGenerador.ALTO_PAGINA - RideGenerador.MARGEN - RideGenerador.ALTO_CAJA_EMISOR
            - RideGenerador.SEPARACION_CAJAS - RideGenerador.calcularAltoCajaComprador(lineasPorFila);
        assertTrue(baselineUltimaFila - bordeInferior >= 8f,
            "La última fila toca o cruza el borde: baseline " + baselineUltimaFila + ", borde " + bordeInferior);
    }

    @Test
    void testDeberiaEnUnaNotaDeCreditoLaUltimaFilaDelRecuadroDelCompradorQuedaDentroDelBorde() throws IOException {
        // fecha, cédula/RUC, razón social, comprobante que se modifica, fecha a modificar y razón de la modificación
        verificarUltimaFilaDentroDelBorde(notaSobreFactura(TipoComprobante.NOTA_CREDITO, "AJUSTE DE PRECIO"), 6,
            "Razón de Modificación:");
    }

    @Test
    void testDeberiaEnUnaNotaDeDebitoLaUltimaFilaDelRecuadroDelCompradorQuedaDentroDelBorde() throws IOException {
        // sin razón: fecha, cédula/RUC, razón social, comprobante que se modifica y fecha a modificar
        verificarUltimaFilaDentroDelBorde(notaSobreFactura(TipoComprobante.NOTA_DEBITO, null), 5,
            "Fecha Emisión (a modificar):");
    }
}
