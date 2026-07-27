package com.facturacion.identidad.infrastructure.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/** Marca un endpoint que requiere el token de sesión (login) en el header Authorization — ver SesionAuthFilter. */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Autenticado {
}
