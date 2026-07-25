package com.facturacion.firma.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.CertificadoDigital;

public interface CertificadoDigitalRepository {

    Uni<CertificadoDigital> persist(CertificadoDigital certificado);

    /** Marca como REVOCADO cualquier certificado ACTIVO existente del emisor — solo puede haber uno vigente a la vez. */
    Uni<Integer> revocarActivosDelEmisor(UUID emisorId);

    Uni<CertificadoDigital> findActivoByEmisorId(UUID emisorId);

    Uni<CertificadoDigital> findById(UUID id);
}
