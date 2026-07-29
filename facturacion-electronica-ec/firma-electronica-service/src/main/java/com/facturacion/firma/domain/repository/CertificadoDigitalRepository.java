package com.facturacion.firma.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.CertificadoDigital;

public interface CertificadoDigitalRepository {

    Uni<CertificadoDigital> persist(CertificadoDigital certificado);

    /** Marca como REVOCADO cualquier certificado ACTIVO existente de la empresa — solo puede haber uno vigente a la vez. */
    Uni<Integer> revocarActivosDelEmpresa(UUID empresaId);

    Uni<CertificadoDigital> findActivoByEmpresaId(UUID empresaId);

    Uni<CertificadoDigital> findById(UUID id);
}
