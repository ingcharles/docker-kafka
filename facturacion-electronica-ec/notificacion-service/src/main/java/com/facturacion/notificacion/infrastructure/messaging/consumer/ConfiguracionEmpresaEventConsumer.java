package com.facturacion.notificacion.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.messaging.LogFallosMensajeria;
import com.facturacion.notificacion.application.event.ConfiguracionEmpresaEventMessage;
import com.facturacion.notificacion.domain.repository.ConfiguracionEmpresaLocalRepository;

/**
 * Mantiene al día la vista materializada {@code configuracion_empresa_local}
 * — un {@code valor} en {@code null} (evento {@code ConfiguracionEmpresaEliminada})
 * borra la fila, revirtiendo al comportamiento por defecto.
 */
@ApplicationScoped
public class ConfiguracionEmpresaEventConsumer {

    private final ConfiguracionEmpresaLocalRepository repository;

    @Inject
    public ConfiguracionEmpresaEventConsumer(ConfiguracionEmpresaLocalRepository repository) {
        this.repository = repository;
    }

    @LogFallosMensajeria
    @Incoming("configuracion-empresa-events-in")
    @WithTransaction
    public Uni<Void> procesarEvento(ConfiguracionEmpresaEventMessage evento) {
        return repository.upsertOEliminar(evento.ruc(), evento.clave(), evento.valor(), evento.occurredAt());
    }
}
