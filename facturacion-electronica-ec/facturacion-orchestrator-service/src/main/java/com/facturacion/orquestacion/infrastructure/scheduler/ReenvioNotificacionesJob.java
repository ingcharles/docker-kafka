package com.facturacion.orquestacion.infrastructure.scheduler;

import java.time.Duration;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.orquestacion.application.service.SagaOrchestratorService;
import com.facturacion.orquestacion.domain.service.PoliticaReenvioNotificacion;
import com.facturacion.orquestacion.domain.service.PoliticaReenvioNotificacion.Accion;
import com.facturacion.orquestacion.shared.error.CodigoError;

/**
 * Reenvía solo los correos fallidos: las sagas en NOTIFICACION_FALLIDA se devuelven a AUTORIZADO y se le pide a
 * comprobantes-service que vuelva a publicar el comprobante, hasta {@code max-intentos} veces por saga y dejando
 * pasar {@code espera-minima} entre una y otra. Una saga AUTORIZADO que espera el resultado del envío y no lo recibe en
 * {@code plazo-respuesta} vuelve a NOTIFICACION_FALLIDA, así el flujo siempre termina en NOTIFICADO o queda visible
 * como NOTIFICACION_FALLIDA con los intentos agotados (ver {@link PoliticaReenvioNotificacion}).
 * <p>
 * Es independiente del reenvío manual {@code POST /sagas/reenviar-notificaciones}, que sigue disponible y sin tope:
 * esta bandera solo gobierna el job. Apagado por defecto: se activa con {@code saga.reenvio-notificaciones.habilitado}.
 * <p>
 * <b>Cuida los recursos así:</b> una ejecución a la vez ({@code SKIP}), un lote acotado por ciclo, las sagas de a una
 * (nunca en paralelo), cada una en su propia transacción corta y la selección en una sesión de solo lectura que se
 * cierra antes de escribir. Una saga cuyo reenvío falla no arrastra a las demás.
 */
@ApplicationScoped
public class ReenvioNotificacionesJob {

    private static final Logger LOG = Logger.getLogger(ReenvioNotificacionesJob.class);

    private final SagaOrchestratorService servicio;
    private final boolean habilitado;
    private final int lote;
    private final PoliticaReenvioNotificacion.Plazos plazos;

    @Inject
    public ReenvioNotificacionesJob(SagaOrchestratorService servicio,
            @ConfigProperty(name = "saga.reenvio-notificaciones.habilitado", defaultValue = "false") boolean habilitado,
            @ConfigProperty(name = "saga.reenvio-notificaciones.lote", defaultValue = "20") int lote,
            @ConfigProperty(name = "saga.reenvio-notificaciones.espera-minima", defaultValue = "PT15M") Duration esperaMinima,
            @ConfigProperty(name = "saga.reenvio-notificaciones.plazo-respuesta", defaultValue = "PT10M") Duration plazoRespuesta,
            @ConfigProperty(name = "saga.reenvio-notificaciones.max-intentos", defaultValue = "3") int maxIntentos) {
        this.servicio = servicio;
        this.habilitado = habilitado;
        this.lote = lote;
        this.plazos = new PoliticaReenvioNotificacion.Plazos(esperaMinima, plazoRespuesta, maxIntentos);
    }

    @Scheduled(every = "${saga.reenvio-notificaciones.intervalo:5m}", delayed = "45s",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    Uni<Void> revisar() {
        if (!habilitado) {
            return Uni.createFrom().voidItem();
        }
        return servicio.idsParaReenvioAutomatico(plazos, lote)
            .onItem().transformToMulti(ids -> Multi.createFrom().iterable(ids))
            .onItem().transformToUniAndConcatenate(this::atender)
            .collect().asList()
            .invoke(acciones -> {
                long reenviados = acciones.stream().filter(a -> a == Accion.REENVIAR).count();
                long sinRespuesta = acciones.stream().filter(a -> a == Accion.DAR_POR_SIN_RESPUESTA).count();
                if (reenviados + sinRespuesta > 0) {
                    LogCodificado.info(LOG, CodigoError.REENVIO_NOTIFICACIONES_REVISADO, null, reenviados, sinRespuesta);
                }
            })
            .onFailure().invoke(error -> LogCodificado.error(LOG, error,
                CodigoError.REENVIO_NOTIFICACIONES_REVISION_FALLIDA, null))
            .onFailure().recoverWithNull()
            .replaceWithVoid();
    }

    /** Una saga, en su propia transacción; si falla, se registra y se sigue con la siguiente. */
    private Uni<Accion> atender(UUID comprobanteId) {
        return servicio.reenviarNotificacionAutomaticamente(comprobanteId, plazos)
            .onFailure().recoverWithUni(error -> {
                LogCodificado.error(LOG, error, CodigoError.REENVIO_AUTOMATICO_FALLIDO, "comprobanteId=" + comprobanteId);
                return Uni.createFrom().item(Accion.ESPERAR);
            });
    }
}
