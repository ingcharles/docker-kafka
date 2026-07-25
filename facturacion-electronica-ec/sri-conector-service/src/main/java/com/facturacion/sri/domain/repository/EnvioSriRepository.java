package com.facturacion.sri.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.sri.domain.model.entity.EnvioSri;
import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;

public interface EnvioSriRepository {

    Uni<EnvioSri> persist(EnvioSri envio);

    Uni<EnvioSri> update(EnvioSri envio);

    Uni<EnvioSri> findById(UUID id);

    Uni<EnvioSri> findByComprobanteId(UUID comprobanteId);

    Uni<EnvioSri> findByClaveAcceso(String claveAcceso);

    /** Para el poller de autorización: todos los envíos en estado RECIBIDO listos para volver a consultarse. */
    Uni<List<EnvioSri>> findPendientesDeAutorizacion(EstadoEnvioSri estado, int maxIntentos, int batchSize);
}
