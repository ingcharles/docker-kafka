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
import com.facturacion.orquestacion.domain.service.PoliticaReintentoSaga;
import com.facturacion.orquestacion.shared.error.CodigoError;

/**
 * Detecta sagas atascadas esperando a otro servicio y las recupera (ver {@link PoliticaReintentoSaga}). Antes la
 * única recuperación era manual ({@code POST /sagas/{id}/retry}): si firma-electronica-service o el SRI nunca
 * respondían, la saga quedaba en CREADO/FIRMADO para siempre sin que nadie lo notara. Ahora se reintenta sola
 * unas veces y, si no hay respuesta, pasa a EXPIRADA (visible con {@code GET /sagas?state=EXPIRADA}).
 * Se puede apagar con {@code saga.watchdog.habilitado=false}.
 * <p>
 * <b>Cada saga se procesa en su propia transacción.</b> Una saga cuyo reintento falla (p. ej. un dato corrupto) no
 * arrastra a las demás: su fallo se registra, se cuenta como un intento (para que termine expirando en vez de
 * fallar eternamente) y el watchdog sigue con la siguiente.
 */
@ApplicationScoped
public class SagaWatchdog {

    private static final Logger LOG = Logger.getLogger(SagaWatchdog.class);
    private static final int LOTE = 100;
    private static final int MAX_CAUSA = 300;

    private final SagaOrchestratorService servicio;
    private final boolean habilitado;
    private final PoliticaReintentoSaga.Plazos plazos;

    @Inject
    public SagaWatchdog(SagaOrchestratorService servicio,
            @ConfigProperty(name = "saga.watchdog.habilitado", defaultValue = "true") boolean habilitado,
            @ConfigProperty(name = "saga.watchdog.plazo-firma", defaultValue = "PT2M") Duration plazoFirma,
            @ConfigProperty(name = "saga.watchdog.plazo-sri", defaultValue = "PT15M") Duration plazoSri,
            @ConfigProperty(name = "saga.watchdog.max-reintentos", defaultValue = "3") int maxReintentos) {
        this.servicio = servicio;
        this.habilitado = habilitado;
        this.plazos = new PoliticaReintentoSaga.Plazos(plazoFirma, plazoSri, maxReintentos);
    }

    @Scheduled(every = "${saga.watchdog.intervalo:30s}", delayed = "30s",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    Uni<Void> revisar() {
        if (!habilitado) {
            return Uni.createFrom().voidItem();
        }
        return servicio.listarIdsParaRecuperar(plazos, LOTE)
            .onItem().transformToMulti(ids -> Multi.createFrom().iterable(ids))
            .onItem().transformToUniAndConcatenate(this::recuperar)
            .collect().asList()
            .invoke(resultados -> {
                long hechas = resultados.stream().filter(Boolean::booleanValue).count();
                if (hechas > 0) {
                    LogCodificado.info(LOG, CodigoError.WATCHDOG_SAGAS_RECUPERADAS, null, hechas);
                }
            })
            .onFailure().invoke(error -> LogCodificado.error(LOG, error, CodigoError.WATCHDOG_REVISION_FALLIDA, null))
            .onFailure().recoverWithNull()
            .replaceWithVoid();
    }

    /** Una saga, en su propia transacción; si falla, se cuenta el intento (en otra transacción) y se sigue. */
    private Uni<Boolean> recuperar(UUID comprobanteId) {
        return servicio.recuperarSaga(comprobanteId, plazos)
            .onFailure().recoverWithUni(error -> {
                LogCodificado.error(LOG, error, CodigoError.REINTENTO_AUTOMATICO_FALLIDO, "comprobanteId=" + comprobanteId);
                return servicio.contarReintentoFallido(comprobanteId, plazos, resumir(error))
                    .onFailure().invoke(e -> LogCodificado.error(LOG, e, CodigoError.REINTENTO_NO_REGISTRADO, "comprobanteId=" + comprobanteId))
                    .onFailure().recoverWithNull()
                    .replaceWith(false);
            });
    }

    private static String resumir(Throwable error) {
        String causa = error.getClass().getSimpleName() + ": " + error.getMessage();
        return causa.length() > MAX_CAUSA ? causa.substring(0, MAX_CAUSA) : causa;
    }
}
