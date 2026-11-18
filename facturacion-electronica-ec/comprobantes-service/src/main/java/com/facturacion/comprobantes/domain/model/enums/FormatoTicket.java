package com.facturacion.comprobantes.domain.model.enums;

import java.util.Arrays;
import java.util.Optional;

/** Cómo se entrega el ticket para impresoras de recibos: texto plano (cualquier cliente) o comandos ESC/POS crudos. */
public enum FormatoTicket {

    /** Texto plano UTF-8, una línea por renglón, ya ajustado al ancho del papel. */
    TEXTO(FormatoTicket.NOMBRE_TEXTO, FormatoTicket.TIPO_TEXTO, ".txt", "inline"),
    /** Bytes ESC/POS listos para enviar tal cual a la impresora (negrita, doble tamaño, corte de papel); solo ASCII. */
    ESCPOS(FormatoTicket.NOMBRE_ESCPOS, FormatoTicket.TIPO_ESCPOS, ".bin", "attachment");

    /** Nombre del parámetro de la URL y sus valores (constantes: también los usan las anotaciones del REST). */
    public static final String PARAMETRO = "formato";
    public static final String NOMBRE_TEXTO = "texto";
    public static final String NOMBRE_ESCPOS = "escpos";
    public static final String POR_DEFECTO = NOMBRE_TEXTO;
    public static final String TIPO_TEXTO = "text/plain;charset=UTF-8";
    public static final String TIPO_ESCPOS = "application/octet-stream";

    private final String nombre;
    private final String tipoContenido;
    private final String extension;
    private final String disposicion;

    FormatoTicket(String nombre, String tipoContenido, String extension, String disposicion) {
        this.nombre = nombre;
        this.tipoContenido = tipoContenido;
        this.extension = extension;
        this.disposicion = disposicion;
    }

    /** Valor del parámetro {@code formato} de la URL. */
    public String nombre() {
        return nombre;
    }

    public String tipoContenido() {
        return tipoContenido;
    }

    public String extension() {
        return extension;
    }

    /** {@code inline} si el cliente puede mostrarlo; {@code attachment} si es binario para la impresora. */
    public String disposicion() {
        return disposicion;
    }

    public static Optional<FormatoTicket> buscarPorNombre(String nombre) {
        return Arrays.stream(values()).filter(f -> f.nombre.equalsIgnoreCase(nombre)).findFirst();
    }
}
