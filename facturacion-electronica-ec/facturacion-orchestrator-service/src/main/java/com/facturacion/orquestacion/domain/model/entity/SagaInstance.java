package com.facturacion.orquestacion.domain.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.facturacion.orquestacion.domain.model.enums.SagaState;

/**
 * Agregado de la saga: una fila por comprobante, donde {@code id} es
 * directamente el {@code comprobanteId} (el id de correlación de toda la
 * saga). Guarda solo lo que hace falta para construir cualquier comando de
 * aquí en adelante sin volver a preguntarle nada a comprobantes-service —
 * {@code empresaRuc} lo necesita {@code FirmarComprobanteCommand} (la empresa
 * ya no se identifica por un id interno de firma-electronica-service, ver
 * ARQUITECTURA.md "Empresa sin ID"), {@code claveAcceso}+{@code ambiente} los
 * necesita {@code EnviarComprobanteCommand}.
 */
public class SagaInstance {

    private UUID id;
    private SagaState state;
    private String empresaRuc;
    private String claveAcceso;
    private String ambiente;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    /** Reintentos automáticos del watchdog (ver SagaWatchdog); los manuales por REST no cuentan. */
    private int reintentos;
    /** false si el comprobante no tiene correo: tras la autorización la saga termina sin esperar una notificación. */
    private boolean notificacionEsperada = true;

    public SagaInstance() {
    }

    public static SagaInstance start(UUID comprobanteId, String empresaRuc, String claveAcceso, String ambiente,
            boolean notificacionEsperada) {
        SagaInstance instance = new SagaInstance();
        instance.id = comprobanteId;
        instance.state = SagaState.CREADO;
        instance.empresaRuc = empresaRuc;
        instance.claveAcceso = claveAcceso;
        instance.ambiente = ambiente;
        instance.notificacionEsperada = notificacionEsperada;
        Instant now = Instant.now();
        instance.createdAt = now;
        instance.updatedAt = now;
        return instance;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SagaState getState() {
        return state;
    }

    public void setState(SagaState state) {
        this.state = state;
    }

    public String getEmpresaRuc() {
        return empresaRuc;
    }

    public void setEmpresaRuc(String empresaRuc) {
        this.empresaRuc = empresaRuc;
    }

    public String getClaveAcceso() {
        return claveAcceso;
    }

    public void setClaveAcceso(String claveAcceso) {
        this.claveAcceso = claveAcceso;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isNotificacionEsperada() {
        return notificacionEsperada;
    }

    public void setNotificacionEsperada(boolean notificacionEsperada) {
        this.notificacionEsperada = notificacionEsperada;
    }

    public int getReintentos() {
        return reintentos;
    }

    public void setReintentos(int reintentos) {
        this.reintentos = reintentos;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
