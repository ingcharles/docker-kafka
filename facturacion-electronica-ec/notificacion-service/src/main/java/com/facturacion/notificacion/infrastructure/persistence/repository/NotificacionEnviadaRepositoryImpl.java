package com.facturacion.notificacion.infrastructure.persistence.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.domain.model.entity.NotificacionEnviada;
import com.facturacion.notificacion.domain.repository.NotificacionEnviadaRepository;
import com.facturacion.notificacion.infrastructure.persistence.mapper.NotificacionEnviadaMapper;

@ApplicationScoped
public class NotificacionEnviadaRepositoryImpl implements NotificacionEnviadaRepository {

    private final NotificacionEnviadaPanacheRepository repository;
    private final NotificacionEnviadaMapper mapper;

    @Inject
    public NotificacionEnviadaRepositoryImpl(NotificacionEnviadaPanacheRepository repository,
            NotificacionEnviadaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<NotificacionEnviada> persist(NotificacionEnviada domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<NotificacionEnviada> actualizar(NotificacionEnviada domain) {
        return repository.findById(domain.getId()).map(entity -> {
            entity.destinatario = domain.getDestinatario();
            entity.estado = domain.getEstado().name();
            entity.motivoError = domain.getMotivoError();
            entity.enviadoEn = domain.getEnviadoEn();
            return mapper.toDomain(entity);
        });
    }

    @Override
    public Uni<NotificacionEnviada> findByComprobanteId(UUID comprobanteId) {
        return repository.find("comprobanteId", comprobanteId).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
