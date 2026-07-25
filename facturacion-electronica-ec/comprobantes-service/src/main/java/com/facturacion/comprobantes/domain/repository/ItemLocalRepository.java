package com.facturacion.comprobantes.domain.repository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.ItemLocal;

public interface ItemLocalRepository {

    /** Upsert por id — se llama al recibir ItemCreado/ItemActualizado desde Kafka. */
    Uni<ItemLocal> upsert(ItemLocal item);

    /** Trae varios de una vez, para no hacer N consultas al armar los detalles de un comprobante. */
    Uni<Map<UUID, ItemLocal>> findByIds(Set<UUID> ids);
}
