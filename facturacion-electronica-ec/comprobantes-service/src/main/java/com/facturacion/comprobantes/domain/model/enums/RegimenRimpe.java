package com.facturacion.comprobantes.domain.model.enums;

/**
 * Régimen RIMPE del emisor (Anexo 22 de la ficha técnica del SRI). La leyenda es literal: va tal cual en la etiqueta
 * {@code <contribuyenteRimpe>} del XML y en el recuadro del emisor del RIDE.
 */
public enum RegimenRimpe {

    RIMPE("CONTRIBUYENTE RÉGIMEN RIMPE"),
    NEGOCIO_POPULAR("CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE");

    private final String leyenda;

    RegimenRimpe(String leyenda) {
        this.leyenda = leyenda;
    }

    public String leyenda() {
        return leyenda;
    }

    /** El régimen de un texto ({@code RIMPE} o {@code NEGOCIO_POPULAR}); {@code null} si viene vacío (no es RIMPE). */
    public static RegimenRimpe desde(String codigo) {
        return codigo == null || codigo.isBlank() ? null : valueOf(codigo.trim());
    }
}
