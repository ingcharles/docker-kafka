package com.facturacion.orquestacion.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.error.Mensajes;
import com.facturacion.orquestacion.application.event.ActualizarComprobanteCommand;
import com.facturacion.orquestacion.application.event.ComprobanteEventMessage;
import com.facturacion.orquestacion.application.event.ComprobanteFirmadoEventMessage;
import com.facturacion.orquestacion.application.event.EnviarComprobanteCommand;
import com.facturacion.orquestacion.application.event.FirmarComprobanteCommand;
import com.facturacion.orquestacion.application.event.NotificacionResultadoEventMessage;
import com.facturacion.orquestacion.application.event.SriResultadoEventMessage;
import com.facturacion.orquestacion.application.event.StockCommandMessage;
import com.facturacion.orquestacion.application.event.StockEventMessage;
import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.entity.SagaStep;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.port.EventoSaliente;
import com.facturacion.orquestacion.domain.port.EventosSalientes;
import com.facturacion.orquestacion.domain.port.MensajesProcesados;
import com.facturacion.orquestacion.domain.repository.SagaInstanceRepository;
import com.facturacion.orquestacion.domain.repository.SagaStepRepository;
import com.facturacion.orquestacion.domain.service.PoliticaReenvioNotificacion;
import com.facturacion.orquestacion.domain.service.PoliticaReintentoSaga;
import com.facturacion.orquestacion.shared.constant.MessagingConstants;
import com.facturacion.orquestacion.shared.error.CodigoError;

/**
 * La única pieza de todo el sistema que conoce la saga completa. Todos los
 * manejadores siguen la misma estructura:
 * revisar el inbox, aplicar la transición de estado (que no hace nada si la
 * saga no está en el estado de origen esperado — por ejemplo, un evento
 * duplicado después de un reintento de Kafka), emitir lo que corresponda a
 * continuación, y marcar el inbox.
 * <p>
 * Acá casi no hay compensación (solo la reserva de stock): firmar,
 * enviar al SRI y notificar no tienen efecto colateral que deshacer, y una
 * vez que el SRI autoriza, el comprobante es legalmente válido pase lo que
 * pase después (una notificación fallida no revierte nada — ver
 * {@link #procesarNotificacionFallida}).
 */
@ApplicationScoped
public class SagaOrchestratorService {

    private static final Logger LOG = Logger.getLogger(SagaOrchestratorService.class);

    /** Código de la nota que guarda el comando de firma mientras se espera la reserva de stock. */
    private static final String NOTA_FIRMA_PENDIENTE = "FIRMA_PENDIENTE";

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final EventosSalientes salientes;
    private final MensajesProcesados mensajesProcesados;
    private final ObjectMapper objectMapper;
    private final boolean descuentoInventarioHabilitado;

