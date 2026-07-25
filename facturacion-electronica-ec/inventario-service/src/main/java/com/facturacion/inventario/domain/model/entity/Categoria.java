package com.facturacion.inventario.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.inventario.domain.model.enums.TipoCategoria;

public class Categoria {

    private UUID id;
    private String nombre;
    private TipoCategoria tipo;
    private String descripcion;
    private boolean activa;
    private Instant creadoEn;

    public Categoria() {
    }

    public Categoria(UUID id, String nombre, TipoCategoria tipo, String descripcion, boolean activa,
            Instant creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.activa = activa;
        this.creadoEn = creadoEn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoCategoria getTipo() {
        return tipo;
    }

    public void setTipo(TipoCategoria tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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
}
