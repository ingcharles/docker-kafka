package com.facturacion.firma.domain.repository;

import java.time.Instant;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.CertificadoDigital;

public interface CertificadoDigitalRepository {

    Uni<CertificadoDigital> persist(CertificadoDigital certificado);

    /** Marca como REVOCADO cualquier certificado ACTIVO existente de la empresa — solo puede haber uno vigente a la vez. */
    Uni<Integer> revocarActivosDelEmpresa(UUID empresaId);

    Uni<CertificadoDigital> findActivoByEmpresaId(UUID empresaId);

    /** El certificado más reciente de la empresa, en cualquier estado: sirve para explicar por qué no hay uno activo. */
    Uni<CertificadoDigital> findUltimoByEmpresaId(UUID empresaId);

    /** Pasa a EXPIRADO todo certificado ACTIVO (de cualquier empresa) cuya fecha de expiración ya pasó. Devuelve cuántos. */
    Uni<Integer> marcarExpiradosHasta(Instant ahora);

    Uni<CertificadoDigital> findById(UUID id);
}
