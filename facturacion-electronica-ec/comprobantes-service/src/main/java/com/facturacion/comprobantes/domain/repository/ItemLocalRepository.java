package com.facturacion.comprobantes.domain.repository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.ItemLocal;

public interface ItemLocalRepository {

    /** Upsert por id — se llama al recibir ItemCreado/ItemActualizado desde Kafka. */
    Uni<ItemLocal> upsert(ItemLocal item);

    /**
     * Trae varios de una vez, para no hacer N consultas al armar los
     * detalles de un comprobante — solo devuelve ítems que pertenezcan al
     * {@code ruc} indicado (el catálogo es multi-tenant, un emisor nunca
     * puede facturar con el ítem de otro).
     */
    Uni<Map<UUID, ItemLocal>> findByIdsAndRuc(Set<UUID> ids, String ruc);
}
