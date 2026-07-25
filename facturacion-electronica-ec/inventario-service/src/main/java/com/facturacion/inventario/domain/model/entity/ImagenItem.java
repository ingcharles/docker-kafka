package com.facturacion.inventario.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Imagen de un producto o servicio. El contenido se guarda directamente en
 * Postgres ({@code bytea}) — ver la nota de arquitectura en
 * {@code infrastructure.rest.ImagenResource} sobre por qué se eligió esto
 * en vez de un object storage (MinIO/S3) para esta versión, y cómo migrar
 * si el catálogo crece.
 */
public class ImagenItem {

    private UUID id;
    private UUID itemId;
    private String nombreArchivo;
    private String tipoMime;
    private long tamanoBytes;
    private byte[] contenido;
    private boolean esPrincipal;
    private int orden;
    private Instant creadoEn;

    public ImagenItem() {
    }

    public ImagenItem(UUID id, UUID itemId, String nombreArchivo, String tipoMime, long tamanoBytes,
            byte[] contenido, boolean esPrincipal, int orden, Instant creadoEn) {
        this.id = id;
        this.itemId = itemId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.contenido = contenido;
        this.esPrincipal = esPrincipal;
        this.orden = orden;
        this.creadoEn = creadoEn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    public boolean isEsPrincipal() {
        return esPrincipal;
    }

    public void setEsPrincipal(boolean esPrincipal) {
        this.esPrincipal = esPrincipal;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
