package com.facturacion.commons.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint que, además de una api-key, acepta un token de SESIÓN de identidad-service (con el
 * {@code ruc}/{@code rol}/{@code establecimiento}/{@code puntoEmision} del principal en {@code null}: una sesión no los
 * lleva). Hoy solo lo usa {@code EmpresaResource.crear} de firma-electronica-service, porque la empresa aún no existe
 * y por tanto no puede haber una api-key para su RUC. Cualquier otro endpoint sigue exigiendo api-key.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PermiteSesion {
}
