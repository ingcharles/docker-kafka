package com.facturacion.sri.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.facturacion.sri.domain.model.entity.EnvioSri;
import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;
import com.facturacion.sri.domain.repository.EnvioSriRepository;
import com.facturacion.sri.infrastructure.persistence.entity.EnvioSriEntity;
import com.facturacion.sri.infrastructure.persistence.mapper.EnvioSriMapper;

@ApplicationScoped
public class EnvioSriRepositoryImpl implements EnvioSriRepository {

    private final EnvioSriPanacheRepository repository;
    private final EnvioSriMapper mapper;

    @Inject
    public EnvioSriRepositoryImpl(EnvioSriPanacheRepository repository, EnvioSriMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<EnvioSri> persist(EnvioSri envio) {
        EnvioSriEntity entity = mapper.toEntity(envio);
        return repository.persist(entity).map(mapper::toDomain);
    }

    @Override
    public Uni<EnvioSri> update(EnvioSri envio) {
        return repository.<EnvioSriEntity>findById(envio.getId())
            .onItem().ifNull().failWith(() -> new IllegalStateException("EnvioSri no encontrado: " + envio.getId()))
            .onItem().transform(existente -> {
                existente.estado = mapper.map(envio.getEstado());
                existente.mensajesRecepcion = envio.getMensajesRecepcion();
                existente.intentosAutorizacion = envio.getIntentosAutorizacion();
                existente.ultimaConsultaEn = envio.getUltimaConsultaEn();
                existente.xmlAutorizado = envio.getXmlAutorizado();
                existente.numeroAutorizacion = envio.getNumeroAutorizacion();
                existente.fechaAutorizacion = envio.getFechaAutorizacion();
                existente.motivoRechazo = envio.getMotivoRechazo();
                existente.actualizadoEn = Instant.now();
                return mapper.toDomain(existente);
            });
    }

    @Override
    public Uni<EnvioSri> findById(UUID id) {
        return repository.<EnvioSriEntity>findById(id).map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<EnvioSri> findByComprobanteId(UUID comprobanteId) {
        return repository.find("comprobanteId", comprobanteId).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<EnvioSri> findByClaveAcceso(String claveAcceso) {
        return repository.find("claveAcceso", claveAcceso).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<List<EnvioSri>> findPendientesDeAutorizacion(EstadoEnvioSri estado, int batchSize) {
        return repository.find("estado", mapper.map(estado))
            .page(Page.ofSize(batchSize))
            .list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }
}
