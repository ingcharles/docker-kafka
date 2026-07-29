package com.facturacion.firma.infrastructure.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * Marca el único endpoint del stack que acepta, además de una api-key,
 * un token de sesión de identidad-service — {@code EmpresaResource.crear}.
 * Necesario para romper un ciclo cerrado: una api-key solo se emite para un
 * ruc al que el usuario ya esté asociado, y la asociación exige que el ruc
 * ya corresponda a una empresa real — para una empresa genuinamente nueva
 * eso nunca se cumple. Ver {@link ApiKeyAuthFilter}/{@link JwtVerificador}.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface PermiteSesion {
}
