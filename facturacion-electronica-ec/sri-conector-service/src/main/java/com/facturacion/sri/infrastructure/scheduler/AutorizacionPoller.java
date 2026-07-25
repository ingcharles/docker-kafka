package com.facturacion.sri.infrastructure.scheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import com.facturacion.sri.application.service.SriConectorService;

/**
 * Consulta Autorización para todo lo que sigue en RECIBIDO — con backoff
 * (no cada segundo): el propio SRI documenta hasta 24h de SLA de
 * procesamiento, así que consultar más seguido solo generaría carga sin
 * beneficio. Contraste deliberado con el envío inicial a Recepción (ver
 * SriConectorService.procesarComando), que sí es inmediato — eso es lo que
 * exige la Resolución NAC-DGERCGC25-00000017, esto no.
 */
@ApplicationScoped
public class AutorizacionPoller {

    private static final Logger LOG = Logger.getLogger(AutorizacionPoller.class);
    private static final int BATCH_SIZE = 20;

    @Inject
    SriConectorService sriConectorService;

    @ConfigProperty(name = "sri.autorizacion.poll.max-intentos")
    int maxIntentos;

    @Scheduled(every = "{sri.autorizacion.poll.intervalo}", delayed = "10s",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> consultarPendientes() {
        return sriConectorService.listarPendientesDeAutorizacion(maxIntentos, BATCH_SIZE)
            .onItem().transformToMulti(batch -> Multi.createFrom().iterable(batch))
            .onItem().transformToUniAndConcatenate(envio -> sriConectorService.consultarAutorizacion(envio)
                .onFailure().invoke(failure -> LOG.errorf(failure,
                    "Falló la consulta de autorización para %s, se reintenta en el siguiente ciclo",
                    envio.getClaveAcceso()))
                .onFailure().recoverWithItem(() -> null))
            .collect().last()
            .replaceWithVoid();
    }
}
