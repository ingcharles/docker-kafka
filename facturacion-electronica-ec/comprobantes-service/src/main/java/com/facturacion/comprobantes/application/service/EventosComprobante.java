package com.facturacion.comprobantes.application.service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.comprobantes.application.event.ComprobanteAutorizadoEventMessage;
import com.facturacion.comprobantes.application.event.ComprobanteCreadoEventMessage;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.comprobantes.shared.constant.MessagingConstants;

/**
 * Arma los eventos que este servicio deja en el outbox (el relay los publica en Kafka). Un solo lugar para el
 * contenido y la serialización de {@code ComprobanteCreado} y {@code ComprobanteAutorizado}, que antes estaban
 * repartidos dentro de {@code ComprobanteService}.
 */
@ApplicationScoped
public class EventosComprobante {

    private final ObjectMapper objectMapper;

    @Inject
    public EventosComprobante(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** {@code ComprobanteCreado}: lleva el XML generado, que viaja sin cambios hasta firma-electronica-service. */
    public OutboxEventEntity creado(Comprobante comprobante) {
        ComprobanteCreadoEventMessage mensaje = new ComprobanteCreadoEventMessage(
            UUID.randomUUID(),
            comprobante.getId(),
            MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO,
            comprobante.getEmpresaRuc(),
            comprobante.getClaveAcceso(),
            comprobante.getAmbiente().name(),
            comprobante.getXmlGenerado(),
            tieneCorreo(comprobante),
            Instant.now());
        return OutboxEventEntity.of(comprobante.getId(), MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO,
            MessagingConstants.COMPROBANTE_EVENTS_TOPIC, serializar(mensaje));
    }

    /**
     * {@code ComprobanteAutorizado}: notificacion-service no puede llamar de vuelta a este servicio (cero REST
     * entre servicios) ni regenerar el RIDE por su cuenta, así que el RIDE ya generado viaja embebido.
     * Solo tiene sentido si el receptor dio un correo (ver {@link #tieneCorreo}).
     */
    public OutboxEventEntity autorizado(Comprobante comprobante) {
        ComprobanteAutorizadoEventMessage mensaje = new ComprobanteAutorizadoEventMessage(
            UUID.randomUUID(),
            comprobante.getId(),
            MessagingConstants.EVENT_TYPE_COMPROBANTE_AUTORIZADO,
            comprobante.getEmpresaRuc(),
            comprobante.getClaveAcceso(),
            comprobante.getTipoComprobante().name(),
            comprobante.getCompradorCorreo(),
            comprobante.getCompradorRazonSocial(),
            Base64.getEncoder().encodeToString(comprobante.getRidePdf()),
            comprobante.getXmlAutorizado(),
            Instant.now());
        return OutboxEventEntity.of(comprobante.getId(), MessagingConstants.EVENT_TYPE_COMPROBANTE_AUTORIZADO,
            MessagingConstants.COMPROBANTE_EVENTS_TOPIC, serializar(mensaje));
    }

    /** Sin correo del receptor no se publica ComprobanteAutorizado y por tanto nunca habrá notificación. */
    public static boolean tieneCorreo(Comprobante comprobante) {
        return comprobante.getCompradorCorreo() != null && !comprobante.getCompradorCorreo().isBlank();
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
