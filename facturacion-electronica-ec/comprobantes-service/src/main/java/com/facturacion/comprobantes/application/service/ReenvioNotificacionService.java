package com.facturacion.comprobantes.application.service;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.port.EventosSalientes;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Reenvío del correo de un comprobante ya autorizado (p. ej. cuando el SMTP falló). Vuelve a publicar
 * {@code ComprobanteAutorizado} con un {@code eventId} nuevo, que lleva el RIDE y el XML ya guardados:
 * notificacion-service lo procesa como un envío más y el resultado vuelve por el camino normal. No toca el estado del
 * comprobante ni llama a otros servicios.
 */
@ApplicationScoped
public class ReenvioNotificacionService {

    private final ComprobanteRepository comprobanteRepository;
    private final EventosSalientes salientes;
    private final EventosComprobante eventos;

    @Inject
    public ReenvioNotificacionService(ComprobanteRepository comprobanteRepository, EventosSalientes salientes,
            EventosComprobante eventos) {
        this.comprobanteRepository = comprobanteRepository;
        this.salientes = salientes;
        this.eventos = eventos;
    }

    /** @param empresaRuc el de la api-key: un comprobante de otra empresa se trata como inexistente (404). */
    @WithTransaction
    public Uni<Void> reenviarCorreo(UUID comprobanteId, String empresaRuc) {
        return comprobanteRepository.findById(comprobanteId)
            .onItem().transform(c -> c != null && c.getEmpresaRuc().equals(empresaRuc) ? c : null)
            .onItem().ifNull().failWith(() -> new NotFoundException(CodigoError.COMPROBANTE_NO_ENCONTRADO, comprobanteId))
            .onItem().invoke(ReenvioNotificacionService::exigirReenviable)
            .chain(c -> salientes.registrar(eventos.construirEventoAutorizado(c)));
    }

    /** Solo se reenvía lo que el SRI ya autorizó, que tiene RIDE y un correo del receptor al cual enviarlo. */
    static void exigirReenviable(Comprobante comprobante) {
        boolean autorizado = comprobante.getEstado() == EstadoComprobante.AUTORIZADO
            || comprobante.getEstado() == EstadoComprobante.NOTIFICADO;
        if (!autorizado || comprobante.getRidePdf() == null || comprobante.getXmlAutorizado() == null) {
            throw new ConflictoException(CodigoError.REENVIO_NO_AUTORIZADO, comprobante.getEstado());
        }
        if (!EventosComprobante.tieneCorreo(comprobante)) {
            throw new ConflictoException(CodigoError.REENVIO_SIN_CORREO);
        }
    }
}
