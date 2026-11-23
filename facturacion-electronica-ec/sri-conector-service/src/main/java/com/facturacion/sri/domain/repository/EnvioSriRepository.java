package com.facturacion.sri.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.sri.domain.model.entity.EnvioSri;

public interface EnvioSriRepository {

    Uni<EnvioSri> persist(EnvioSri envio);

    Uni<EnvioSri> update(EnvioSri envio);

    Uni<EnvioSri> findById(UUID id);

    Uni<EnvioSri> findByComprobanteId(UUID comprobanteId);

    Uni<EnvioSri> findByClaveAcceso(String claveAcceso);

    /**
     * Para el poller de autorización: toma hasta {@code batchSize} envíos en estado RECIBIDO que no se hayan
     * consultado desde {@code consultadosAntesDe} (los más antiguos primero) y los reserva con {@code ahora} como última
     * consulta. Con {@code FOR UPDATE SKIP LOCKED}: con 2 o más instancias cada una toma envíos distintos, y la reserva
     * evita que otra instancia los vuelva a tomar mientras esta espera al SRI. Exige transacción (el bloqueo dura hasta
     * el commit). No filtra por intentos aquí — el máximo puede variar por empresa (ver
     * {@code ConfiguracionEmpresaLocal}), así que esa decisión se toma fila por fila en
     * {@code SriConectorService.aplicarResultadoAutorizacion}; una vez que una fila se agota pasa a ERROR_CONSULTA y
     * deja de calificar por el filtro de estado.
     */
    Uni<List<EnvioSri>> reclamarPendientesDeAutorizacion(int batchSize, Instant consultadosAntesDe, Instant ahora);

    /**
     * El envío con bloqueo de escritura hasta el commit: quien aplica el resultado de una consulta lo toma así para
     * comprobar, sin carreras, que otra instancia no aplicó ya el suyo. Exige transacción.
     */
    Uni<EnvioSri> findByIdBloqueado(UUID id);
}
