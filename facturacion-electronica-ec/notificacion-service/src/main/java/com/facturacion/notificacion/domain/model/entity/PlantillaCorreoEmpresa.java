package com.facturacion.notificacion.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;

/**
 * Plantilla de correo propia de una empresa para un tipo de comprobante (o {@link TipoComprobanteCorreo#GENERICO}, que
 * aplica a cualquier tipo sin plantilla propia). El HTML se guarda ya limpiado. Cada cambio sube la versión; la anterior
 * queda en el historial.
 */
public class PlantillaCorreoEmpresa {

    /** Largo máximo del asunto. */
    public static final int ASUNTO_MAXIMO = 200;
    /** Largo máximo del cuerpo HTML, en caracteres. */
    public static final int HTML_MAXIMO = 100_000;
    /** Largo máximo del cuerpo en texto plano, en caracteres. */
    public static final int TEXTO_MAXIMO = 50_000;

    private static final int PRIMERA_VERSION = 1;

    private UUID id;
    private String empresaRuc;
    private String tipoComprobante;
    private String asunto;
    private String cuerpoHtml;
    private String cuerpoTexto;
    private int version;
    private boolean activa;
    private Instant creadoEn;
    private Instant actualizadoEn;

    /** La primera versión de la plantilla de una empresa para un tipo. */
    public static PlantillaCorreoEmpresa crear(String empresaRuc, TipoComprobanteCorreo tipo, String asunto,
            String cuerpoHtml, String cuerpoTexto, boolean activa, Instant ahora) {
        PlantillaCorreoEmpresa plantilla = new PlantillaCorreoEmpresa();
        plantilla.id = UUID.randomUUID();
        plantilla.empresaRuc = empresaRuc;
        plantilla.tipoComprobante = tipo.name();
        plantilla.asunto = asunto;
        plantilla.cuerpoHtml = cuerpoHtml;
        plantilla.cuerpoTexto = cuerpoTexto;
        plantilla.version = PRIMERA_VERSION;
        plantilla.activa = activa;
        plantilla.creadoEn = ahora;
        plantilla.actualizadoEn = ahora;
        return plantilla;
    }

    /** Cambia el contenido y sube la versión. */
    public void reemplazar(String nuevoAsunto, String nuevoHtml, String nuevoTexto, boolean nuevaActiva, Instant ahora) {
        this.asunto = nuevoAsunto;
        this.cuerpoHtml = nuevoHtml;
        this.cuerpoTexto = nuevoTexto;
        this.activa = nuevaActiva;
        this.version++;
        this.actualizadoEn = ahora;
    }

    /** Una copia con los mismos datos (para guardar la versión anterior en el historial antes de cambiarla). */
    public PlantillaCorreoEmpresa copiar() {
        PlantillaCorreoEmpresa copia = new PlantillaCorreoEmpresa();
        copia.id = id;
        copia.empresaRuc = empresaRuc;
        copia.tipoComprobante = tipoComprobante;
        copia.asunto = asunto;
        copia.cuerpoHtml = cuerpoHtml;
        copia.cuerpoTexto = cuerpoTexto;
        copia.version = version;
        copia.activa = activa;
        copia.creadoEn = creadoEn;
        copia.actualizadoEn = actualizadoEn;
        return copia;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmpresaRuc() {
        return empresaRuc;
    }

    public void setEmpresaRuc(String empresaRuc) {
        this.empresaRuc = empresaRuc;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpoHtml() {
        return cuerpoHtml;
    }

    public void setCuerpoHtml(String cuerpoHtml) {
        this.cuerpoHtml = cuerpoHtml;
    }

    public String getCuerpoTexto() {
        return cuerpoTexto;
    }

    public void setCuerpoTexto(String cuerpoTexto) {
        this.cuerpoTexto = cuerpoTexto;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
