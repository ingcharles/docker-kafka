package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.event.ItemEventMessage;
import com.facturacion.comprobantes.domain.model.entity.ItemLocal;
import com.facturacion.comprobantes.domain.repository.ItemLocalRepository;

/**
 * Mantiene al día la vista materializada {@code item_local} — mismo
 * criterio que {@code EmpresaEventConsumer}: sin inbox de idempotencia
 * porque un upsert por id ya es naturalmente idempotente.
 */
@ApplicationScoped
public class ItemEventConsumer {

    private final ItemLocalRepository repository;

    @Inject
    public ItemEventConsumer(ItemLocalRepository repository) {
        this.repository = repository;
    }

    @Incoming("item-events-in")
    @WithTransaction
    public Uni<Void> procesarEvento(ItemEventMessage evento) {
        ItemLocal item = new ItemLocal();
        item.setId(evento.itemId());
        item.setRuc(evento.ruc());
        item.setCodigoPrincipal(evento.codigoPrincipal());
        item.setDescripcion(evento.descripcion());
        item.setCodigoImpuesto(evento.codigoImpuesto());
        item.setCodigoPorcentaje(evento.codigoPorcentaje());
        item.setTieneIce(evento.tieneIce());
        item.setCodigoPorcentajeIce(evento.codigoPorcentajeIce());
        item.setTarifaIce(evento.tarifaIce());
        item.setActivo(evento.activo());
        item.setActualizadoEn(evento.occurredAt());
        return repository.upsert(item).replaceWithVoid();
    }
}
