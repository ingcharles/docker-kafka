package com.facturacion.identidad.domain.port;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.identidad.domain.model.enums.Rol;

/** Puerto de salida: emite los JWT del servicio (sesión de usuario y api-keys). La firma es de infraestructura. */
public interface EmisorTokens {

    String generarTokenSesion(UUID usuarioId, String email);

    String generarApiKey(UUID usuarioId, String ruc, Rol rol, String establecimiento, String puntoEmision, String jti,
            Instant expiracion);
}
