package com.facturacion.comprobantes.infrastructure.ride;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import jakarta.enterprise.context.ApplicationScoped;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;

/**
 * RIDE (Representación Impresa de Documento Electrónico) en PDF — Anexo 2 de
 * la ficha técnica del SRI. Se genera una sola vez, en el momento en que el
 * comprobante pasa a AUTORIZADO (ver {@code ComprobanteService}), a partir
 * del XML ya autorizado — nunca antes, porque el RIDE debe mostrar el
 * número y la fecha de autorización reales.
 * <p>
 * Layout deliberadamente simple (sin logo, sin código de barras de la clave
 * de acceso) — cubre los campos obligatorios del Anexo 2 (identificación
 * del emisor y el comprador, detalle, totales, clave de acceso como texto),
 * dejando la personalización visual (logo, colores) como un punto de
 * extensión vía {@code configuracion_emisor} en firma-electronica-service.
 */
@ApplicationScoped
public class RideGenerador {

    private static final float MARGEN = 40f;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] generar(Comprobante c) {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            PDFont fuenteNegrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fuenteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(documento, pagina)) {
                float y = pagina.getMediaBox().getHeight() - MARGEN;

                y = escribir(cs, fuenteNegrita, 14, MARGEN, y, c.getEmisorRazonSocial());
                if (c.getEmisorNombreComercial() != null && !c.getEmisorNombreComercial().isBlank()) {
                    y = escribir(cs, fuenteNormal, 10, MARGEN, y - 4, c.getEmisorNombreComercial());
                }
                y = escribir(cs, fuenteNormal, 10, MARGEN, y - 4, "RUC: " + c.getEmisorRuc());
                y = escribir(cs, fuenteNormal, 10, MARGEN, y - 2, "Dirección matriz: " + c.getEmisorDireccionMatriz());
                y = escribir(cs, fuenteNormal, 10, MARGEN, y - 2,
                    "Obligado a llevar contabilidad: " + (c.isEmisorObligadoContabilidad() ? "SI" : "NO"));
                if (c.isEmisorContribuyenteEspecial() && c.getEmisorNumeroContribuyenteEspecial() != null) {
                    y = escribir(cs, fuenteNormal, 10, MARGEN, y - 2,
                        "Contribuyente especial No. " + c.getEmisorNumeroContribuyenteEspecial());
                }
                if (c.isEmisorEsGranContribuyente()) {
                    y = escribir(cs, fuenteNormal, 10, MARGEN, y - 2,
                        "Gran Contribuyente Res. " + c.getEmisorNumeroResolucionGranContribuyente());
                }

                y -= 16;
                y = escribir(cs, fuenteNegrita, 12, MARGEN, y, etiquetaTipo(c));
                y = escribir(cs, fuenteNormal, 10, MARGEN, y - 4,
                    "No. " + c.getEstablecimiento() + "-" + c.getPuntoEmision() + "-" + c.getSecuencial());
                y = escribir(cs, fuenteNormal, 9, MARGEN, y - 2, "Clave de acceso: " + c.getClaveAcceso());
                y = escribir(cs, fuenteNormal, 9, MARGEN, y - 2, "Ambiente: "
                    + (c.getAmbiente().name().equals("PRUEBAS") ? "PRUEBAS" : "PRODUCCIÓN"));
                if (c.getNumeroAutorizacion() != null) {
                    y = escribir(cs, fuenteNormal, 9, MARGEN, y - 2, "Autorización: " + c.getNumeroAutorizacion());
                }
                if (c.getFechaAutorizacion() != null) {
                    y = escribir(cs, fuenteNormal, 9, MARGEN, y - 2, "Fecha autorización: "
                        + FORMATO_FECHA_HORA.format(c.getFechaAutorizacion().atZone(ZoneOffset.of("-05:00"))));
                }

                y -= 16;
                if (c.getCompradorRazonSocial() != null) {
                    y = escribir(cs, fuenteNegrita, 11, MARGEN, y, "Datos del comprador");
                    y = escribir(cs, fuenteNormal, 10, MARGEN, y - 4, "Razón social: " + c.getCompradorRazonSocial());
                    y = escribir(cs, fuenteNormal, 10, MARGEN, y - 2,
                        "Identificación: " + c.getCompradorIdentificacion());
                    if (c.getCompradorDireccion() != null && !c.getCompradorDireccion().isBlank()) {
                        y = escribir(cs, fuenteNormal, 10, MARGEN, y - 2, "Dirección: " + c.getCompradorDireccion());
                    }
                    y -= 2;
                }
                y = escribir(cs, fuenteNormal, 10, MARGEN, y,
                    "Fecha de emisión: " + FORMATO_FECHA.format(c.getFechaEmision().atZone(ZoneOffset.of("-05:00"))));

                y -= 16;
                y = escribirEncabezadoTabla(cs, fuenteNegrita, y);
                for (DetalleComprobante d : c.getDetalles()) {
                    y = escribirFilaDetalle(cs, fuenteNormal, y, d);
                }

                y -= 16;
                y = escribir(cs, fuenteNormal, 10, 350, y, "Subtotal sin impuestos: " + moneda(c.getSubtotalSinImpuestos()));
                y = escribir(cs, fuenteNormal, 10, 350, y - 2, "Descuento: " + moneda(c.getTotalDescuento()));
                y = escribir(cs, fuenteNegrita, 11, 350, y - 4, "TOTAL: " + c.getMoneda() + " " + moneda(c.getImporteTotal()));

                if (c.getEstado() != null && c.getEstado().name().equals("NO_AUTORIZADO")
                        && c.getMotivoRechazo() != null) {
                    y -= 20;
                    escribir(cs, fuenteNegrita, 10, MARGEN, y, "NO AUTORIZADO: " + c.getMotivoRechazo());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            documento.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el RIDE del comprobante " + c.getId(), e);
        }
    }

    private String etiquetaTipo(Comprobante c) {
        return switch (c.getTipoComprobante()) {
            case FACTURA -> "FACTURA";
            case NOTA_CREDITO -> "NOTA DE CREDITO";
            case NOTA_DEBITO -> "NOTA DE DEBITO";
            case GUIA_REMISION -> "GUIA DE REMISION";
            case RETENCION -> "COMPROBANTE DE RETENCION";
            case LIQUIDACION_COMPRA -> "LIQUIDACION DE COMPRA";
        };
    }

    private float escribirEncabezadoTabla(PDPageContentStream cs, PDFont fuente, float y) throws IOException {
        cs.setFont(fuente, 9);
        cs.beginText();
        cs.newLineAtOffset(MARGEN, y);
        cs.showText("Código      Descripción                          Cant.   P.Unit.   Desc.   Total");
        cs.endText();
        return y - 12;
    }

    private float escribirFilaDetalle(PDPageContentStream cs, PDFont fuente, float y, DetalleComprobante d)
            throws IOException {
        String descripcionCorta = d.getDescripcion().length() > 28
            ? d.getDescripcion().substring(0, 28) : d.getDescripcion();
        String fila = String.format("%-11s %-28s %6s %9s %7s %9s",
            d.getCodigoPrincipal(), descripcionCorta, d.getCantidad().toPlainString(),
            moneda(d.getPrecioUnitario()), moneda(d.getDescuento()), moneda(d.precioTotalSinImpuesto()));
        return escribir(cs, fuente, 9, MARGEN, y, fila);
    }

    private float escribir(PDPageContentStream cs, PDFont fuente, float tamano, float x, float y, String texto)
            throws IOException {
        cs.setFont(fuente, tamano);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(sanear(texto));
        cs.endText();
        return y - (tamano + 4);
    }

    /** PDFBox (fuentes estándar WinAnsi) no soporta todo Unicode — se filtra a caracteres representables. */
    private String sanear(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (char ch : texto.toCharArray()) {
            sb.append(ch < 256 ? ch : '?');
        }
        return sb.toString();
    }

    private String moneda(java.math.BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
