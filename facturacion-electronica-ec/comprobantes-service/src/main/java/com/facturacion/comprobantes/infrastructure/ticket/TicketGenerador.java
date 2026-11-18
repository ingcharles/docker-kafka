package com.facturacion.comprobantes.infrastructure.ticket;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.EtiquetaTotal;
import com.facturacion.comprobantes.domain.model.enums.FormatoTicket;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoIdentificacionSri;
import com.facturacion.comprobantes.domain.model.vo.AnchoTicket;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.Pago;
import com.facturacion.comprobantes.domain.port.GeneradorTicket;
import com.facturacion.comprobantes.domain.service.TotalesComprobante;

/**
 * Ticket (recibo corto) para impresoras térmicas de 58 u 80 mm con los datos mínimos que se le entregan al cliente:
 * emisor, número del documento, fecha, cliente, líneas, totales, forma de pago y estado de autorización con la clave
 * de acceso. No reemplaza al RIDE (el comprobante legal en PDF).
 * <p>
 * Se arma una sola vez como líneas ya ajustadas al ancho del papel y se entrega en texto plano o en ESC/POS. El
 * ESC/POS sale en ASCII (sin tildes ni eñes) porque la página de códigos de las impresoras varía de un modelo a otro.
 */
@ApplicationScoped
public class TicketGenerador implements GeneradorTicket {


    // Formato del texto.
    private static final char CARACTER_RAYA = '-';
    private static final char CARACTER_ESPACIO = ' ';
    private static final String SEPARADOR_NUMERO_DOCUMENTO = "-";
    private static final char SALTO_DE_LINEA = '\n';
    private static final String SEPARADOR_PALABRAS = "\\s+";
    private static final String MARCAS_DIACRITICAS = "\\p{M}";
    private static final char REEMPLAZO_NO_ASCII = '?';
    private static final int PRIMER_ASCII_IMPRIMIBLE = 32;
    private static final int ULTIMO_ASCII_IMPRIMIBLE = 126;
    private static final String SANGRIA_LINEA = "  ";
    private static final String SEPARADOR_CANTIDAD_PRECIO = " x ";
    private static final int DECIMALES_DINERO = 2;
    private static final int ESPACIO_MINIMO_ENTRE_COLUMNAS = 1;
    private static final int LINEAS_EN_BLANCO_ANTES_DE_CORTAR = 3;

    // Comandos ESC/POS (Epson): inicializar, negrita, tamaño de carácter y corte parcial.
    private static final byte[] INICIAR = {0x1B, 0x40};
    private static final byte[] NEGRITA_SI = {0x1B, 0x45, 0x01};
    private static final byte[] NEGRITA_NO = {0x1B, 0x45, 0x00};
    private static final byte[] DOBLE_SI = {0x1D, 0x21, 0x11};
    private static final byte[] DOBLE_NO = {0x1D, 0x21, 0x00};
    private static final byte[] CORTAR = {0x1D, 0x56, 0x42, 0x00};

    /** Una línea ya ajustada al ancho; {@code doble} ocupa el doble de ancho y alto (la mitad de columnas). */
    record Linea(String texto, boolean negrita, boolean doble) {

        static Linea crearNormal(String texto) {
            return new Linea(texto, false, false);
        }

        static Linea crearNegrita(String texto) {
            return new Linea(texto, true, false);
        }
    }

