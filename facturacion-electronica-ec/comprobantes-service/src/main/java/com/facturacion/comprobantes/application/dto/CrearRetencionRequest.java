package com.facturacion.comprobantes.application.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Certifica la retención de IVA/renta aplicada sobre uno o más documentos
 * sustento — no tiene {@code <detalles>} de ítems, solo la lista de líneas
 * de retención ({@link RetencionLineaRequest}). El "sujeto retenido" (a
 * quien se le retiene) se guarda reutilizando las columnas comprador_* del
 * agregado Comprobante — mismo dato, distinto nombre según el tipo de
 * comprobante (ver ComprobanteService).
 */
public class CrearRetencionRequest {

    @NotNull
    @Valid
    private EmisorRequest emisor;

    @NotBlank
    private String sujetoRetenidoTipoIdentificacion;

    @NotBlank
    private String sujetoRetenidoIdentificacion;

    @NotBlank
    private String sujetoRetenidoRazonSocial;

    private String sujetoRetenidoCorreo;

    /** MM/aaaa */
    @NotBlank
    private String periodoFiscal;

    @NotEmpty
    @Valid
    private List<RetencionLineaRequest> retenciones;

    @Valid
    private List<CampoAdicionalRequest> campoAdicional = List.of();

    public EmisorRequest getEmisor() {
        return emisor;
    }

    public void setEmisor(EmisorRequest emisor) {
        this.emisor = emisor;
    }

    public String getSujetoRetenidoTipoIdentificacion() {
        return sujetoRetenidoTipoIdentificacion;
    }

    public void setSujetoRetenidoTipoIdentificacion(String sujetoRetenidoTipoIdentificacion) {
        this.sujetoRetenidoTipoIdentificacion = sujetoRetenidoTipoIdentificacion;
    }

    public String getSujetoRetenidoIdentificacion() {
        return sujetoRetenidoIdentificacion;
    }

    public void setSujetoRetenidoIdentificacion(String sujetoRetenidoIdentificacion) {
        this.sujetoRetenidoIdentificacion = sujetoRetenidoIdentificacion;
    }

    public String getSujetoRetenidoRazonSocial() {
        return sujetoRetenidoRazonSocial;
    }

    public void setSujetoRetenidoRazonSocial(String sujetoRetenidoRazonSocial) {
        this.sujetoRetenidoRazonSocial = sujetoRetenidoRazonSocial;
    }

    public String getSujetoRetenidoCorreo() {
        return sujetoRetenidoCorreo;
    }

    public void setSujetoRetenidoCorreo(String sujetoRetenidoCorreo) {
        this.sujetoRetenidoCorreo = sujetoRetenidoCorreo;
    }

    public String getPeriodoFiscal() {
        return periodoFiscal;
    }

    public void setPeriodoFiscal(String periodoFiscal) {
        this.periodoFiscal = periodoFiscal;
    }

    public List<RetencionLineaRequest> getRetenciones() {
        return retenciones;
    }

    public void setRetenciones(List<RetencionLineaRequest> retenciones) {
        this.retenciones = retenciones;
    }

    public List<CampoAdicionalRequest> getCampoAdicional() {
        return campoAdicional;
    }

    public void setCampoAdicional(List<CampoAdicionalRequest> campoAdicional) {
        this.campoAdicional = campoAdicional;
    }
}
