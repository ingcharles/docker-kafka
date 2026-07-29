package com.facturacion.comprobantes.application.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * No lleva comprador ni valores monetarios — es un traslado de bienes, no
 * una venta (infoGuiaRemision de la ficha técnica). Puede tener uno o más
 * destinatarios, cada uno con su propia lista de ítems trasladados.
 */
public class CrearGuiaRemisionRequest {

    @NotNull
    @Valid
    private EmpresaRequest empresa;

    @NotBlank
    private String dirPartida;

    @NotBlank
    private String transportistaRazonSocial;

    @NotBlank
    private String transportistaTipoIdentificacion;

    @NotBlank
    private String transportistaRuc;

    /** dd/mm/aaaa */
    @NotBlank
    private String fechaIniTransporte;

    /** dd/mm/aaaa */
    @NotBlank
    private String fechaFinTransporte;

    /**
     * Obligatorio en toda Guía de Remisión según el XSD real del SRI
     * (confirmado con un rechazo real de Recepción — la ficha técnica en
     * texto solo dejaba claro que es obligatorio en Factura cuando el
     * empresa vende combustibles, Tabla 29; en Guía de Remisión lo exige
     * siempre el propio esquema). Si de verdad no hay vehículo, usar el
     * código {@code ZZZ9999} documentado en esa misma tabla.
     */
    @NotBlank
    private String placa;

    @NotEmpty
    @Valid
    private List<DestinatarioGuiaRemisionRequest> destinatarios;

    @Valid
    private List<CampoAdicionalRequest> campoAdicional = List.of();

    public EmpresaRequest getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaRequest empresa) {
        this.empresa = empresa;
    }

    public String getDirPartida() {
        return dirPartida;
    }

    public void setDirPartida(String dirPartida) {
        this.dirPartida = dirPartida;
    }

    public String getTransportistaRazonSocial() {
        return transportistaRazonSocial;
    }

    public void setTransportistaRazonSocial(String transportistaRazonSocial) {
        this.transportistaRazonSocial = transportistaRazonSocial;
    }

    public String getTransportistaTipoIdentificacion() {
        return transportistaTipoIdentificacion;
    }

    public void setTransportistaTipoIdentificacion(String transportistaTipoIdentificacion) {
        this.transportistaTipoIdentificacion = transportistaTipoIdentificacion;
    }

    public String getTransportistaRuc() {
        return transportistaRuc;
    }

    public void setTransportistaRuc(String transportistaRuc) {
        this.transportistaRuc = transportistaRuc;
    }

    public String getFechaIniTransporte() {
        return fechaIniTransporte;
    }

    public void setFechaIniTransporte(String fechaIniTransporte) {
        this.fechaIniTransporte = fechaIniTransporte;
    }

    public String getFechaFinTransporte() {
        return fechaFinTransporte;
    }

    public void setFechaFinTransporte(String fechaFinTransporte) {
        this.fechaFinTransporte = fechaFinTransporte;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public List<DestinatarioGuiaRemisionRequest> getDestinatarios() {
        return destinatarios;
    }

    public void setDestinatarios(List<DestinatarioGuiaRemisionRequest> destinatarios) {
        this.destinatarios = destinatarios;
    }

    public List<CampoAdicionalRequest> getCampoAdicional() {
        return campoAdicional;
    }

    public void setCampoAdicional(List<CampoAdicionalRequest> campoAdicional) {
        this.campoAdicional = campoAdicional;
    }
}
