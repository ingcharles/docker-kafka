package com.facturacion.notificacion.domain.model.enums;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tipos de comprobante para los que hay plantilla de correo. {@link #etiqueta} es el nombre que ve el receptor y
 * {@link #plantilla} el nombre de los archivos {@code templates/correo/<plantilla>.html} y {@code .txt}. Un tipo que el
 * servicio no conoce usa {@link #GENERICO}, así que el correo sale aunque aparezca un tipo nuevo.
 */
public enum TipoComprobanteCorreo {

    FACTURA("Factura", "factura"),
    NOTA_CREDITO("Nota de crédito", "nota-credito"),
    NOTA_DEBITO("Nota de débito", "nota-debito"),
    GUIA_REMISION("Guía de remisión", "guia-remision"),
    RETENCION("Comprobante de retención", "retencion"),
    LIQUIDACION_COMPRA("Liquidación de compra", "liquidacion-compra"),
    GENERICO("Comprobante", "comprobante");

    private final String etiqueta;
    private final String plantilla;

    TipoComprobanteCorreo(String etiqueta, String plantilla) {
        this.etiqueta = etiqueta;
        this.plantilla = plantilla;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String plantilla() {
        return plantilla;
    }

    /** El tipo que corresponde al nombre que viaja en el evento ({@code FACTURA}, {@code NOTA_CREDITO}...), o {@link #GENERICO}. */
    public static TipoComprobanteCorreo obtenerPorNombre(String nombre) {
        return buscarPorNombre(nombre).orElse(GENERICO);
    }

    /** El tipo con ese nombre exacto, o vacío si no existe (para validar lo que escribe una persona). */
    public static Optional<TipoComprobanteCorreo> buscarPorNombre(String nombre) {
        return Arrays.stream(values()).filter(tipo -> tipo.name().equals(nombre)).findFirst();
    }

    /** Los nombres válidos, para los mensajes de error. */
    public static String listarNombres() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
