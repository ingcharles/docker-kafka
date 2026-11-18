package com.facturacion.comprobantes.infrastructure.ride;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.google.zxing.oned.Code128Writer;

import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.port.GeneradorRide;
import com.facturacion.comprobantes.domain.service.TotalesComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * RIDE (Representación Impresa de Documento Electrónico) en PDF — Anexo 2 de la ficha técnica del SRI. Se genera una
 * sola vez, cuando el comprobante pasa a AUTORIZADO, a partir de los datos ya autorizados: el RIDE debe mostrar el
 * número y la fecha de autorización reales.
 * <p>
 * Formato (A4 vertical): arriba, a la izquierda, el recuadro del emisor (razón social y datos tributarios) y debajo el
 * del comprador; a la derecha, el recuadro del documento (RUC, tipo, número, autorización, ambiente, emisión y la clave
 * de acceso con su código de barras Code 128). Después, la tabla del detalle con bordes; abajo, "Información
 * Adicional" a la izquierda y los totales por tarifa a la derecha. Si el detalle no cabe en la página, continúa en la
 * siguiente repitiendo el encabezado de la tabla.
 * <p>
 * {@code colorRide} (opcional, por empresa, ver {@code configuracion_empresa}) es el color del número del documento;
 * por defecto, rojo.
 */
@ApplicationScoped
public class RideGenerador implements GeneradorRide {

    private static final float ANCHO_PAGINA = PDRectangle.A4.getWidth();
    static final float ALTO_PAGINA = PDRectangle.A4.getHeight();
    static final float MARGEN = 28f;
    private static final float ANCHO_UTIL = ANCHO_PAGINA - 2 * MARGEN;
    private static final float ANCHO_COLUMNA_IZQUIERDA = 286f;
    private static final float SEPARACION = 10f;
    static final float SEPARACION_CAJAS = 4f;
    private static final float ANCHO_COLUMNA_DERECHA = ANCHO_UTIL - ANCHO_COLUMNA_IZQUIERDA - SEPARACION;
    static final float ALTO_CAJA_EMISOR = 202f;
    private static final float ALTO_MINIMO_CAJA_COMPRADOR = 66f;
    private static final float TAMANO_COMPRADOR = 8f;
    private static final float MARGEN_SUPERIOR_COMPRADOR = 17f;
    private static final float INTERLINEA_ADICIONAL = 2.5f;
    private static final float SEPARACION_FILAS = 2f;
    private static final float ALTO_MINIMO_CAJA_DOCUMENTO = 272f;
    private static final float RADIO_ESQUINA = 9f;
    private static final float ALTO_CODIGO_BARRAS = 38f;
    private static final float ANCHO_ETIQUETA_EMISOR = 118f;

    private static final float[] ANCHOS_COLUMNAS = {62f, 42f, 150f, 110f, 60f, 55f, 60f};
    private static final String[] TITULOS_COLUMNAS =
        {"Cod. Principal", "Cant", "Descripción", "Detalles Adicionales", "Precio Unit.", "Descuento", "Precio Total"};
    private static final float ALTO_ENCABEZADO_TABLA = 24f;
    private static final float TAMANO_TABLA = 7.5f;
    private static final float INTERLINEA_TABLA = 9f;

    private static final float ANCHO_INFO_ADICIONAL = 330f;
    private static final float ANCHO_ETIQUETA_INFO = 105f;
    private static final float ANCHO_TOTALES = 197f;
    private static final float ANCHO_ETIQUETA_TOTAL = 130f;
    private static final float ALTO_FILA_TOTAL = 14f;

    private static final float[] COLOR_NUMERO_POR_DEFECTO = {0.85f, 0.15f, 0.15f};

    private final PDFont fuenteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont fuenteNegrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /** Página en curso: el contenido se escribe de arriba hacia abajo y, al agotarse, abre otra. */
    private static final class Lienzo implements AutoCloseable {
        private final PDDocument documento;
        private PDPageContentStream flujo;
        private float y;

        Lienzo(PDDocument documento) throws IOException {
            this.documento = documento;
            abrirPagina();
        }

        void abrirPagina() throws IOException {
            if (flujo != null) {
                flujo.close();
            }
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            flujo = new PDPageContentStream(documento, pagina);
            y = ALTO_PAGINA - MARGEN;
        }

