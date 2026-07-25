package com.facturacion.notificacion.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.domain.model.entity.NotificacionEnviada;

public interface NotificacionEnviadaRepository {

    Uni<NotificacionEnviada> persist(NotificacionEnviada notificacion);

    Uni<NotificacionEnviada> findByComprobanteId(UUID comprobanteId);
}
