package com.facturacion.identidad.application.dto;

/** Token de sesión devuelto por /usuarios/login — solo sirve para hablar con este servicio. */
public class TokenResponse {

    private String token;

    public TokenResponse() {
    }

    public TokenResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
