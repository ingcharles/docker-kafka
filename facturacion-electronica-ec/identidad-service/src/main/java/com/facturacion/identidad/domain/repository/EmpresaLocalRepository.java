package com.facturacion.identidad.domain.repository;

import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.domain.model.entity.EmpresaLocal;

public interface EmpresaLocalRepository {

    /** Upsert por id (empresaId) — se llama al recibir EmpresaRegistrada/EmpresaActualizada desde Kafka. */
    Uni<EmpresaLocal> upsert(EmpresaLocal empresa);

    /** Null si no existe ninguna copia local para ese ruc todavía. */
    Uni<EmpresaLocal> findByRuc(String ruc);
}
