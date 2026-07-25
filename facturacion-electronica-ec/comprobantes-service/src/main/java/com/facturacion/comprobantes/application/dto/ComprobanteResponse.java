package com.facturacion.comprobantes.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ComprobanteResponse {

    private UUID id;
    private String tipoComprobante;
    private String claveAcceso;
    private String ambiente;
    private String establecimiento;
    private String puntoEmision;
    private String secuencial;
    private UUID emisorId;
    private String emisorRuc;
    private String emisorRazonSocial;
    private Instant fechaEmision;
    private String compradorTipoIdentificacion;
    private String compradorIdentificacion;
    private String compradorRazonSocial;
    private String compradorDireccion;
    private String compradorCorreo;
    private BigDecimal subtotalSinImpuestos;
    private BigDecimal totalDescuento;
    private BigDecimal importeTotal;
    private String moneda;
    private String estado;
    private String numeroAutorizacion;
    private Instant fechaAutorizacion;
    private String motivoRechazo;

    private String direccionEstablecimiento;
    private String docModificadoTipo;
    private String docModificadoNumero;
    private Instant docModificadoFecha;
    private String motivo;
    private String periodoFiscal;
    private String transportistaRazonSocial;
    private String transportistaTipoIdentificacion;
    private String transportistaRuc;
    private String dirPartida;
    private Instant fechaIniTransporte;
    private Instant fechaFinTransporte;
    private String placa;
    private List<DestinatarioGuiaRemisionResponse> destinatarios;

    private List<DetalleComprobanteResponse> detalles;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
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

    public String getEstablecimiento() {
        return establecimiento;
    }

    public void setEstablecimiento(String establecimiento) {
        this.establecimiento = establecimiento;
    }

    public String getPuntoEmision() {
        return puntoEmision;
    }

    public void setPuntoEmision(String puntoEmision) {
        this.puntoEmision = puntoEmision;
    }

    public String getSecuencial() {
        return secuencial;
    }

    public void setSecuencial(String secuencial) {
        this.secuencial = secuencial;
    }

    public UUID getEmisorId() {
        return emisorId;
    }

    public void setEmisorId(UUID emisorId) {
        this.emisorId = emisorId;
    }

    public String getEmisorRuc() {
        return emisorRuc;
    }

    public void setEmisorRuc(String emisorRuc) {
        this.emisorRuc = emisorRuc;
    }

    public String getEmisorRazonSocial() {
        return emisorRazonSocial;
    }

    public void setEmisorRazonSocial(String emisorRazonSocial) {
        this.emisorRazonSocial = emisorRazonSocial;
    }

    public Instant getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(Instant fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getCompradorTipoIdentificacion() {
        return compradorTipoIdentificacion;
    }

    public void setCompradorTipoIdentificacion(String compradorTipoIdentificacion) {
        this.compradorTipoIdentificacion = compradorTipoIdentificacion;
    }

    public String getCompradorIdentificacion() {
        return compradorIdentificacion;
    }

    public void setCompradorIdentificacion(String compradorIdentificacion) {
        this.compradorIdentificacion = compradorIdentificacion;
    }

    public String getCompradorRazonSocial() {
        return compradorRazonSocial;
    }

    public void setCompradorRazonSocial(String compradorRazonSocial) {
        this.compradorRazonSocial = compradorRazonSocial;
    }

    public String getCompradorDireccion() {
        return compradorDireccion;
    }

    public void setCompradorDireccion(String compradorDireccion) {
        this.compradorDireccion = compradorDireccion;
    }

    public String getCompradorCorreo() {
        return compradorCorreo;
    }

    public void setCompradorCorreo(String compradorCorreo) {
        this.compradorCorreo = compradorCorreo;
    }

    public BigDecimal getSubtotalSinImpuestos() {
        return subtotalSinImpuestos;
    }

    public void setSubtotalSinImpuestos(BigDecimal subtotalSinImpuestos) {
        this.subtotalSinImpuestos = subtotalSinImpuestos;
    }

    public BigDecimal getTotalDescuento() {
        return totalDescuento;
    }

    public void setTotalDescuento(BigDecimal totalDescuento) {
        this.totalDescuento = totalDescuento;
    }

    public BigDecimal getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNumeroAutorizacion() {
        return numeroAutorizacion;
    }

    public void setNumeroAutorizacion(String numeroAutorizacion) {
        this.numeroAutorizacion = numeroAutorizacion;
    }

    public Instant getFechaAutorizacion() {
        return fechaAutorizacion;
    }

    public void setFechaAutorizacion(Instant fechaAutorizacion) {
        this.fechaAutorizacion = fechaAutorizacion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getDireccionEstablecimiento() {
        return direccionEstablecimiento;
    }

    public void setDireccionEstablecimiento(String direccionEstablecimiento) {
        this.direccionEstablecimiento = direccionEstablecimiento;
    }

    public String getDocModificadoTipo() {
        return docModificadoTipo;
    }

    public void setDocModificadoTipo(String docModificadoTipo) {
        this.docModificadoTipo = docModificadoTipo;
    }

    public String getDocModificadoNumero() {
        return docModificadoNumero;
    }

    public void setDocModificadoNumero(String docModificadoNumero) {
        this.docModificadoNumero = docModificadoNumero;
    }

    public Instant getDocModificadoFecha() {
        return docModificadoFecha;
    }

    public void setDocModificadoFecha(Instant docModificadoFecha) {
        this.docModificadoFecha = docModificadoFecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getPeriodoFiscal() {
        return periodoFiscal;
    }

    public void setPeriodoFiscal(String periodoFiscal) {
        this.periodoFiscal = periodoFiscal;
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

    public String getDirPartida() {
        return dirPartida;
    }

    public void setDirPartida(String dirPartida) {
        this.dirPartida = dirPartida;
    }

    public Instant getFechaIniTransporte() {
        return fechaIniTransporte;
    }

    public void setFechaIniTransporte(Instant fechaIniTransporte) {
        this.fechaIniTransporte = fechaIniTransporte;
    }

    public Instant getFechaFinTransporte() {
        return fechaFinTransporte;
    }

    public void setFechaFinTransporte(Instant fechaFinTransporte) {
        this.fechaFinTransporte = fechaFinTransporte;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public List<DestinatarioGuiaRemisionResponse> getDestinatarios() {
        return destinatarios;
    }

    public void setDestinatarios(List<DestinatarioGuiaRemisionResponse> destinatarios) {
        this.destinatarios = destinatarios;
    }

    public List<DetalleComprobanteResponse> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleComprobanteResponse> detalles) {
        this.detalles = detalles;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
