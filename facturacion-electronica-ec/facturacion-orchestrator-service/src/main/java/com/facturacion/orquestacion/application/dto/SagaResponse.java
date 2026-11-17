package com.facturacion.orquestacion.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SagaResponse {

    private UUID comprobanteId;
    private String state;
    private String empresaRuc;
    private String claveAcceso;
    private String ambiente;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    /** Reintentos automáticos del watchdog; si la saga quedó EXPIRADA, es el máximo configurado. */
    private int reintentos;
    /** Reenvíos automáticos del correo hechos por el job; si llegó al máximo configurado, el job ya no la toca. */
    private int reintentosNotificacion;
    private List<SagaStepResponse> steps;

    public UUID getComprobanteId() {
        return comprobanteId;
    }

    public void setComprobanteId(UUID comprobanteId) {
        this.comprobanteId = comprobanteId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
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

    public int getReintentos() {
        return reintentos;
    }

    public void setReintentos(int reintentos) {
        this.reintentos = reintentos;
    }

    public int getReintentosNotificacion() {
        return reintentosNotificacion;
    }

    public void setReintentosNotificacion(int reintentosNotificacion) {
        this.reintentosNotificacion = reintentosNotificacion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SagaStepResponse> getSteps() {
        return steps;
    }

    public void setSteps(List<SagaStepResponse> steps) {
        this.steps = steps;
    }
}
