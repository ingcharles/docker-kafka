package com.facturacion.notificacion.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.notificacion.domain.model.enums.EstadoNotificacion;

/**
 * La notificación por correo de un comprobante — el agregado de este servicio. Un
 * único registro por comprobante (índice único) que guarda el resultado del último
 * envío: un evento ComprobanteAutorizado duplicado (Kafka, "al menos una vez") lo
 * descarta el inbox por eventId, y un reenvío pedido por el usuario trae un eventId
 * nuevo y actualiza este mismo registro.
 */
public class NotificacionEnviada {

    private UUID id;
    private UUID comprobanteId;
    private String claveAcceso;
    private String destinatario;
    private EstadoNotificacion estado;
    private String motivoError;
    private Instant enviadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getComprobanteId() {
        return comprobanteId;
    }

    public void setComprobanteId(UUID comprobanteId) {
        this.comprobanteId = comprobanteId;
    }

    public String getClaveAcceso() {
        return claveAcceso;
    }

    public void setClaveAcceso(String claveAcceso) {
        this.claveAcceso = claveAcceso;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public EstadoNotificacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoNotificacion estado) {
        this.estado = estado;
    }

    public String getMotivoError() {
        return motivoError;
    }

    public void setMotivoError(String motivoError) {
        this.motivoError = motivoError;
    }

    public Instant getEnviadoEn() {
        return enviadoEn;
    }

    public void setEnviadoEn(Instant enviadoEn) {
        this.enviadoEn = enviadoEn;
    }
}