    @Inject
    public SagaOrchestratorService(SagaInstanceRepository sagaInstanceRepository,
            SagaStepRepository sagaStepRepository, EventosSalientes salientes,
            MensajesProcesados mensajesProcesados, ObjectMapper objectMapper,
            @ConfigProperty(name = "orquestacion.inventario.descuento.habilitado") boolean descuentoInventarioHabilitado) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaStepRepository = sagaStepRepository;
        this.salientes = salientes;
        this.mensajesProcesados = mensajesProcesados;
        this.objectMapper = objectMapper;
        this.descuentoInventarioHabilitado = descuentoInventarioHabilitado;
    }

    @WithTransaction
    public Uni<ResultadoEvento> procesarEventoComprobante(ComprobanteEventMessage event) {
        // ComprobanteAutorizado también viaja por este tópico, pero lo
        // consume directo notificacion-service — acá se ignora sin más.
        if (!MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO.equals(event.eventType())) {
            return Uni.createFrom().item(ResultadoEvento.IGNORADO_TIPO);
        }
        return ejecutarSiNoProcesado(event.eventId(), () -> {
            boolean notificacionEsperada = !Boolean.FALSE.equals(event.notificacionEsperada());
            FirmarComprobanteCommand firmar = new FirmarComprobanteCommand(UUID.randomUUID(), event.comprobanteId(),
                event.empresaRuc(), event.xmlGenerado(), Instant.now());

            if (debeDescontarInventario(event)) {
                return iniciarConReservaDeStock(event, notificacionEsperada, firmar);
            }
            SagaInstance saga = SagaInstance.iniciar(event.comprobanteId(), event.empresaRuc(), event.claveAcceso(),
                event.ambiente(), notificacionEsperada);
            return sagaInstanceRepository.persist(saga)
                .chain(() -> registrarRecibido(event.comprobanteId(), MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO))
                .chain(() -> emitir(event.comprobanteId(), "FirmarComprobante", MessagingConstants.FIRMA_COMMANDS_TOPIC,
                    firmar))
                .replaceWith(ResultadoEvento.APLICADO);
        });
    }

    // --- Inventario (solo con orquestacion.inventario.descuento.habilitado=true) -------------------------------

    /** Solo las facturas (ventas) con al menos una línea que apunte a un ítem de inventario descuentan stock. */
    private boolean debeDescontarInventario(ComprobanteEventMessage event) {
        return descuentoInventarioHabilitado
            && MessagingConstants.TIPO_COMPROBANTE_FACTURA.equals(event.tipoComprobante())
            && event.items() != null && !event.items().isEmpty();
    }

    /**
     * Empieza por RESERVAR el stock. El comando de firma ya armado queda guardado como nota de la saga y sale cuando
     * inventario confirma la reserva ({@link #procesarStockReservado}): así el XML no hace falta volver a pedirlo.
     */
    private Uni<ResultadoEvento> iniciarConReservaDeStock(ComprobanteEventMessage event, boolean notificacionEsperada,
            FirmarComprobanteCommand firmar) {
        SagaInstance saga = SagaInstance.iniciarConReservaDeStock(event.comprobanteId(), event.empresaRuc(),
            event.claveAcceso(), event.ambiente(), notificacionEsperada);
        StockCommandMessage reservar = new StockCommandMessage(UUID.randomUUID(),
            MessagingConstants.COMANDO_STOCK_RESERVAR, event.comprobanteId(), event.items(), Instant.now());
        return sagaInstanceRepository.persist(saga)
            .chain(() -> registrarRecibido(event.comprobanteId(), MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO))
            .chain(() -> sagaStepRepository.persist(SagaStep.nota(event.comprobanteId(), NOTA_FIRMA_PENDIENTE,
                serializarAJson(firmar))))
            .chain(() -> emitir(event.comprobanteId(), "ReservarStock", MessagingConstants.INVENTARIO_COMMANDS_TOPIC,
                reservar))
            .replaceWith(ResultadoEvento.APLICADO);
    }

    @WithTransaction
    public Uni<ResultadoEvento> procesarEventoStock(StockEventMessage event) {
        return ejecutarSiNoProcesado(event.eventId(), () -> registrarRecibido(event.comprobanteId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_STOCK_RESERVADO -> procesarStockReservado(event.comprobanteId());
                case MessagingConstants.EVENT_TYPE_STOCK_NO_DISPONIBLE ->
                    procesarStockNoDisponible(event.comprobanteId(), event.itemsInsuficientes());
                // StockConfirmado / StockLiberado: respuestas informativas, solo quedan en la bitácora.
                default -> Uni.createFrom().item(ResultadoEvento.IGNORADO_TIPO);
            }));
    }

    /** Stock reservado: ahora sí sale el comando de firma que se dejó guardado al crear la saga. */
    private Uni<ResultadoEvento> procesarStockReservado(UUID comprobanteId) {
        return sagaInstanceRepository.transicionarEstado(comprobanteId, SagaState.STOCK_RESERVADO, null,
                SagaState.STOCK_PENDIENTE, SagaState.EXPIRADA)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().item(ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO);
                }
                return sagaStepRepository.findUltimaNota(comprobanteId, NOTA_FIRMA_PENDIENTE)
                    .onItem().transformToUni(nota -> {
                        if (nota == null) {
                            return Uni.<ResultadoEvento>createFrom().failure(new IllegalStateException(
                                "La saga " + comprobanteId + " no tiene el comando de firma pendiente"));
                        }
                        return emitir(comprobanteId, "FirmarComprobante", MessagingConstants.FIRMA_COMMANDS_TOPIC,
                            leerJson(nota.getPayload(), FirmarComprobanteCommand.class))
                            .replaceWith(ResultadoEvento.APLICADO);
                    });
            });
    }

    /** Sin stock: inventario ya devolvió lo reservado de más, no hay nada que liberar. El comprobante queda terminal. */
    private Uni<ResultadoEvento> procesarStockNoDisponible(UUID comprobanteId, List<UUID> itemsInsuficientes) {
        String motivo = "Stock insuficiente en inventario para los ítems: "
            + (itemsInsuficientes == null ? "(sin detalle)" : itemsInsuficientes.stream().map(UUID::toString)
                .collect(Collectors.joining(", ")));
        return sagaInstanceRepository.transicionarEstado(comprobanteId, SagaState.STOCK_NO_DISPONIBLE, motivo,
                SagaState.STOCK_PENDIENTE, SagaState.EXPIRADA)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().item(ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO);
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    comprobanteId, MessagingConstants.COMANDO_MARCAR_STOCK_INSUFICIENTE, null, null, null, null,
                    motivo, Instant.now());
                return emitir(comprobanteId, MessagingConstants.COMANDO_MARCAR_STOCK_INSUFICIENTE,
                    MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, command)
                    .replaceWith(ResultadoEvento.APLICADO);
            });
    }

    /** CONFIRMAR (autorizado: escribe el kardex) o LIBERAR (fallido: devuelve la reserva); ambos idempotentes. */
    private Uni<Void> emitirComandoDeStock(SagaInstance saga, String tipoComando) {
        if (!saga.isUsaStock()) {
            return Uni.createFrom().voidItem();
        }
        StockCommandMessage command = new StockCommandMessage(UUID.randomUUID(), tipoComando, saga.getId(), List.of(),
            Instant.now());
        return emitir(saga.getId(), "Stock" + tipoComando, MessagingConstants.INVENTARIO_COMMANDS_TOPIC, command);
    }

    @WithTransaction
    public Uni<ResultadoEvento> procesarEventoFirma(ComprobanteFirmadoEventMessage event) {
        return ejecutarSiNoProcesado(event.eventId(), () -> registrarRecibido(event.comprobanteId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_COMPROBANTE_FIRMADO -> procesarComprobanteFirmado(event);
                case MessagingConstants.EVENT_TYPE_FIRMA_FALLIDA ->
                    procesarFirmaFallida(event.comprobanteId(), event.motivoError());
                default -> Uni.createFrom().item(ResultadoEvento.IGNORADO_TIPO);
            }));
    }

    @WithTransaction
    public Uni<ResultadoEvento> procesarEventoSri(SriResultadoEventMessage event) {
        return ejecutarSiNoProcesado(event.eventId(), () -> registrarRecibido(event.comprobanteId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_SRI_AUTORIZADO -> procesarSriAutorizado(event);
                case MessagingConstants.EVENT_TYPE_SRI_NO_AUTORIZADO ->
                    procesarSriRechazo(event, SagaState.NO_AUTORIZADO, MessagingConstants.COMANDO_MARCAR_NO_AUTORIZADO);
                case MessagingConstants.EVENT_TYPE_SRI_RECHAZADO_RECEPCION ->
                    procesarSriRechazo(event, SagaState.RECHAZADO_RECEPCION,
                        MessagingConstants.COMANDO_MARCAR_NO_AUTORIZADO);
                // SriRecibido: solo queda registrado en la bitácora, no dispara ninguna
                // transición — el poller de autorización sigue trabajando por su cuenta.
                default -> Uni.createFrom().item(ResultadoEvento.IGNORADO_TIPO);
            }));
    }

    @WithTransaction
    public Uni<ResultadoEvento> procesarEventoNotificacion(NotificacionResultadoEventMessage event) {
        return ejecutarSiNoProcesado(event.eventId(), () -> registrarRecibido(event.comprobanteId(), event.eventType())
            .chain(() -> switch (event.eventType()) {
                case MessagingConstants.EVENT_TYPE_NOTIFICACION_ENVIADA -> procesarNotificacionEnviada(event.comprobanteId());
                case MessagingConstants.EVENT_TYPE_NOTIFICACION_FALLIDA ->
                    procesarNotificacionFallida(event.comprobanteId(), event.motivoError());
                default -> Uni.createFrom().item(ResultadoEvento.IGNORADO_TIPO);
            }));
    }

    /**
     * Reenvío masivo de correos: las sagas de la empresa en NOTIFICACION_FALLIDA (hasta {@code limite}) vuelven a
     * AUTORIZADO (esperan la notificación, así una segunda llamada no las reenvía otra vez) y a cada una se le pide a
     * comprobantes-service que vuelva a publicar ComprobanteAutorizado. El resultado vuelve por el camino normal:
     * NotificacionEnviada la lleva a NOTIFICADO y NotificacionFallida de nuevo a NOTIFICACION_FALLIDA, con el motivo
     * nuevo.
     *
     * @return cuántas sagas se mandaron a reenviar
     */
    @WithTransaction
    public Uni<Integer> reenviarNotificacionesFallidas(String empresaRuc, int limite) {
        return sagaInstanceRepository.findPaged(0, limite, SagaState.NOTIFICACION_FALLIDA, empresaRuc)
            .onItem().transformToUni(pagina -> Multi.createFrom().iterable(pagina.items())
                .onItem().transformToUniAndConcatenate(saga -> solicitarReenvio(saga.getId()))
                .collect().with(Collectors.summingInt(solicitada -> solicitada ? 1 : 0)));
    }

    private Uni<Boolean> solicitarReenvio(UUID comprobanteId) {
        return sagaInstanceRepository.transicionarEstado(comprobanteId, SagaState.AUTORIZADO, null,
                SagaState.NOTIFICACION_FALLIDA)
            .onItem().transformToUni(saga -> saga == null
                ? Uni.createFrom().item(false)
                : emitirComandoDeReenvio(comprobanteId).replaceWith(true));
    }

    /** Registra (outbox) el comando que le pide a comprobantes-service volver a publicar el comprobante autorizado. */
    private Uni<Void> emitirComandoDeReenvio(UUID comprobanteId) {
        ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
            comprobanteId, MessagingConstants.COMANDO_REENVIAR_NOTIFICACION, null, null, null, null, null,
            Instant.now());
        return emitir(comprobanteId, MessagingConstants.COMANDO_REENVIAR_NOTIFICACION,
            MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, command);
    }

    /**
     * Ids de las sagas que el job de reenvío automático debe atender (ver {@link PoliticaReenvioNotificacion}), a lo
     * más {@code lote}. Solo lectura y en su propia sesión: se cierra antes de empezar a escribir, y cada saga se
     * procesa después en su PROPIA transacción ({@link #reenviarNotificacionAutomaticamente}), igual que el watchdog.
     */
    @WithSession
    public Uni<List<UUID>> idsParaReenvioAutomatico(PoliticaReenvioNotificacion.Plazos plazos, int lote) {
        Instant ahora = Instant.now();
        return sagaInstanceRepository.findPendientesDeNotificacion(ahora.minus(plazos.esperaMinima()),
                ahora.minus(plazos.plazoRespuesta()), plazos.maxIntentos(), lote)
            .map(sagas -> sagas.stream()
                .filter(saga -> PoliticaReenvioNotificacion.decidir(saga, ahora, plazos)
                    != PoliticaReenvioNotificacion.Accion.ESPERAR)
                .map(SagaInstance::getId)
                .collect(Collectors.toList()));
    }

    /**
     * Atiende UNA saga del reenvío automático en una transacción corta: vuelve a leerla y a decidir (si algo la movió
     * mientras tanto, no hace nada) y, según la política, o cuenta un intento y pide el reenvío del correo, o da por
     * perdido el resultado y la devuelve a NOTIFICACION_FALLIDA. Todo es compare-and-set sobre el estado, así que dos
     * instancias del job, o el job y un reenvío manual, no duplican el envío. El comando sale por el outbox en la misma
     * transacción: o se registran el cambio de estado y el comando, o ninguno de los dos.
     *
     * @return la acción que se aplicó ({@code ESPERAR} si no hizo nada)
     */
    @WithTransaction
    public Uni<PoliticaReenvioNotificacion.Accion> reenviarNotificacionAutomaticamente(UUID comprobanteId,
            PoliticaReenvioNotificacion.Plazos plazos) {
        return sagaInstanceRepository.findById(comprobanteId).onItem().transformToUni(saga -> {
            if (saga == null) {
                return Uni.createFrom().item(PoliticaReenvioNotificacion.Accion.ESPERAR);
            }
            return switch (PoliticaReenvioNotificacion.decidir(saga, Instant.now(), plazos)) {
                case ESPERAR -> Uni.createFrom().item(PoliticaReenvioNotificacion.Accion.ESPERAR);
                case REENVIAR -> reenviarYContar(saga, plazos);
                case DAR_POR_SIN_RESPUESTA -> darPorSinRespuesta(saga, plazos);
            };
        });
    }

    private Uni<PoliticaReenvioNotificacion.Accion> reenviarYContar(SagaInstance saga,
            PoliticaReenvioNotificacion.Plazos plazos) {
        UUID id = saga.getId();
        return sagaInstanceRepository.registrarReenvioAutomatico(id, plazos.maxIntentos())
            .onItem().transformToUni(actualizada -> {
                if (actualizada == null) {
                    return Uni.createFrom().item(PoliticaReenvioNotificacion.Accion.ESPERAR);
                }
                // El contador de "saga" es el anterior al UPDATE (la sesión conserva la instancia ya cargada).
                String nota = Mensajes.obtenerTexto(CodigoError.REENVIO_AUTOMATICO_NOTIFICACION,
                    saga.getReintentosNotificacion() + 1, plazos.maxIntentos());
                return sagaStepRepository.persist(SagaStep.nota(id, CodigoError.REENVIO_AUTOMATICO_NOTIFICACION.codigo(), nota))
                    .chain(() -> emitirComandoDeReenvio(id))
                    .replaceWith(PoliticaReenvioNotificacion.Accion.REENVIAR);
            });
    }

    private Uni<PoliticaReenvioNotificacion.Accion> darPorSinRespuesta(SagaInstance saga,
            PoliticaReenvioNotificacion.Plazos plazos) {
        UUID id = saga.getId();
        String motivo = Mensajes.obtenerTexto(CodigoError.NOTIFICACION_SIN_RESPUESTA, plazos.plazoRespuesta().toMinutes());
        return sagaInstanceRepository.transicionarEstado(id, SagaState.NOTIFICACION_FALLIDA, motivo, SagaState.AUTORIZADO)
            .onItem().transformToUni(actualizada -> actualizada == null
                ? Uni.createFrom().item(PoliticaReenvioNotificacion.Accion.ESPERAR)
                : sagaStepRepository.persist(SagaStep.nota(id, CodigoError.NOTIFICACION_SIN_RESPUESTA.codigo(), motivo))
                    .replaceWith(PoliticaReenvioNotificacion.Accion.DAR_POR_SIN_RESPUESTA));
    }

    /**
     * Vuelve a publicar el comando enviado más recientemente (estado SENT)
     * para esta saga, usando exactamente el mismo contenido — y por lo
     * tanto el mismo {@code eventId} incrustado — que se emitió la primera
     * vez. Es la idempotencia del servicio destino la que después decide si
     * esto es un reintento genuino o si no hace falta nada.
     */
    @WithTransaction
    public Uni<Void> reintentarUltimoComando(UUID comprobanteId) {
        return sagaStepRepository.findLastSent(comprobanteId)
            .onItem().transformToUni(lastSent -> {
                if (lastSent == null) {
                    return Uni.createFrom().voidItem();
                }
                return salientes.registrar(new EventoSaliente(comprobanteId, lastSent.getStepName(),
                        lastSent.getTopic(), lastSent.getPayload()))
                    .replaceWithVoid();
            });
    }

    /**
     * Ids de las sagas que esperan respuesta de otro servicio y a las que {@link PoliticaReintentoSaga} manda
     * reintentar o expirar. Solo lectura; cada saga se procesa después en su PROPIA transacción
     * ({@link #recuperarSaga}) para que una saga que falla no arrastre a las demás ni deje a todas sin avanzar.
     */
    @WithSession
    public Uni<List<UUID>> idsParaRecuperar(PoliticaReintentoSaga.Plazos plazos, int lote) {
        Instant ahora = Instant.now();
        return sagaInstanceRepository.findByEstados(SagaState.reintentables(), lote)
            .map(sagas -> sagas.stream()
                .filter(saga -> PoliticaReintentoSaga.decidir(saga, ahora, plazos) != PoliticaReintentoSaga.Accion.ESPERAR)
                .map(SagaInstance::getId)
                .collect(Collectors.toList()));
    }

    /**
     * Reintenta (vuelve a publicar el último comando, que el destino trata de forma idempotente) o expira UNA saga.
     * Vuelve a leerla y a decidir dentro de su transacción: si otro evento la movió mientras tanto, no hace nada. La
     * transición es compare-and-set, así que dos instancias del watchdog no duplican reintentos.
     *
     * @return true si hizo algo
     */
    @WithTransaction
    public Uni<Boolean> recuperarSaga(UUID comprobanteId, PoliticaReintentoSaga.Plazos plazos) {
        return sagaInstanceRepository.findById(comprobanteId).onItem().transformToUni(saga -> {
            if (saga == null) {
                return Uni.createFrom().item(false);
            }
            PoliticaReintentoSaga.Accion accion = PoliticaReintentoSaga.decidir(saga, Instant.now(), plazos);
            return accion == PoliticaReintentoSaga.Accion.ESPERAR
                ? Uni.createFrom().item(false)
                : aplicar(accion, saga, plazos);
        });
    }

    /**
     * Cuenta como intento un reintento que FALLÓ (en su propia transacción, porque la del reintento se deshizo): sin
     * esto, una saga cuyo reintento siempre falla se reintentaría eternamente sin llegar nunca a EXPIRADA.
     */
    @WithTransaction
    public Uni<Void> contarReintentoFallido(UUID comprobanteId, PoliticaReintentoSaga.Plazos plazos, String causa) {
        return sagaInstanceRepository.findById(comprobanteId).onItem().transformToUni(saga -> {
            if (saga == null || !saga.getState().esReintentable()) {
                return Uni.createFrom().voidItem();
            }
            return sagaInstanceRepository.registrarReintento(comprobanteId, saga.getState())
                .onItem().transformToUni(actualizada -> actualizada == null
                    ? Uni.createFrom().voidItem()
                    : sagaStepRepository.persist(SagaStep.nota(comprobanteId, CodigoError.SAGA_REINTENTO_FALLIDO.codigo(),
                        Mensajes.obtenerTexto(CodigoError.SAGA_REINTENTO_FALLIDO, saga.getReintentos() + 1, plazos.maxReintentos(),
                            causa))).replaceWithVoid());
        });
    }

    private Uni<Boolean> aplicar(PoliticaReintentoSaga.Accion accion, SagaInstance saga, PoliticaReintentoSaga.Plazos plazos) {
        UUID id = saga.getId();
        if (accion == PoliticaReintentoSaga.Accion.REINTENTAR) {
            return sagaInstanceRepository.registrarReintento(id, saga.getState())
                .onItem().transformToUni(actualizada -> {
                    if (actualizada == null) {
                        return Uni.createFrom().item(false);
                    }
                    // No se usa actualizada.getReintentos(): tras el UPDATE masivo, findById devuelve la instancia ya
                    // cargada en esta sesión (caché de primer nivel) con el contador viejo.
                    String nota = Mensajes.obtenerTexto(CodigoError.SAGA_REINTENTO_AUTOMATICO, saga.getReintentos() + 1,
                        plazos.maxReintentos(), saga.getState());
                    return sagaStepRepository.persist(SagaStep.nota(id, CodigoError.SAGA_REINTENTO_AUTOMATICO.codigo(), nota))
                        .chain(() -> reintentarUltimoComando(id)).replaceWith(true);
                });
        }
        String motivo = Mensajes.obtenerTexto(CodigoError.SAGA_EXPIRADA, saga.getState(), plazos.maxReintentos());
        return sagaInstanceRepository.transicionarEstado(id, SagaState.EXPIRADA, motivo, saga.getState())
            .onItem().transformToUni(actualizada -> {
                if (actualizada == null) {
                    return Uni.createFrom().item(false);
                }
                LogCodificado.error(LOG, CodigoError.SAGA_EXPIRADA, "comprobanteId=" + id, saga.getState(),
                    plazos.maxReintentos());
                return sagaStepRepository.persist(SagaStep.nota(id, CodigoError.SAGA_EXPIRADA.codigo(), motivo))
                    .replaceWith(true);
            });
    }

    // --- Camino feliz -----------------------------------------------------

    private Uni<ResultadoEvento> procesarComprobanteFirmado(ComprobanteFirmadoEventMessage event) {
        return sagaInstanceRepository.transicionarEstado(event.comprobanteId(), SagaState.FIRMADO, null, SagaState.CREADO,
                SagaState.STOCK_RESERVADO, SagaState.EXPIRADA)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().item(ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO);
                }
                ActualizarComprobanteCommand marcarFirmado = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    event.comprobanteId(), MessagingConstants.COMANDO_MARCAR_FIRMADO, event.xmlFirmado(), null, null,
                    null, null, Instant.now());
                EnviarComprobanteCommand enviarSri = new EnviarComprobanteCommand(UUID.randomUUID(),
                    event.comprobanteId(), saga.getClaveAcceso(), saga.getEmpresaRuc(), saga.getAmbiente(),
                    event.xmlFirmado(), Instant.now());
                return emitir(event.comprobanteId(), MessagingConstants.COMANDO_MARCAR_FIRMADO,
                        MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, marcarFirmado)
                    .chain(() -> emitir(event.comprobanteId(), "EnviarComprobante",
                        MessagingConstants.SRI_COMMANDS_TOPIC, enviarSri))
                    .replaceWith(ResultadoEvento.APLICADO);
            });
    }

    private Uni<ResultadoEvento> procesarSriAutorizado(SriResultadoEventMessage event) {
        return sagaInstanceRepository.transicionarEstado(event.comprobanteId(), SagaState.AUTORIZADO, null,
                SagaState.FIRMADO, SagaState.EXPIRADA)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().item(ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO);
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    event.comprobanteId(), MessagingConstants.COMANDO_MARCAR_AUTORIZADO, null,
                    event.xmlAutorizado(), event.numeroAutorizacion(), event.fechaAutorizacion(), null,
                    Instant.now());
                // A partir de acá comprobantes-service genera el RIDE y publica
                // ComprobanteAutorizado por su cuenta — notificacion-service lo
                // consume directo, este orquestador no tiene que hacer nada más
                // para que el correo salga.
                return emitir(event.comprobanteId(), MessagingConstants.COMANDO_MARCAR_AUTORIZADO,
                    MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, command)
                    .chain(() -> emitirComandoDeStock(saga, MessagingConstants.COMANDO_STOCK_CONFIRMAR))
                    // Sin correo del receptor no habrá notificación: la saga termina aquí en vez de esperar para siempre.
                    .chain(() -> saga.isNotificacionEsperada()
                        ? Uni.createFrom().voidItem()
                        : sagaInstanceRepository.transicionarEstado(event.comprobanteId(),
                            SagaState.AUTORIZADO_SIN_NOTIFICACION, null, SagaState.AUTORIZADO).replaceWithVoid())
                    .replaceWith(ResultadoEvento.APLICADO);
            });
    }

    private Uni<ResultadoEvento> procesarNotificacionEnviada(UUID comprobanteId) {
        return sagaInstanceRepository.transicionarEstadoLimpiandoMotivo(comprobanteId, SagaState.NOTIFICADO,
                SagaState.AUTORIZADO, SagaState.NOTIFICACION_FALLIDA)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().item(ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO);
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    comprobanteId, MessagingConstants.COMANDO_MARCAR_NOTIFICADO, null, null, null, null, null,
                    Instant.now());
                return emitir(comprobanteId, MessagingConstants.COMANDO_MARCAR_NOTIFICADO,
                    MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, command)
                    .replaceWith(ResultadoEvento.APLICADO);
            });
    }

    // --- Terminales de error (sin compensación) ----------------------------

    private Uni<ResultadoEvento> procesarFirmaFallida(UUID comprobanteId, String motivoError) {
        return sagaInstanceRepository.transicionarEstado(comprobanteId, SagaState.FIRMA_FALLIDA, motivoError,
                SagaState.CREADO, SagaState.STOCK_RESERVADO, SagaState.EXPIRADA)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().item(ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO);
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    comprobanteId, MessagingConstants.COMANDO_MARCAR_FIRMA_FALLIDA, null, null, null, null,
                    motivoError, Instant.now());
                return emitir(comprobanteId, MessagingConstants.COMANDO_MARCAR_FIRMA_FALLIDA,
                    MessagingConstants.COMPROBANTE_COMMANDS_TOPIC, command)
                    .chain(() -> emitirComandoDeStock(saga, MessagingConstants.COMANDO_STOCK_LIBERAR))
                    .replaceWith(ResultadoEvento.APLICADO);
            });
    }

    private Uni<ResultadoEvento> procesarSriRechazo(SriResultadoEventMessage event, SagaState estadoDestino, String tipoComando) {
        return sagaInstanceRepository.transicionarEstado(event.comprobanteId(), estadoDestino, event.motivoRechazo(),
                SagaState.FIRMADO, SagaState.EXPIRADA)
            .onItem().transformToUni(saga -> {
                if (saga == null) {
                    return Uni.createFrom().item(ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO);
                }
                ActualizarComprobanteCommand command = new ActualizarComprobanteCommand(UUID.randomUUID(),
                    event.comprobanteId(), tipoComando, null, null, null, null, event.motivoRechazo(), Instant.now());
                return emitir(event.comprobanteId(), tipoComando, MessagingConstants.COMPROBANTE_COMMANDS_TOPIC,
                    command)
                    .chain(() -> emitirComandoDeStock(saga, MessagingConstants.COMANDO_STOCK_LIBERAR))
                    .replaceWith(ResultadoEvento.APLICADO);
            });
    }

    /**
     * Una notificación fallida no revierte nada: para este punto el
     * comprobante ya está autorizado por el SRI (legalmente válido pase lo
     * que pase con el correo), así que no se manda ningún comando a
     * comprobantes-service — solo queda registrado acá, en la saga, para
     * que un humano (o un futuro mecanismo de reintento) lo pueda ver.
     */
    private Uni<ResultadoEvento> procesarNotificacionFallida(UUID comprobanteId, String motivoError) {
        return sagaInstanceRepository.transicionarEstado(comprobanteId, SagaState.NOTIFICACION_FALLIDA, motivoError,
                SagaState.AUTORIZADO)
            .map(saga -> saga == null ? ResultadoEvento.IGNORADO_ESTADO_NO_ESPERADO : ResultadoEvento.APLICADO);
    }

    // --- Ayudantes compartidos ----------------------------------------------

    private Uni<ResultadoEvento> ejecutarSiNoProcesado(UUID eventId, Supplier<Uni<ResultadoEvento>> accion) {
        return mensajesProcesados.yaProcesado(eventId)
            .onItem().transformToUni(procesado -> Boolean.TRUE.equals(procesado)
                ? Uni.createFrom().item(ResultadoEvento.DUPLICADO)
                : accion.get().call(() -> marcarProcesado(eventId)));
    }

    private Uni<Void> registrarRecibido(UUID sagaId, String stepName) {
        return sagaStepRepository.persist(SagaStep.crearRecibido(sagaId, stepName)).replaceWithVoid();
    }

    private Uni<Void> emitir(UUID sagaId, String stepName, String topic, Object message) {
        String json = serializarAJson(message);
        return salientes.registrar(new EventoSaliente(sagaId, stepName, topic, json))
            .chain(() -> sagaStepRepository.persist(SagaStep.crearEnviado(sagaId, stepName, topic, json)))
            .replaceWithVoid();
    }

    private Uni<Void> marcarProcesado(UUID eventId) {
        return mensajesProcesados.marcarProcesado(eventId);
    }

    private <T> T leerJson(String json, Class<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer el payload guardado de la saga", e);
        }
    }

    private String serializarAJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
