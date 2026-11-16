package com.facturacion.comprobantes.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.comprobantes.application.event.ActualizarComprobanteCommand;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.port.EventosSalientes;
import com.facturacion.comprobantes.domain.port.GeneradorRide;
import com.facturacion.comprobantes.domain.repository.CatalogoSriRepository;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.domain.repository.ConfiguracionEmpresaLocalRepository;
import com.facturacion.comprobantes.shared.constant.MessagingConstants;
import com.facturacion.comprobantes.shared.constant.ZonaHoraria;

/**
 * Aplica al comprobante las transiciones de estado que llegan del orquestador de la saga (ver
 * {@code MessagingConstants.COMANDO_*}); es la otra mitad del ciclo de vida, separada de la emisión. Es aquí, en
 * {@code MARCAR_AUTORIZADO}, donde se genera y guarda el RIDE en PDF (nunca antes: debe mostrar el número y la
 * fecha de autorización reales).
 * <p>
 * Una transición que no sigue el orden del ciclo de vida (ver {@link EstadoComprobante#puedePasarA}) no se aplica:
 * un comando repetido o fuera de orden ya no puede retroceder un comprobante autorizado a "firmado", ni regenerar
 * el RIDE y reenviar el correo.
 */
@ApplicationScoped
public class ActualizacionComprobanteService {

    private static final Logger LOG = Logger.getLogger(ActualizacionComprobanteService.class);

    private final ComprobanteRepository comprobanteRepository;
    private final ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository;
    private final CatalogoSriRepository catalogoSriRepository;
    private final GeneradorRide rideGenerador;
    private final EventosSalientes salientes;
    private final EventosComprobante eventos;

    @Inject
    public ActualizacionComprobanteService(ComprobanteRepository comprobanteRepository,
            ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository,
            CatalogoSriRepository catalogoSriRepository, GeneradorRide rideGenerador,
            EventosSalientes salientes, EventosComprobante eventos) {
        this.comprobanteRepository = comprobanteRepository;
        this.configuracionEmpresaLocalRepository = configuracionEmpresaLocalRepository;
        this.catalogoSriRepository = catalogoSriRepository;
        this.rideGenerador = rideGenerador;
        this.salientes = salientes;
        this.eventos = eventos;
    }

    /** Estado al que lleva cada comando; un comando desconocido es un error de programación del orquestador. */
    static EstadoComprobante estadoDestino(String tipoComando) {
        return switch (tipoComando) {
            case MessagingConstants.COMANDO_MARCAR_FIRMADO -> EstadoComprobante.FIRMADO;
            case MessagingConstants.COMANDO_MARCAR_FIRMA_FALLIDA -> EstadoComprobante.FIRMA_FALLIDA;
            case MessagingConstants.COMANDO_MARCAR_NO_AUTORIZADO -> EstadoComprobante.NO_AUTORIZADO;
            case MessagingConstants.COMANDO_MARCAR_NOTIFICADO -> EstadoComprobante.NOTIFICADO;
            case MessagingConstants.COMANDO_MARCAR_AUTORIZADO -> EstadoComprobante.AUTORIZADO;
            case MessagingConstants.COMANDO_MARCAR_STOCK_INSUFICIENTE -> EstadoComprobante.STOCK_INSUFICIENTE;
            default -> throw new IllegalArgumentException(
                "Tipo de comando de comprobante desconocido: " + tipoComando);
        };
    }

