package com.facturacion.firma.application.dto;

/** Lo que devuelve {@code POST /empresas/{id}/logo}: el tipo detectado y el tamaño guardado (nunca los bytes). */
public class LogoResponse {

    private String tipo;
    private int tamanoBytes;

    public LogoResponse() {
    }

    public LogoResponse(String tipo, int tamanoBytes) {
        this.tipo = tipo;
        this.tamanoBytes = tamanoBytes;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(int tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }
}
