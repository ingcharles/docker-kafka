package com.facturacion.comprobantes.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.Pago;

/**
 * El agregado central de este servicio. Guarda el XML en cada una de sus
 * tres etapas ({@link #xmlGenerado}, {@link #xmlFirmado}, {@link #xmlAutorizado})
 * y el RIDE en PDF una vez autorizado — el historial completo de cada
 * documento queda auditable sin volver a pedirle nada a otro servicio. Ver
 * {@code EstadoComprobante} para qué campo se llena en cada transición.
 */
public class Comprobante {

    private UUID id;
    private TipoComprobante tipoComprobante;
    private String claveAcceso;
    private AmbienteSri ambiente;
    private String establecimiento;
    private String puntoEmision;
    private String secuencial;

    // Datos del emisor recibidos inline en el request de creación — este
    // servicio ya no mantiene una copia local de Emisor (ver ARQUITECTURA.md,
    // "Emisor sin ID"); no hay ningún emisorId de firma-electronica-service.
    private String emisorRuc;
    private String emisorRazonSocial;
    private String emisorNombreComercial;
    private String emisorDireccionMatriz;
    private boolean emisorObligadoContabilidad;
    private boolean emisorContribuyenteEspecial;
    private String emisorNumeroContribuyenteEspecial;
    private boolean emisorEsGranContribuyente;
    private String emisorNumeroResolucionGranContribuyente;

    private Instant fechaEmision;
    private String compradorTipoIdentificacion;
    private String compradorIdentificacion;
    private String compradorRazonSocial;
    private String compradorDireccion;
    /** A quién le manda notificacion-service el comprobante autorizado — null significa "no notificar por correo". */
    private String compradorCorreo;

    private BigDecimal subtotalSinImpuestos;
    private BigDecimal totalDescuento;
    private BigDecimal importeTotal;
    private String moneda;

    private EstadoComprobante estado;

    private String xmlGenerado;
    private String xmlFirmado;
    private String xmlAutorizado;
    private String numeroAutorizacion;
    private Instant fechaAutorizacion;
    private String motivoRechazo;
    private byte[] ridePdf;

    // Campos específicos de los 5 tipos que no son Factura — quedan en null
    // salvo para el tipo que los usa, ver GeneradorXmlComprobante por tipo.
    private String direccionEstablecimiento;

    // Nota de Crédito / Nota de Débito: comprobante que se modifica.
    private String docModificadoTipo;
    private String docModificadoNumero;
    private Instant docModificadoFecha;

    // Nota de Crédito: motivo general de la modificación — obligatorio
    // según el XSD real del SRI, aunque no aparecía claro como tal en la
    // ficha técnica en texto (ver V4__motivo_nota_credito.sql).
    private String motivo;

    // Retención.
    private String periodoFiscal;

    // Guía de Remisión.
    private String transportistaRazonSocial;
    private String transportistaTipoIdentificacion;
    private String transportistaRuc;
    private String dirPartida;
    private Instant fechaIniTransporte;
    private Instant fechaFinTransporte;
    private String placa;
    private List<DestinatarioGuiaRemision> destinatarios;

    private List<DetalleComprobante> detalles;

    /** Solo Factura/Nota de Débito/Liquidación de Compra — null en los otros 3 tipos (ver Pago). */
    private List<Pago> pagos = List.of();

    private List<CampoAdicional> camposAdicionales = List.of();

    private Instant creadoEn;
    private Instant actualizadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TipoComprobante getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(TipoComprobante tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getClaveAcceso() {
        return claveAcceso;
    }

    public void setClaveAcceso(String claveAcceso) {
        this.claveAcceso = claveAcceso;
    }

    public AmbienteSri getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(AmbienteSri ambiente) {
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

    public String getEmisorNombreComercial() {
        return emisorNombreComercial;
    }

    public void setEmisorNombreComercial(String emisorNombreComercial) {
        this.emisorNombreComercial = emisorNombreComercial;
    }

    public String getEmisorDireccionMatriz() {
        return emisorDireccionMatriz;
    }

    public void setEmisorDireccionMatriz(String emisorDireccionMatriz) {
        this.emisorDireccionMatriz = emisorDireccionMatriz;
    }

    public boolean isEmisorObligadoContabilidad() {
        return emisorObligadoContabilidad;
    }

    public void setEmisorObligadoContabilidad(boolean emisorObligadoContabilidad) {
        this.emisorObligadoContabilidad = emisorObligadoContabilidad;
    }

    public boolean isEmisorContribuyenteEspecial() {
        return emisorContribuyenteEspecial;
    }

    public void setEmisorContribuyenteEspecial(boolean emisorContribuyenteEspecial) {
        this.emisorContribuyenteEspecial = emisorContribuyenteEspecial;
    }

    public String getEmisorNumeroContribuyenteEspecial() {
        return emisorNumeroContribuyenteEspecial;
    }

    public void setEmisorNumeroContribuyenteEspecial(String emisorNumeroContribuyenteEspecial) {
        this.emisorNumeroContribuyenteEspecial = emisorNumeroContribuyenteEspecial;
    }

    public boolean isEmisorEsGranContribuyente() {
        return emisorEsGranContribuyente;
    }

    public void setEmisorEsGranContribuyente(boolean emisorEsGranContribuyente) {
        this.emisorEsGranContribuyente = emisorEsGranContribuyente;
    }

    public String getEmisorNumeroResolucionGranContribuyente() {
        return emisorNumeroResolucionGranContribuyente;
    }

    public void setEmisorNumeroResolucionGranContribuyente(String emisorNumeroResolucionGranContribuyente) {
        this.emisorNumeroResolucionGranContribuyente = emisorNumeroResolucionGranContribuyente;
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

    public EstadoComprobante getEstado() {
        return estado;
    }

    public void setEstado(EstadoComprobante estado) {
        this.estado = estado;
    }

    public String getXmlGenerado() {
        return xmlGenerado;
    }

    public void setXmlGenerado(String xmlGenerado) {
        this.xmlGenerado = xmlGenerado;
    }

    public String getXmlFirmado() {
        return xmlFirmado;
    }

    public void setXmlFirmado(String xmlFirmado) {
        this.xmlFirmado = xmlFirmado;
    }

    public String getXmlAutorizado() {
        return xmlAutorizado;
    }

    public void setXmlAutorizado(String xmlAutorizado) {
        this.xmlAutorizado = xmlAutorizado;
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

    public byte[] getRidePdf() {
        return ridePdf;
    }

    public void setRidePdf(byte[] ridePdf) {
        this.ridePdf = ridePdf;
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

    public List<DestinatarioGuiaRemision> getDestinatarios() {
        return destinatarios;
    }

    public void setDestinatarios(List<DestinatarioGuiaRemision> destinatarios) {
        this.destinatarios = destinatarios;
    }

    public List<DetalleComprobante> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleComprobante> detalles) {
        this.detalles = detalles;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public void setPagos(List<Pago> pagos) {
        this.pagos = pagos;
    }

    public List<CampoAdicional> getCamposAdicionales() {
        return camposAdicionales;
    }

    public void setCamposAdicionales(List<CampoAdicional> camposAdicionales) {
        this.camposAdicionales = camposAdicionales;
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
