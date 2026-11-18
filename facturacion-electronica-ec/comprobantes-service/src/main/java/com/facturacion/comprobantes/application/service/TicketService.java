package com.facturacion.comprobantes.application.service;

import java.util.Arrays;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.FormatoTicket;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.AnchoTicket;
import com.facturacion.comprobantes.domain.port.AnchosTicket;
import com.facturacion.comprobantes.domain.port.GeneradorTicket;
import com.facturacion.comprobantes.domain.repository.CatalogoSriRepository;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Arma el ticket (recibo corto para impresora térmica) de un comprobante ya emitido. Solo lee: el ticket sale de los
 * datos del comprobante y de los catálogos a su fecha de emisión; se puede pedir las veces que haga falta.
 */
@ApplicationScoped
public class TicketService {

    private final ComprobanteConsultaService consultaService;
    private final CatalogoSriRepository catalogoSriRepository;
    private final GeneradorTicket generadorTicket;
    private final AnchosTicket anchosTicket;

    @Inject
    public TicketService(ComprobanteConsultaService consultaService, CatalogoSriRepository catalogoSriRepository,
            GeneradorTicket generadorTicket, AnchosTicket anchosTicket) {
        this.consultaService = consultaService;
        this.catalogoSriRepository = catalogoSriRepository;
        this.generadorTicket = generadorTicket;
        this.anchosTicket = anchosTicket;
    }

    /** El ticket y el formato con el que se armó (para el tipo de contenido de la respuesta). */
    public record Ticket(byte[] contenido, FormatoTicket formato, String claveAcceso) {
    }

    @WithSession
    public Uni<Ticket> generar(UUID id, String empresaRuc, String formatoSolicitado, Integer anchoMilimetros) {
        FormatoTicket formato = FormatoTicket.buscarPorNombre(formatoSolicitado).orElseThrow(
            () -> new ErrorNegocioException(CodigoError.TICKET_PARAMETRO_INVALIDO, FormatoTicket.PARAMETRO, formatoSolicitado,
                Arrays.stream(FormatoTicket.values()).map(FormatoTicket::nombre).toList()));
        AnchoTicket ancho = anchoMilimetros == null ? anchosTicket.obtenerPorDefecto()
            : anchosTicket.buscar(anchoMilimetros).orElseThrow(
                () -> new ErrorNegocioException(CodigoError.TICKET_PARAMETRO_INVALIDO, AnchoTicket.PARAMETRO,
                    anchoMilimetros, anchosTicket.listarDisponibles()));
        return consultaService.obtenerDominioPorId(id, empresaRuc)
            .onItem().transformToUni(c -> {
                if (!esTipoConTicket(c)) {
                    return Uni.createFrom().failure(new ErrorNegocioException(CodigoError.TICKET_TIPO_NO_DISPONIBLE,
                        c.getTipoComprobante().nombre()));
                }
                return catalogoSriRepository.cargar(FechasSri.convertirAFecha(c.getFechaEmision()))
                    .map(catalogos -> new Ticket(generadorTicket.generar(c, formato, ancho, catalogos), formato,
                        c.getClaveAcceso()));
            });
    }

    /** Guías de remisión y retenciones no tienen líneas con precio ni totales de venta: no hay qué cobrar en un ticket. */
    private static boolean esTipoConTicket(Comprobante c) {
        TipoComprobante tipo = c.getTipoComprobante();
        return tipo != TipoComprobante.GUIA_REMISION && tipo != TipoComprobante.RETENCION;
    }
}
