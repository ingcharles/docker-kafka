package com.facturacion.orquestacion.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SagaResponse {

    private UUID comprobanteId;
    private String state;
    private String emisorRuc;
    private String claveAcceso;
    private String ambiente;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
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

    public String getEmisorRuc() {
        return emisorRuc;
    }

    public void setEmisorRuc(String emisorRuc) {
        this.emisorRuc = emisorRuc;
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
