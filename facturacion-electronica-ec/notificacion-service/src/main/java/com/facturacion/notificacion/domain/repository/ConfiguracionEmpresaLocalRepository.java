package com.facturacion.notificacion.domain.repository;

import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.domain.model.entity.ConfiguracionEmpresaLocal;

public interface ConfiguracionEmpresaLocalRepository {

    /** Upsert por (ruc, clave) — un {@code valor} null (evento de eliminación) borra la fila. */
    Uni<Void> upsertOEliminar(String ruc, String clave, String valor, java.time.Instant actualizadoEn);

    Uni<ConfiguracionEmpresaLocal> findByRucAndClave(String ruc, String clave);
}
