package com.facturacion.comprobantes.domain.repository;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.ConfiguracionEmpresaLocal;

public interface ConfiguracionEmpresaLocalRepository {

    /** Upsert por (ruc, clave) — un {@code valor} null (evento de eliminación) borra la fila. */
    Uni<Void> insertarActualizarOEliminar(String ruc, String clave, String valor, java.time.Instant actualizadoEn);

    Uni<ConfiguracionEmpresaLocal> findByRucAndClave(String ruc, String clave);
}
