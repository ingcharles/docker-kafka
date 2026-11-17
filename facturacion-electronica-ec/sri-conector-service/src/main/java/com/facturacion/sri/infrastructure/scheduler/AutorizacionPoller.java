package com.facturacion.sri.infrastructure.scheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.sri.application.service.SriConectorService;
import com.facturacion.sri.shared.error.CodigoLog;

/**
 * Consulta Autorización para todo lo que sigue en RECIBIDO — con backoff
 * (no cada segundo): el propio SRI documenta hasta 24h de SLA de
 * procesamiento, así que consultar más seguido solo generaría carga sin
 * beneficio. Contraste deliberado con el envío inicial a Recepción (ver
 * SriConectorService.procesarComando), que sí es inmediato — eso es lo que
 * exige la Resolución NAC-DGERCGC25-00000017, esto no.
 * <p>
 * No filtra por intentos acá — el máximo puede variar por empresa (ver
 * {@code SriConectorService.resolverMaxIntentos}), así que esa decisión se
 * toma fila por fila, no en esta consulta.
 */
@ApplicationScoped
public class AutorizacionPoller {

    private static final Logger LOG = Logger.getLogger(AutorizacionPoller.class);
    private static final int BATCH_SIZE = 20;

    @Inject
    SriConectorService sriConectorService;

    @Scheduled(every = "{sri.autorizacion.poll.intervalo}", delayed = "10s",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> consultarPendientes() {
        return sriConectorService.listarPendientesDeAutorizacion(BATCH_SIZE)
            .onItem().transformToMulti(batch -> Multi.createFrom().iterable(batch))
            .onItem().transformToUniAndConcatenate(envio -> sriConectorService.consultarAutorizacion(envio)
                .onFailure().invoke(failure -> LogCodificado.error(LOG, failure, CodigoLog.CONSULTA_AUTORIZACION_FALLIDA,
                    "claveAcceso=" + envio.getClaveAcceso()))
                .onFailure().recoverWithItem(() -> null))
            .collect().last()
            .replaceWithVoid();
    }
}
