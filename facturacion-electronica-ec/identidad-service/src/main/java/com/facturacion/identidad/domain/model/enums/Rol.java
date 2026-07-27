package com.facturacion.identidad.domain.model.enums;

/** Rol de un usuario dentro de una empresa (ruc) específica — un mismo usuario puede tener roles distintos en distintas empresas. */
public enum Rol {

    /** Puede todo: emitir comprobantes, asociar otros usuarios, crear/revocar api-keys. */
    ADMIN,

    /** Puede emitir comprobantes (vía api-key) pero no gestionar usuarios ni otras api-keys de la empresa. */
    OPERADOR,

    /** Solo consulta — no puede emitir comprobantes. */
    LECTURA
}
