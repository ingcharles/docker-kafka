package com.facturacion.firma.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.CertificadoDigital;
import com.facturacion.firma.domain.model.enums.EstadoCertificado;
import com.facturacion.firma.domain.repository.CertificadoDigitalRepository;
import com.facturacion.firma.infrastructure.persistence.mapper.CertificadoDigitalMapper;

@ApplicationScoped
public class CertificadoDigitalRepositoryImpl implements CertificadoDigitalRepository {

    private final CertificadoDigitalPanacheRepository repository;
    private final CertificadoDigitalMapper mapper;

    @Inject
    public CertificadoDigitalRepositoryImpl(CertificadoDigitalPanacheRepository repository,
            CertificadoDigitalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<CertificadoDigital> persist(CertificadoDigital domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Integer> revocarActivosDelEmpresa(UUID empresaId) {
        return repository.update("estado = ?1 where empresaId = ?2 and estado = ?3",
            EstadoCertificado.REVOCADO.name(), empresaId, EstadoCertificado.ACTIVO.name());
    }

    @Override
    public Uni<CertificadoDigital> findActivoByEmpresaId(UUID empresaId) {
        return repository.find("empresaId = ?1 and estado = ?2", empresaId, EstadoCertificado.ACTIVO.name())
            .firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<CertificadoDigital> findUltimoByEmpresaId(UUID empresaId) {
        return repository.find("empresaId = ?1", Sort.descending("creadoEn"), empresaId)
            .firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Integer> marcarExpiradosHasta(Instant ahora) {
        return repository.update("estado = ?1 where estado = ?2 and fechaExpiracionCertificado < ?3",
            EstadoCertificado.EXPIRADO.name(), EstadoCertificado.ACTIVO.name(), ahora);
    }

    @Override
    public Uni<CertificadoDigital> findById(UUID id) {
        return repository.<com.facturacion.firma.infrastructure.persistence.entity.CertificadoDigitalEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
