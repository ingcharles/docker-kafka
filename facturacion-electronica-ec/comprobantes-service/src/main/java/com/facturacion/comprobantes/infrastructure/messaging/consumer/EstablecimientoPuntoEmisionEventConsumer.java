package com.facturacion.comprobantes.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.messaging.LogFallosMensajeria;
import com.facturacion.comprobantes.application.event.EstablecimientoPuntoEmisionEventMessage;
import com.facturacion.comprobantes.domain.model.entity.EstablecimientoLocal;
import com.facturacion.comprobantes.domain.model.entity.PuntoEmisionLocal;
import com.facturacion.comprobantes.domain.repository.EstablecimientoLocalRepository;
import com.facturacion.comprobantes.domain.repository.PuntoEmisionLocalRepository;

/**
 * Mantiene al día las vistas materializadas {@code establecimiento_local}/
 * {@code punto_emision_local} — un solo canal, ramificado por
 * {@code tipoEvento} (mismo criterio que {@code ComprobanteEventConsumer} en
 * facturacion-orchestrator-service para un tópico con más de una forma).
 */
@ApplicationScoped
public class EstablecimientoPuntoEmisionEventConsumer {

    private final EstablecimientoLocalRepository establecimientoRepository;
    private final PuntoEmisionLocalRepository puntoEmisionRepository;

    @Inject
    public EstablecimientoPuntoEmisionEventConsumer(EstablecimientoLocalRepository establecimientoRepository,
            PuntoEmisionLocalRepository puntoEmisionRepository) {
        this.establecimientoRepository = establecimientoRepository;
        this.puntoEmisionRepository = puntoEmisionRepository;
    }

    @LogFallosMensajeria
    @Incoming("establecimiento-punto-emision-events-in")
    @WithTransaction
    public Uni<Void> procesarEvento(EstablecimientoPuntoEmisionEventMessage evento) {
        if (evento.tipoEvento().startsWith("Establecimiento")) {
            EstablecimientoLocal establecimiento = new EstablecimientoLocal();
            establecimiento.setRuc(evento.ruc());
            establecimiento.setCodigo(evento.establecimientoCodigo());
            establecimiento.setActivo(evento.activo());
            establecimiento.setActualizadoEn(evento.occurredAt());
            return establecimientoRepository.upsert(establecimiento).replaceWithVoid();
        }
        PuntoEmisionLocal puntoEmision = new PuntoEmisionLocal();
        puntoEmision.setRuc(evento.ruc());
        puntoEmision.setEstablecimientoCodigo(evento.establecimientoCodigo());
        puntoEmision.setCodigo(evento.puntoEmisionCodigo());
        puntoEmision.setActivo(evento.activo());
        puntoEmision.setActualizadoEn(evento.occurredAt());
        return puntoEmisionRepository.upsert(puntoEmision).replaceWithVoid();
    }
}