    @Override
    public byte[] generar(Comprobante c, FormatoTicket formato, AnchoTicket ancho, CatalogosSriAFecha catalogos) {
        List<Linea> lineas = armar(c, ancho.columnas(), catalogos);
        return formato == FormatoTicket.ESCPOS ? armarEscPos(lineas) : armarTexto(lineas);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Contenido
    // ---------------------------------------------------------------------------------------------------------------

    List<Linea> armar(Comprobante c, int columnas, CatalogosSriAFecha catalogos) {
        List<Linea> l = new ArrayList<>();
        String raya = String.valueOf(CARACTER_RAYA).repeat(columnas);

        partir(c.getEmpresaRazonSocial(), columnas).forEach(t -> l.add(Linea.crearNegrita(centrar(t, columnas))));
        l.add(Linea.crearNormal(centrar(TextoTicket.RUC.texto() + c.getEmpresaRuc(), columnas)));
        String direccion = c.getDireccionEstablecimiento() != null ? c.getDireccionEstablecimiento()
            : c.getEmpresaDireccionMatriz();
        if (direccion != null && !direccion.isBlank()) {
            partir(direccion, columnas).forEach(t -> l.add(Linea.crearNormal(centrar(t, columnas))));
        }
        l.add(Linea.crearNormal(raya));

        partir(c.getTipoComprobante().nombre(), columnas).forEach(t -> l.add(Linea.crearNegrita(t)));
        l.add(Linea.crearNegrita(TextoTicket.NUMERO.texto() + c.getEstablecimiento() + SEPARADOR_NUMERO_DOCUMENTO
            + c.getPuntoEmision() + SEPARADOR_NUMERO_DOCUMENTO + c.getSecuencial()));
        l.add(Linea.crearNormal(TextoTicket.FECHA.texto() + FechasSri.formatearFechaHoraCorta(c.getFechaEmision())));
        if (c.getDocModificadoNumero() != null) {
            String modifica = TipoComprobante.buscarPorCodigo(c.getDocModificadoTipo()).map(TipoComprobante::nombre)
                .orElse(TextoTicket.DOCUMENTO_GENERICO.texto());
            partir(TextoTicket.MODIFICA.texto() + modifica + CARACTER_ESPACIO + c.getDocModificadoNumero(), columnas)
                .forEach(t -> l.add(Linea.crearNormal(t)));
        }
        agregarComprador(l, c, columnas);
        l.add(Linea.crearNormal(raya));

        for (DetalleComprobante d : c.getDetalles()) {
            partir(d.getDescripcion(), columnas).forEach(t -> l.add(Linea.crearNormal(t)));
            l.addAll(armarDosColumnas(SANGRIA_LINEA + formatearCantidad(d.getCantidad()) + SEPARADOR_CANTIDAD_PRECIO
                + formatearPrecio(d.getPrecioUnitario()), formatearDinero(d.calcularPrecioTotalSinImpuesto()), columnas));
        }
        l.add(Linea.crearNormal(raya));

        for (TotalesComprobante.Total total : TotalesComprobante.calcular(c)) {
            boolean esTotal = total.tipo() == EtiquetaTotal.VALOR_TOTAL;
            if (total.valor().signum() == 0 && !esTotal) {
                continue;
            }
            List<Linea> fila = armarDosColumnas(total.etiqueta(), formatearDinero(total.valor()), columnas);
            l.addAll(esTotal ? fila.stream().map(f -> Linea.crearNegrita(f.texto())).toList() : fila);
        }
        agregarPagos(l, c, columnas, catalogos);
        l.add(Linea.crearNormal(raya));

        agregarAutorizacion(l, c, columnas);
        l.add(Linea.crearNormal(""));
        l.add(Linea.crearNormal(centrar(TextoTicket.AGRADECIMIENTO.texto(), columnas)));
        return l;
    }

    private void agregarComprador(List<Linea> l, Comprobante c, int columnas) {
        if (c.getCompradorRazonSocial() == null && c.getCompradorIdentificacion() == null) {
            return;
        }
        if (TipoIdentificacionSri.CONSUMIDOR_FINAL.codigo().equals(c.getCompradorTipoIdentificacion())) {
            l.add(Linea.crearNormal(TextoTicket.CLIENTE.texto() + TextoTicket.CONSUMIDOR_FINAL.texto()));
            return;
        }
        if (c.getCompradorRazonSocial() != null) {
            partir(TextoTicket.CLIENTE.texto() + c.getCompradorRazonSocial(), columnas)
                .forEach(t -> l.add(Linea.crearNormal(t)));
        }
        if (c.getCompradorIdentificacion() != null) {
            l.add(Linea.crearNormal(TextoTicket.IDENTIFICACION.texto() + c.getCompradorIdentificacion()));
        }
    }

    private void agregarPagos(List<Linea> l, Comprobante c, int columnas, CatalogosSriAFecha catalogos) {
        if (c.getPagos() == null || c.getPagos().isEmpty()) {
            return;
        }
        l.add(Linea.crearNormal(""));
        l.add(Linea.crearNegrita(TextoTicket.FORMA_DE_PAGO.texto()));
        for (Pago pago : c.getPagos()) {
            l.addAll(armarDosColumnas(catalogos.descripcionFormaPago(pago.formaPago()), formatearDinero(pago.total()), columnas));
        }
    }

    private void agregarAutorizacion(List<Linea> l, Comprobante c, int columnas) {
        EstadoComprobante estado = c.getEstado();
        if (estado == EstadoComprobante.AUTORIZADO || estado == EstadoComprobante.NOTIFICADO) {
            l.add(Linea.crearNegrita(centrar(TextoTicket.AUTORIZADO.texto(), columnas)));
            if (c.getFechaAutorizacion() != null) {
                l.add(Linea.crearNormal(TextoTicket.FECHA_AUTORIZACION.texto()
                    + FechasSri.formatearFechaHoraCorta(c.getFechaAutorizacion())));
            }
            l.add(Linea.crearNormal(TextoTicket.NUMERO_AUTORIZACION.texto()));
            partirParejo(c.getNumeroAutorizacion() != null ? c.getNumeroAutorizacion() : c.getClaveAcceso(), columnas)
                .forEach(t -> l.add(Linea.crearNormal(t)));
        } else if (estado == EstadoComprobante.BORRADOR || estado == EstadoComprobante.FIRMADO) {
            l.add(Linea.crearNegrita(centrar(TextoTicket.EN_PROCESO.texto(), columnas)));
            l.add(Linea.crearNormal(TextoTicket.CLAVE_DE_ACCESO.texto()));
            partirParejo(c.getClaveAcceso(), columnas).forEach(t -> l.add(Linea.crearNormal(t)));
        } else {
            l.add(Linea.crearNegrita(centrar(TextoTicket.NO_AUTORIZADO.texto(), columnas)));
            l.add(Linea.crearNormal(centrar(TextoTicket.SIN_VALOR.texto(), columnas)));
        }
        if (c.getAmbiente() == AmbienteSri.PRUEBAS) {
            l.add(Linea.crearNegrita(centrar(TextoTicket.AMBIENTE_PRUEBAS.texto(), columnas)));
            l.add(Linea.crearNormal(centrar(TextoTicket.SIN_VALIDEZ_TRIBUTARIA.texto(), columnas)));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Salida
    // ---------------------------------------------------------------------------------------------------------------

    private byte[] armarTexto(List<Linea> lineas) {
        StringBuilder sb = new StringBuilder();
        lineas.forEach(l -> sb.append(l.texto().stripTrailing()).append(SALTO_DE_LINEA));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] armarEscPos(List<Linea> lineas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(INICIAR);
        for (Linea l : lineas) {
            if (l.negrita()) {
                out.writeBytes(NEGRITA_SI);
            }
            if (l.doble()) {
                out.writeBytes(DOBLE_SI);
            }
            out.writeBytes(convertirAAscii(l.texto().stripTrailing()).getBytes(StandardCharsets.US_ASCII));
            out.write(SALTO_DE_LINEA);
            if (l.doble()) {
                out.writeBytes(DOBLE_NO);
            }
            if (l.negrita()) {
                out.writeBytes(NEGRITA_NO);
            }
        }
        for (int i = 0; i < LINEAS_EN_BLANCO_ANTES_DE_CORTAR; i++) {
            out.write(SALTO_DE_LINEA);
        }
        out.writeBytes(CORTAR);
        return out.toByteArray();
    }

    /** Sin tildes ni eñes: cualquier otro carácter que no sea ASCII imprimible se cambia por "?". */
    static String convertirAAscii(String texto) {
        String sinMarcas = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll(MARCAS_DIACRITICAS, "");
        StringBuilder sb = new StringBuilder(sinMarcas.length());
        for (char ch : sinMarcas.toCharArray()) {
            sb.append(ch >= PRIMER_ASCII_IMPRIMIBLE && ch <= ULTIMO_ASCII_IMPRIMIBLE ? ch : REEMPLAZO_NO_ASCII);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Formato
    // ---------------------------------------------------------------------------------------------------------------

    /** Parte por palabras en renglones de a lo más {@code ancho}; una palabra más larga se corta. */
    static List<String> partir(String texto, int ancho) {
        List<String> renglones = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return renglones;
        }
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.trim().split(SEPARADOR_PALABRAS)) {
            while (palabra.length() > ancho) {
                if (actual.length() > 0) {
                    renglones.add(actual.toString());
                    actual.setLength(0);
                }
                renglones.add(palabra.substring(0, ancho));
                palabra = palabra.substring(ancho);
            }
            if (actual.length() > 0 && actual.length() + 1 + palabra.length() > ancho) {
                renglones.add(actual.toString());
                actual.setLength(0);
            }
            if (actual.length() > 0) {
                actual.append(CARACTER_ESPACIO);
            }
            actual.append(palabra);
        }
        if (actual.length() > 0) {
            renglones.add(actual.toString());
        }
        return renglones;
    }

    /** Parte un texto sin espacios (la clave de acceso) en renglones del mismo largo, no uno lleno y otro con un dígito. */
    static List<String> partirParejo(String texto, int ancho) {
        List<String> renglones = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            return renglones;
        }
        int cantidad = (texto.length() + ancho - 1) / ancho;
        int largo = (texto.length() + cantidad - 1) / cantidad;
        for (int desde = 0; desde < texto.length(); desde += largo) {
            renglones.add(texto.substring(desde, Math.min(desde + largo, texto.length())));
        }
        return renglones;
    }

    static String centrar(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto;
        }
        return String.valueOf(CARACTER_ESPACIO).repeat((ancho - texto.length()) / 2) + texto;
    }

    /** Etiqueta a la izquierda y valor pegado a la derecha; si no caben juntos, la etiqueta se parte y el valor va en el último renglón. */
    static List<Linea> armarDosColumnas(String izquierda, String derecha, int ancho) {
        List<Linea> lineas = new ArrayList<>();
        int espacio = ancho - derecha.length() - ESPACIO_MINIMO_ENTRE_COLUMNAS;
        List<String> partes = partir(izquierda, Math.max(espacio, 1));
        if (partes.isEmpty()) {
            partes = List.of("");
        }
        for (int i = 0; i < partes.size() - 1; i++) {
            lineas.add(Linea.crearNormal(partes.get(i)));
        }
        String ultimo = partes.get(partes.size() - 1);
        int relleno = Math.max(ancho - ultimo.length() - derecha.length(), ESPACIO_MINIMO_ENTRE_COLUMNAS);
        lineas.add(Linea.crearNormal(ultimo + String.valueOf(CARACTER_ESPACIO).repeat(relleno) + derecha));
        return lineas;
    }

    private static String formatearDinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(DECIMALES_DINERO, RoundingMode.HALF_UP)
            .toPlainString();
    }

    /** Sin ceros de más: "2", "0.5". */
    private static String formatearCantidad(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO.toPlainString();
        }
        BigDecimal limpio = valor.stripTrailingZeros();
        return (limpio.scale() < 0 ? limpio.setScale(0, RoundingMode.UNNECESSARY) : limpio).toPlainString();
    }

    /** Con al menos 2 decimales ("10.00") y sin ceros de más después de ellos ("3.333333"). */
    private static String formatearPrecio(BigDecimal valor) {
        if (valor == null) {
            return formatearDinero(null);
        }
        BigDecimal limpio = valor.stripTrailingZeros();
        return (limpio.scale() < DECIMALES_DINERO ? limpio.setScale(DECIMALES_DINERO, RoundingMode.UNNECESSARY) : limpio)
            .toPlainString();
    }
}