        @Override
        public void close() throws IOException {
            if (flujo != null) {
                flujo.close();
            }
        }
    }

    /** Una fila de "etiqueta: valor". */
    private record Fila(String etiqueta, String valor) {
    }

    /**
     * @param colorRide hex RGB opcional (ej. "#1A73E8", con o sin '#') para el número del documento; si es
     *                  {@code null} o no es un hex válido se usa el rojo por defecto.
     */
    @Override
    public byte[] generar(Comprobante c, String colorRide, byte[] logo, CatalogosSriAFecha catalogos) {
        try (PDDocument documento = new PDDocument()) {
            try (Lienzo lienzo = new Lienzo(documento)) {
                dibujarEncabezado(lienzo, c, interpretarColor(colorRide), logo);
                if (c.getDetalles() != null && !c.getDetalles().isEmpty()) {
                    dibujarTablaDetalle(lienzo, c);
                    dibujarInformacionYTotales(lienzo, c, catalogos);
                }
                if (c.getEstado() == EstadoComprobante.NO_AUTORIZADO && c.getMotivoRechazo() != null) {
                    asegurarEspacio(lienzo, 30f);
                    escribirTexto(lienzo.flujo, fuenteNegrita, 9, MARGEN, lienzo.y - 12,
                        "NO AUTORIZADO: " + c.getMotivoRechazo());
                }
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new ErrorInternoException(e, CodigoError.RIDE_NO_GENERADO, c.getId());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Encabezado: recuadro del emisor, recuadro del comprador y recuadro del documento
    // ---------------------------------------------------------------------------------------------------------------

    private void dibujarEncabezado(Lienzo l, Comprobante c, float[] colorNumero, byte[] logo) throws IOException {
        PDPageContentStream cs = l.flujo;
        float arriba = l.y;
        float xIzquierda = MARGEN;
        float xDerecha = MARGEN + ANCHO_COLUMNA_IZQUIERDA + SEPARACION;

        List<Fila> filasComprador = construirFilasComprador(c);
        // Etiquetas cortas (Fecha Emisión, Cédula/RUC, Razón Social) dejan más ancho al valor; las de una nota de crédito/débito son largas.
        float anchoEtiquetaComprador = c.getDocModificadoNumero() == null ? 88f : ANCHO_ETIQUETA_EMISOR;
        float anchoValorComprador = ANCHO_COLUMNA_IZQUIERDA - 16 - anchoEtiquetaComprador;
        List<List<String>> lineasComprador = new ArrayList<>();
        for (Fila fila : filasComprador) {
            lineasComprador.add(ajustar(fila.valor(), fuenteNormal, TAMANO_COMPRADOR, anchoValorComprador));
        }
        float altoComprador = calcularAltoCajaComprador(lineasComprador);
        float altoIzquierda = ALTO_CAJA_EMISOR + SEPARACION_CAJAS + altoComprador;
        float altoDocumento = Math.max(ALTO_MINIMO_CAJA_DOCUMENTO, altoIzquierda);

        dibujarCajaEmisor(l, c, xIzquierda, arriba, logo);

        float yComprador = arriba - ALTO_CAJA_EMISOR - SEPARACION_CAJAS;
        dibujarCajaRedondeada(cs, xIzquierda, yComprador - altoComprador, ANCHO_COLUMNA_IZQUIERDA, altoComprador);
        float y = yComprador - MARGEN_SUPERIOR_COMPRADOR;
        for (int i = 0; i < filasComprador.size(); i++) {
            y = dibujarFilaLineas(cs, xIzquierda + 8, y, anchoEtiquetaComprador, filasComprador.get(i).etiqueta(),
                lineasComprador.get(i), TAMANO_COMPRADOR);
        }

        dibujarCajaRedondeada(cs, xDerecha, arriba - altoDocumento, ANCHO_COLUMNA_DERECHA, altoDocumento);
        dibujarCajaDocumento(cs, c, colorNumero, xDerecha, arriba);

        l.y = arriba - altoDocumento - 12;
    }

    private void dibujarCajaEmisor(Lienzo l, Comprobante c, float x, float arriba, byte[] logo) throws IOException {
        PDPageContentStream cs = l.flujo;
        dibujarCajaRedondeada(cs, x, arriba - ALTO_CAJA_EMISOR, ANCHO_COLUMNA_IZQUIERDA, ALTO_CAJA_EMISOR);
        float centro = x + ANCHO_COLUMNA_IZQUIERDA / 2;
        float y = arriba - 36;
        PDImageXObject imagen = cargarLogo(l.documento, logo);
        if (imagen != null) {
            dibujarLogo(cs, imagen, x, arriba);
        } else if (c.getEmpresaNombreComercial() != null && !c.getEmpresaNombreComercial().isBlank()) {
            // Sin logo: el nombre comercial ocupa el espacio de la marca.
            for (String linea : ajustar(c.getEmpresaNombreComercial(), fuenteNegrita, 15, ANCHO_COLUMNA_IZQUIERDA - 24)) {
                escribirTextoCentrado(cs, fuenteNegrita, 15, centro, y, linea);
                y -= 18;
            }
        }
        // Con logo, la razón social empieza debajo de la imagen (que llega hasta arriba - 96).
        y = Math.min(y, imagen != null ? arriba - 112 : arriba - 100);
        for (String linea : ajustar(c.getEmpresaRazonSocial(), fuenteNegrita, 10.5f, ANCHO_COLUMNA_IZQUIERDA - 20)) {
            escribirTextoCentrado(cs, fuenteNegrita, 10.5f, centro, y, linea);
            y -= 13;
        }
        y -= 8;
        List<Fila> datos = new ArrayList<>();
        datos.add(new Fila("Dirección Matriz:", c.getEmpresaDireccionMatriz()));
        if (c.getDireccionEstablecimiento() != null && !c.getDireccionEstablecimiento().isBlank()) {
            datos.add(new Fila("Dirección Sucursal:", c.getDireccionEstablecimiento()));
        }
        if (c.isEmpresaContribuyenteEspecial() && c.getEmpresaNumeroContribuyenteEspecial() != null) {
            datos.add(new Fila("Contribuyente Especial Nro:", c.getEmpresaNumeroContribuyenteEspecial()));
        }
        datos.add(new Fila("Obligado A Llevar Contabilidad:", c.isEmpresaObligadoContabilidad() ? "SI" : "NO"));
        // Anexo 21 y Anexo 22: la leyenda va completa en negrita, sin valor aparte.
        if (c.getEmpresaAgenteRetencion() != null && !c.getEmpresaAgenteRetencion().isBlank()) {
            datos.add(new Fila("Agente de Retención Resolución No. " + c.getEmpresaAgenteRetencion(), ""));
        }
        if (c.getEmpresaRegimenRimpe() != null) {
            datos.add(new Fila(c.getEmpresaRegimenRimpe().leyenda(), ""));
        }
        for (Fila dato : datos) {
            List<String> lineas = ajustar(dato.valor(), fuenteNormal, 7, ANCHO_COLUMNA_IZQUIERDA - 16 - ANCHO_ETIQUETA_EMISOR);
            y = dibujarFilaLineas(cs, x + 8, y, ANCHO_ETIQUETA_EMISOR, dato.etiqueta(), lineas.isEmpty() ? List.of("") : lineas, 7);
        }
    }

    /** El logo de la empresa; {@code null} si no hay o no es una imagen que se pueda leer (el RIDE sale sin él). */
    private PDImageXObject cargarLogo(PDDocument documento, byte[] logo) {
        if (logo == null || logo.length == 0) {
            return null;
        }
        try {
            return PDImageXObject.createFromByteArray(documento, logo, "logo");
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Centrado en la parte alta del recuadro del emisor, a escala: nunca deformado y sin pasar de 2x su tamaño. */
    private void dibujarLogo(PDPageContentStream cs, PDImageXObject imagen, float x, float arriba) throws IOException {
        float anchoMaximo = ANCHO_COLUMNA_IZQUIERDA - 28;
        float altoMaximo = 84f;
        float escala = Math.min(2f, Math.min(anchoMaximo / imagen.getWidth(), altoMaximo / imagen.getHeight()));
        float ancho = imagen.getWidth() * escala;
        float alto = imagen.getHeight() * escala;
        cs.drawImage(imagen, x + (ANCHO_COLUMNA_IZQUIERDA - ancho) / 2, arriba - 12 - alto, ancho, alto);
    }

    private void dibujarCajaDocumento(PDPageContentStream cs, Comprobante c, float[] colorNumero, float x,
            float arriba) throws IOException {
        float centro = x + ANCHO_COLUMNA_DERECHA / 2;
        float y = arriba - 30;
        escribirTextoCentrado(cs, fuenteNegrita, 15, centro, y, "R.U.C.:  " + c.getEmpresaRuc());
        y -= 32;
        String tipo = obtenerEtiquetaTipo(c);
        escribirTextoCentrado(cs, fuenteNegrita, calcularTamanoQueCabe(fuenteNegrita, tipo, 24, ANCHO_COLUMNA_DERECHA - 20), centro, y, tipo);
        y -= 28;
        cs.setNonStrokingColor(colorNumero[0], colorNumero[1], colorNumero[2]);
        escribirTextoCentrado(cs, fuenteNegrita, 15, centro, y,
            "No. " + c.getEstablecimiento() + "-" + c.getPuntoEmision() + "-" + c.getSecuencial());
        cs.setNonStrokingColor(0f, 0f, 0f);
        y -= 28;
        escribirTextoCentrado(cs, fuenteNegrita, 10, centro, y, "NÚMERO DE AUTORIZACIÓN");
        y -= 13;
        if (c.getNumeroAutorizacion() != null) {
            escribirTextoCentrado(cs, fuenteNormal, 7.5f, centro, y, c.getNumeroAutorizacion());
        }
        y -= 24;
        float xValor = x + 128;
        escribirTexto(cs, fuenteNegrita, 8.5f, x + 10, y, "FECHA Y HORA DE");
        escribirTexto(cs, fuenteNegrita, 8.5f, x + 10, y - 10, "AUTORIZACIÓN:");
        if (c.getFechaAutorizacion() != null) {
            escribirTexto(cs, fuenteNormal, 9, xValor, y - 5,
                FechasSri.formatearFechaHora(c.getFechaAutorizacion()));
        }
        y -= 26;
        escribirTexto(cs, fuenteNegrita, 8.5f, x + 10, y, "AMBIENTE:");
        escribirTexto(cs, fuenteNormal, 9, xValor, y, c.getAmbiente() == AmbienteSri.PRUEBAS ? "PRUEBAS" : "PRODUCCION");
        y -= 13;
        escribirTexto(cs, fuenteNegrita, 8.5f, x + 10, y, "EMISIÓN:");
        escribirTexto(cs, fuenteNormal, 9, xValor, y, "NORMAL");
        y -= 22;
        escribirTextoCentrado(cs, fuenteNegrita, 10, centro, y, "CLAVE DE ACCESO");
        y -= 6;
        dibujarCodigoBarras(cs, c.getClaveAcceso(), x + 12, y, ANCHO_COLUMNA_DERECHA - 24, ALTO_CODIGO_BARRAS);
        y -= ALTO_CODIGO_BARRAS + 10;
        escribirTextoCentrado(cs, fuenteNormal, 7.5f, centro, y, c.getClaveAcceso());
    }

    private List<Fila> construirFilasComprador(Comprobante c) {
        List<Fila> filas = new ArrayList<>();
        filas.add(new Fila("Fecha Emisión:", FechasSri.formatearFecha(c.getFechaEmision())));
        if (c.getCompradorIdentificacion() != null) {
            filas.add(new Fila("Cédula/RUC:", c.getCompradorIdentificacion()));
        }
        if (c.getCompradorRazonSocial() != null) {
            filas.add(new Fila("Razón Social:", c.getCompradorRazonSocial()));
        }
        if (c.getDocModificadoNumero() != null) {
            filas.add(new Fila("Comprobante que se modifica:",
                obtenerNombreTipoDocumento(c.getDocModificadoTipo()) + " " + c.getDocModificadoNumero()));
            if (c.getDocModificadoFecha() != null) {
                filas.add(new Fila("Fecha Emisión (a modificar):",
                    FechasSri.formatearFecha(c.getDocModificadoFecha())));
            }
            if (c.getMotivo() != null && !c.getMotivo().isBlank()) {
                filas.add(new Fila("Razón de Modificación:", c.getMotivo()));
            }
        }
        return filas;
    }

    private String obtenerNombreTipoDocumento(String codigo) {
        if (codigo == null) {
            return "";
        }
        return TipoComprobante.buscarPorCodigo(codigo).map(TipoComprobante::nombre).orElse(codigo);
    }

    private String obtenerEtiquetaTipo(Comprobante c) {
        return c.getTipoComprobante().nombre();
    }

    /** Code 128 (el código de barras de la clave de acceso): cada módulo encendido es una barra negra. */
    private void dibujarCodigoBarras(PDPageContentStream cs, String clave, float x, float arriba, float ancho,
            float alto) throws IOException {
        if (clave == null || clave.isBlank()) {
            return;
        }
        boolean[] modulos = new Code128Writer().encode(clave);
        float modulo = ancho / modulos.length;
        cs.setNonStrokingColor(0f, 0f, 0f);
        int i = 0;
        while (i < modulos.length) {
            if (!modulos[i]) {
                i++;
                continue;
            }
            int inicio = i;
            while (i < modulos.length && modulos[i]) {
                i++;
            }
            cs.addRect(x + inicio * modulo, arriba - alto, (i - inicio) * modulo, alto);
        }
        cs.fill();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Tabla del detalle
    // ---------------------------------------------------------------------------------------------------------------

    private void dibujarTablaDetalle(Lienzo l, Comprobante c) throws IOException {
        dibujarEncabezadoTabla(l);
        for (DetalleComprobante d : c.getDetalles()) {
            String[] celdas = {
                reemplazarNuloPorVacio(d.getCodigoPrincipal()),
                formatearNumero(d.getCantidad(), 4),
                reemplazarNuloPorVacio(d.getDescripcion()),
                armarDetalleAdicional(d),
                formatearNumero(d.getPrecioUnitario(), 4),
                formatearDinero(d.getDescuento()),
                formatearDinero(d.calcularPrecioTotalSinImpuesto())
            };
            List<List<String>> lineasPorCelda = new ArrayList<>();
            int maximoLineas = 1;
            for (int i = 0; i < celdas.length; i++) {
                List<String> lineas = ajustar(celdas[i], fuenteNormal, TAMANO_TABLA, ANCHOS_COLUMNAS[i] - 8);
                lineasPorCelda.add(lineas);
                maximoLineas = Math.max(maximoLineas, lineas.size());
            }
            float alto = maximoLineas * INTERLINEA_TABLA + 6;
            if (l.y - alto < MARGEN + 10) {
                l.abrirPagina();
                dibujarEncabezadoTabla(l);
            }
            dibujarFilaTabla(l, lineasPorCelda, alto);
        }
    }

    private void dibujarEncabezadoTabla(Lienzo l) throws IOException {
        PDPageContentStream cs = l.flujo;
        float x = MARGEN;
        cs.setLineWidth(0.8f);
        for (int i = 0; i < TITULOS_COLUMNAS.length; i++) {
            cs.addRect(x, l.y - ALTO_ENCABEZADO_TABLA, ANCHOS_COLUMNAS[i], ALTO_ENCABEZADO_TABLA);
            cs.stroke();
            List<String> lineas = ajustar(TITULOS_COLUMNAS[i], fuenteNegrita, 8, ANCHOS_COLUMNAS[i] - 6);
            float yTexto = l.y - ALTO_ENCABEZADO_TABLA / 2 - (lineas.size() * 9f) / 2 + 7;
            for (String linea : lineas) {
                escribirTextoCentrado(cs, fuenteNegrita, 8, x + ANCHOS_COLUMNAS[i] / 2, yTexto, linea);
                yTexto -= 9f;
            }
            x += ANCHOS_COLUMNAS[i];
        }
        l.y -= ALTO_ENCABEZADO_TABLA;
    }

    private void dibujarFilaTabla(Lienzo l, List<List<String>> lineasPorCelda, float alto) throws IOException {
        PDPageContentStream cs = l.flujo;
        float x = MARGEN;
        for (int i = 0; i < ANCHOS_COLUMNAS.length; i++) {
            cs.addRect(x, l.y - alto, ANCHOS_COLUMNAS[i], alto);
            cs.stroke();
            float yTexto = l.y - 9;
            boolean numerica = i == 1 || i >= 4;
            for (String linea : lineasPorCelda.get(i)) {
                if (numerica) {
                    escribirTextoDerecha(cs, fuenteNormal, TAMANO_TABLA, x + ANCHOS_COLUMNAS[i] - 4, yTexto, linea);
                } else {
                    escribirTexto(cs, fuenteNormal, TAMANO_TABLA, x + 4, yTexto, linea);
                }
                yTexto -= INTERLINEA_TABLA;
            }
            x += ANCHOS_COLUMNAS[i];
        }
        l.y -= alto;
    }

    /** Código auxiliar (si existe) y los detalles adicionales de la línea ("Medida: UN, Centro Logistico: 23"). */
    private String armarDetalleAdicional(DetalleComprobante d) {
        List<String> partes = new ArrayList<>();
        if (d.getCodigoAuxiliar() != null && !d.getCodigoAuxiliar().isBlank()) {
            partes.add("Cod. Auxiliar: " + d.getCodigoAuxiliar());
        }
        if (d.getDetallesAdicionales() != null) {
            d.getDetallesAdicionales().forEach(adicional -> partes.add(adicional.nombre() + ": " + reemplazarNuloPorVacio(adicional.valor())));
        }
        return String.join(", ", partes);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Información adicional (izquierda) y totales (derecha)
    // ---------------------------------------------------------------------------------------------------------------

    private void dibujarInformacionYTotales(Lienzo l, Comprobante c, CatalogosSriAFecha catalogos) throws IOException {
        List<Fila> info = construirInformacionAdicional(c, catalogos);
        List<String[]> totales = construirTotales(c);

        float altoInfo = 0;
        List<List<String>> lineasInfo = new ArrayList<>();
        for (Fila fila : info) {
            List<String> lineas = ajustar(fila.valor(), fuenteNormal, 8,
                ANCHO_INFO_ADICIONAL - ANCHO_ETIQUETA_INFO - 4);
            lineasInfo.add(lineas);
            altoInfo += lineas.size() * 10.5f + 2;
        }
        if (!info.isEmpty()) {
            altoInfo += 28;
        }
        float altoTotales = totales.size() * ALTO_FILA_TOTAL;
        float alto = Math.max(altoInfo, altoTotales);
        asegurarEspacio(l, alto + 8);

        PDPageContentStream cs = l.flujo;
        float arriba = l.y - 6;

        if (!info.isEmpty()) {
            dibujarInformacionAdicional(cs, info, lineasInfo, MARGEN, arriba);
        }

        float xTotales = MARGEN + ANCHO_UTIL - ANCHO_TOTALES;
        cs.setLineWidth(0.8f);
        float y = arriba;
        for (String[] fila : totales) {
            cs.addRect(xTotales, y - ALTO_FILA_TOTAL, ANCHO_ETIQUETA_TOTAL, ALTO_FILA_TOTAL);
            cs.addRect(xTotales + ANCHO_ETIQUETA_TOTAL, y - ALTO_FILA_TOTAL, ANCHO_TOTALES - ANCHO_ETIQUETA_TOTAL,
                ALTO_FILA_TOTAL);
            cs.stroke();
            escribirTexto(cs, fuenteNegrita, 7, xTotales + 4, y - 9.5f, fila[0]);
            escribirTextoDerecha(cs, fuenteNormal, 8, xTotales + ANCHO_TOTALES - 4, y - 9.5f, fila[1]);
            y -= ALTO_FILA_TOTAL;
        }
        l.y = arriba - alto;
    }

    private void dibujarInformacionAdicional(PDPageContentStream cs, List<Fila> info, List<List<String>> lineasInfo,
            float x, float arriba) throws IOException {
        float ancho = ANCHO_INFO_ADICIONAL - 40;
        float y = arriba - 14;
        escribirTextoCentrado(cs, fuenteNegrita, 11, x + ancho / 2, y, "Información Adicional");
        cs.setLineWidth(1.2f);
        cs.moveTo(x, y - 6);
        cs.lineTo(x + ancho, y - 6);
        cs.stroke();
        y -= 22;
        for (int i = 0; i < info.size(); i++) {
            y = dibujarFilaLineas(cs, x, y, ANCHO_ETIQUETA_INFO, info.get(i).etiqueta(), lineasInfo.get(i), 8);
            cs.setLineWidth(0.4f);
            cs.setLineDashPattern(new float[] {1f, 1.5f}, 0);
            cs.moveTo(x, y + 8.5f);
            cs.lineTo(x + ancho, y + 8.5f);
            cs.stroke();
            cs.setLineDashPattern(new float[] {}, 0);
            y -= 1;
        }
    }

    /** Forma de pago, RUC proveedor (Anexo 26), Gran Contribuyente (Anexo 24) y los campos adicionales del llamador, en ese orden. */
    private List<Fila> construirInformacionAdicional(Comprobante c, CatalogosSriAFecha catalogos) {
        List<Fila> filas = new ArrayList<>();
        if (c.getPagos() != null) {
            boolean primera = true;
            for (var pago : c.getPagos()) {
                filas.add(new Fila(primera ? "Forma de Pago:" : "",
                    catalogos.descripcionFormaPago(pago.formaPago()) + " " + formatearDinero(pago.total())));
                primera = false;
            }
        }
        if (c.getProveedorRuc() != null && !c.getProveedorRuc().isBlank()) {
            filas.add(new Fila("RUC Proveedor:", c.getProveedorRuc()));
        }
        if (c.isEmpresaEsGranContribuyente() && c.getEmpresaNumeroResolucionGranContribuyente() != null
                && !c.getEmpresaNumeroResolucionGranContribuyente().isBlank()) {
            filas.add(new Fila("Gran Contribuyente:", c.getEmpresaNumeroResolucionGranContribuyente()));
        }
        if (c.getCamposAdicionales() != null) {
            for (var campo : c.getCamposAdicionales()) {
                filas.add(new Fila(campo.nombre() + ":", reemplazarNuloPorVacio(campo.valor())));
            }
        }
        return filas;
    }

    /** Resumen de totales del RIDE: las filas salen de {@link TotalesComprobante} (compartido con el ticket). */
    private List<String[]> construirTotales(Comprobante c) {
        return TotalesComprobante.calcular(c).stream()
            .map(t -> new String[] {t.etiqueta(), formatearDinero(t.valor())}).toList();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Dibujo y formato
    // ---------------------------------------------------------------------------------------------------------------

    private void asegurarEspacio(Lienzo l, float alto) throws IOException {
        if (l.y - alto < MARGEN) {
            l.abrirPagina();
        }
    }

    private void dibujarCajaRedondeada(PDPageContentStream cs, float x, float y, float ancho, float alto) throws IOException {
        float r = RADIO_ESQUINA;
        float k = 0.5523f * r;
        cs.setLineWidth(0.9f);
        cs.moveTo(x + r, y);
        cs.lineTo(x + ancho - r, y);
        cs.curveTo(x + ancho - r + k, y, x + ancho, y + r - k, x + ancho, y + r);
        cs.lineTo(x + ancho, y + alto - r);
        cs.curveTo(x + ancho, y + alto - r + k, x + ancho - r + k, y + alto, x + ancho - r, y + alto);
        cs.lineTo(x + r, y + alto);
        cs.curveTo(x + r - k, y + alto, x, y + alto - r + k, x, y + alto - r);
        cs.lineTo(x, y + r);
        cs.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        cs.closePath();
        cs.stroke();
    }

    /** Etiqueta en negrita y, a su derecha, el valor ya partido en líneas; devuelve la y de la fila siguiente. */
    private float dibujarFilaLineas(PDPageContentStream cs, float x, float y, float anchoEtiqueta, String etiqueta,
            List<String> lineas, float tamano) throws IOException {
        escribirTexto(cs, fuenteNegrita, tamano, x, y, etiqueta);
        float interlinea = tamano + INTERLINEA_ADICIONAL;
        for (String linea : lineas) {
            escribirTexto(cs, fuenteNormal, tamano, x + anchoEtiqueta, y, linea);
            y -= interlinea;
        }
        return y - SEPARACION_FILAS;
    }

    /** Lo que baja {@link #filaLineas} al dibujar una fila de {@code lineas} líneas: la medida y el dibujo usan la misma cuenta. */
    static float calcularAvanceFila(int lineas, float tamano) {
        return lineas * (tamano + INTERLINEA_ADICIONAL) + SEPARACION_FILAS;
    }

    /** Alto del recuadro del comprador: lo que ocupan sus filas (ya partidas en líneas), con un mínimo. */
    static float calcularAltoCajaComprador(List<List<String>> lineasPorFila) {
        float alto = MARGEN_SUPERIOR_COMPRADOR;
        for (List<String> lineas : lineasPorFila) {
            alto += calcularAvanceFila(lineas.size(), TAMANO_COMPRADOR);
        }
        return Math.max(ALTO_MINIMO_CAJA_COMPRADOR, alto);
    }

    private void escribirTexto(PDPageContentStream cs, PDFont fuente, float tamano, float x, float y, String texto)
            throws IOException {
        cs.setFont(fuente, tamano);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(sanear(texto));
        cs.endText();
    }

    private void escribirTextoCentrado(PDPageContentStream cs, PDFont fuente, float tamano, float centro, float y,
            String texto) throws IOException {
        escribirTexto(cs, fuente, tamano, centro - medirAnchoTexto(fuente, tamano, sanear(texto)) / 2, y, texto);
    }

    private void escribirTextoDerecha(PDPageContentStream cs, PDFont fuente, float tamano, float derecha, float y,
            String texto) throws IOException {
        escribirTexto(cs, fuente, tamano, derecha - medirAnchoTexto(fuente, tamano, sanear(texto)), y, texto);
    }

    /** El mayor tamano (hasta {@code maximo}) con el que el texto cabe en {@code ancho}; los nombres largos se achican. */
    private float calcularTamanoQueCabe(PDFont fuente, String texto, float maximo, float ancho) throws IOException {
        float tamano = maximo;
        while (tamano > 8 && medirAnchoTexto(fuente, tamano, sanear(texto)) > ancho) {
            tamano -= 1;
        }
        return tamano;
    }

    private float medirAnchoTexto(PDFont fuente, float tamano, String texto) throws IOException {
        return fuente.getStringWidth(texto) / 1000f * tamano;
    }

    /** Parte el texto en líneas que quepan en {@code ancho}, cortando por palabras (y por letras si una no cabe). */
    private List<String> ajustar(String texto, PDFont fuente, float tamano, float ancho) throws IOException {
        List<String> lineas = new ArrayList<>();
        String limpio = sanear(reemplazarNuloPorVacio(texto)).trim();
        if (limpio.isEmpty()) {
            lineas.add("");
            return lineas;
        }
        StringBuilder actual = new StringBuilder();
        for (String palabra : limpio.split("\\s+")) {
            String candidata = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (medirAnchoTexto(fuente, tamano, candidata) <= ancho) {
                actual = new StringBuilder(candidata);
                continue;
            }
            if (actual.length() > 0) {
                lineas.add(actual.toString());
                actual = new StringBuilder();
            }
            String resto = palabra;
            while (medirAnchoTexto(fuente, tamano, resto) > ancho && resto.length() > 1) {
                int corte = resto.length() - 1;
                while (corte > 1 && medirAnchoTexto(fuente, tamano, resto.substring(0, corte)) > ancho) {
                    corte--;
                }
                lineas.add(resto.substring(0, corte));
                resto = resto.substring(corte);
            }
            actual.append(resto);
        }
        if (actual.length() > 0) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    /** PDFBox (fuentes estándar WinAnsi) no soporta todo Unicode — se filtra a caracteres representables. */
    private String sanear(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (char ch : texto.toCharArray()) {
            sb.append(ch >= 32 && ch < 256 ? ch : '?');
        }
        return sb.toString();
    }

    private String reemplazarNuloPorVacio(String valor) {
        return valor == null ? "" : valor;
    }

    private String formatearDinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Cantidad y precio unitario con al menos 4 decimales (el SRI admite hasta 6). */
    private String formatearNumero(BigDecimal valor, int decimalesMinimos) {
        if (valor == null) {
            return "";
        }
        int escala = Math.min(Math.max(valor.scale(), decimalesMinimos), 6);
        return valor.setScale(escala, RoundingMode.HALF_UP).toPlainString();
    }

    /** Rojo por defecto si {@code hex} es {@code null} o no es un color RGB válido de 6 dígitos (con o sin '#'). */
    private float[] interpretarColor(String hex) {
        if (hex == null) {
            return COLOR_NUMERO_POR_DEFECTO;
        }
        String limpio = hex.startsWith("#") ? hex.substring(1) : hex;
        if (!limpio.matches("[0-9a-fA-F]{6}")) {
            return COLOR_NUMERO_POR_DEFECTO;
        }
        return new float[] {
            Integer.parseInt(limpio.substring(0, 2), 16) / 255f,
            Integer.parseInt(limpio.substring(2, 4), 16) / 255f,
            Integer.parseInt(limpio.substring(4, 6), 16) / 255f};
    }
}
