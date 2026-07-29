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

    /**
     * Para el poller de autorización: todos los envíos en estado RECIBIDO listos
     * para volver a consultarse. No filtra por intentos aquí — el máximo puede
     * variar por empresa (ver {@code ConfiguracionEmpresaLocal}), así que esa
     * decisión se toma fila por fila en {@code SriConectorService.aplicarResultadoAutorizacion},
     * no en esta consulta; una vez que una fila se agota pasa a ERROR_CONSULTA
     * y deja de calificar por el filtro de {@code estado}.
     */
    Uni<List<EnvioSri>> findPendientesDeAutorizacion(EstadoEnvioSri estado, int batchSize);
}
