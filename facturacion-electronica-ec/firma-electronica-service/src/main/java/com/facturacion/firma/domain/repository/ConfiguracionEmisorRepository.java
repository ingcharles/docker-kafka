package com.facturacion.firma.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.ConfiguracionEmisor;

public interface ConfiguracionEmisorRepository {

    /** Inserta o actualiza el valor de esa clave para ese emisor (upsert por el índice único emisor_id+clave). */
    Uni<ConfiguracionEmisor> upsert(UUID emisorId, String clave, String valor);

    Uni<List<ConfiguracionEmisor>> findByEmisorId(UUID emisorId);

    Uni<ConfiguracionEmisor> findByEmisorIdAndClave(UUID emisorId, String clave);

    Uni<Boolean> deleteByEmisorIdAndClave(UUID emisorId, String clave);
}
