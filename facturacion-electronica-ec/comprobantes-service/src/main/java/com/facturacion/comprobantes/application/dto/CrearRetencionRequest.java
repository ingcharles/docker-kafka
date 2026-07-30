package com.facturacion.comprobantes.application.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Certifica la retención de IVA/renta aplicada sobre uno o más documentos
 * sustento (XSD v2.0.0, estilo ATS) — no tiene {@code <detalles>} de
 * ítems, sino una lista de {@link DocumentoSustentoRequest}, cada uno con
 * sus propias líneas de retención. El "sujeto retenido" (a quien se le
 * retiene) se guarda reutilizando las columnas comprador_* del agregado
 * Comprobante — mismo dato, distinto nombre según el tipo de comprobante
 * (ver ComprobanteService).
 */
public class CrearRetencionRequest {

    @NotNull
    @Valid
    private EmpresaRequest empresa;

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

    /** "SI"|"NO" — si el sujeto retenido es parte relacionada (XSD v2.0.0); si no se manda, se asume "NO". */
    private String parteRelacionada;

    @NotEmpty
    @Valid
    private List<DocumentoSustentoRequest> documentosSustento;

    @Valid
    private List<CampoAdicionalRequest> campoAdicional = List.of();

    public EmpresaRequest getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaRequest empresa) {
        this.empresa = empresa;
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

    public String getParteRelacionada() {
        return parteRelacionada;
    }

    public void setParteRelacionada(String parteRelacionada) {
        this.parteRelacionada = parteRelacionada;
    }

    public List<DocumentoSustentoRequest> getDocumentosSustento() {
        return documentosSustento;
    }

    public void setDocumentosSustento(List<DocumentoSustentoRequest> documentosSustento) {
        this.documentosSustento = documentosSustento;
    }

    public List<CampoAdicionalRequest> getCampoAdicional() {
        return campoAdicional;
    }

    public void setCampoAdicional(List<CampoAdicionalRequest> campoAdicional) {
        this.campoAdicional = campoAdicional;
    }
}
