package com.facturacion.firma.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.ConfiguracionEmpresa;

public interface ConfiguracionEmpresaRepository {

    /** Inserta o actualiza el valor de esa clave para esa empresa (upsert por el índice único empresa_id+clave). */
    Uni<ConfiguracionEmpresa> upsert(UUID empresaId, String clave, String valor);

    Uni<List<ConfiguracionEmpresa>> findByEmpresaId(UUID empresaId);

    Uni<ConfiguracionEmpresa> findByEmpresaIdAndClave(UUID empresaId, String clave);

    Uni<Boolean> deleteByEmpresaIdAndClave(UUID empresaId, String clave);
}
