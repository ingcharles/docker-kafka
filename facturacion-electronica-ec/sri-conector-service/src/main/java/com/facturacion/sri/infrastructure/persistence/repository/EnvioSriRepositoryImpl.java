package com.facturacion.sri.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.sri.domain.model.entity.EnvioSri;
import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;
import com.facturacion.sri.domain.repository.EnvioSriRepository;
import com.facturacion.sri.infrastructure.persistence.entity.EnvioSriEntity;
import com.facturacion.sri.infrastructure.persistence.mapper.EnvioSriMapper;
import com.facturacion.sri.shared.error.SriError;

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
            .onItem().ifNull().failWith(() -> new NotFoundException(SriError.ENVIO_NO_ENCONTRADO_POR_ID, envio.getId()))
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
    public Uni<List<EnvioSri>> reclamarPendientesDeAutorizacion(int batchSize, Instant consultadosAntesDe, Instant ahora) {
        return repository.findRecibidosParaConsultar(mapper.map(EstadoEnvioSri.RECIBIDO), consultadosAntesDe, batchSize)
            .map(entities -> entities.stream().map(entity -> {
                // La reserva se guarda al confirmar la transacción: las demás instancias ya no lo ven como pendiente.
                entity.ultimaConsultaEn = ahora;
                return mapper.toDomain(entity);
            }).collect(Collectors.toList()));
    }

    @Override
    public Uni<EnvioSri> findByIdBloqueado(UUID id) {
        return repository.<EnvioSriEntity>findById(id, LockModeType.PESSIMISTIC_WRITE)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