    @WithTransaction
    public Uni<ResultadoComando> aplicarComando(ActualizarComprobanteCommand command) {
        return comprobanteRepository.findById(command.comprobanteId())
            .onItem().ifNull().failWith(() ->
                new NotFoundException("Comprobante no encontrado: " + command.comprobanteId()))
            .onItem().transformToUni(comprobante -> {
                EstadoComprobante destino = estadoDestino(command.tipoComando());
                if (comprobante.saltaPasos(destino)) {
                    // No se descarta en silencio: el comprobante quedaría atascado aunque el SRI ya lo autorizó.
                    LOG.errorf("Comando %s (%s) fuera de orden en el comprobante %s: %s -> %s se salta pasos del ciclo",
                        command.tipoComando(), command.eventId(), comprobante.getId(), comprobante.getEstado(), destino);
                    return Uni.<ResultadoComando>createFrom().failure(new IllegalStateException(
                        "Comando " + command.tipoComando() + " fuera de orden: el comprobante " + comprobante.getId()
                            + " está " + comprobante.getEstado() + " y no puede pasar a " + destino
                            + " sin aplicar antes los pasos intermedios"));
                }
                if (!comprobante.puedePasarA(destino)) {
                    LOG.warnf("Se ignora el comando %s (%s) del comprobante %s: no se puede pasar de %s a %s",
                        command.tipoComando(), command.eventId(), comprobante.getId(), comprobante.getEstado(),
                        destino);
                    return Uni.createFrom().item(ResultadoComando.IGNORADO_TRANSICION_NO_PERMITIDA);
                }
                if (destino == EstadoComprobante.AUTORIZADO) {
                    return autorizar(comprobante, command).replaceWith(ResultadoComando.APLICADO);
                }
                aplicarTransicion(comprobante, destino, command);
                return comprobanteRepository.update(comprobante).replaceWith(ResultadoComando.APLICADO);
            });
    }

    private static void aplicarTransicion(Comprobante comprobante, EstadoComprobante destino,
            ActualizarComprobanteCommand command) {
        switch (destino) {
            case FIRMADO -> comprobante.marcarFirmado(command.xmlFirmado());
            case FIRMA_FALLIDA -> comprobante.marcarFirmaFallida(command.motivoRechazo());
            case NO_AUTORIZADO -> comprobante.marcarNoAutorizado(command.motivoRechazo());
            case NOTIFICADO -> comprobante.marcarNotificado();
            case STOCK_INSUFICIENTE -> comprobante.marcarStockInsuficiente(command.motivoRechazo());
            default -> throw new IllegalArgumentException("Destino no aplicable por este camino: " + destino);
        }
    }

    /**
     * El RIDE se genera acá, no antes: recién ahora existen numeroAutorizacion/fechaAutorizacion reales para
     * mostrar. El colorRide es un ajuste opcional por empresa (configuracion_empresa en
     * firma-electronica-service, ver ConfiguracionEmpresaLocal); si no está, GeneradorRide usa su estilo por defecto.
     */
    private Uni<Void> autorizar(Comprobante comprobante, ActualizarComprobanteCommand command) {
        comprobante.marcarAutorizado(command.xmlAutorizado(), command.numeroAutorizacion(),
            command.fechaAutorizacion());
        // Las lecturas van encadenadas, no en paralelo: dos consultas Hibernate Reactive concurrentes sobre la misma
        // sesión rompen el procesamiento del result set.
        return configuracionEmpresaLocalRepository.findByRucAndClave(comprobante.getEmpresaRuc(), "ride.color")
            .onItem().transformToUni(configuracionColor -> configuracionEmpresaLocalRepository
                .findByRucAndClave(comprobante.getEmpresaRuc(), "ride.logo")
                .onItem().transformToUni(configuracionLogo -> catalogoSriRepository
                    .cargar(comprobante.getFechaEmision().atOffset(ZonaHoraria.ECUADOR).toLocalDate())
                    .onItem().transformToUni(catalogos -> {
                        String colorRide = configuracionColor == null ? null : configuracionColor.getValor();
                        byte[] logo = decodificarLogo(configuracionLogo == null ? null : configuracionLogo.getValor());
                        // Las descripciones de las formas de pago salen del catálogo (tabla forma_pago).
                        comprobante.adjuntarRide(rideGenerador.generar(comprobante, colorRide, logo, catalogos));
                        return comprobanteRepository.update(comprobante)
                            .chain(this::publicarAutorizadoSiHayCorreo);
                    })));
    }

    /** El logo viaja en base64 (ajuste "ride.logo"); si no está o no se puede decodificar, el RIDE sale sin logo. */
    private static byte[] decodificarLogo(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            return java.util.Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            LOG.warnf("El ajuste ride.logo no es base64 válido; el RIDE sale sin logo");
            return null;
        }
    }

    /** Si el llamador nunca dio un correo no hay nada que notificar: no es un error, simplemente no se publica. */
    private Uni<Void> publicarAutorizadoSiHayCorreo(Comprobante comprobante) {
        if (!EventosComprobante.tieneCorreo(comprobante)) {
            return Uni.createFrom().voidItem();
        }
        return salientes.registrar(eventos.construirEventoAutorizado(comprobante)).replaceWithVoid();
    }
}
