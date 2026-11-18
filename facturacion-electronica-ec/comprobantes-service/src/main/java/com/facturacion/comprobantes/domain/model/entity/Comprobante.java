package com.facturacion.comprobantes.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.RegimenRimpe;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.DocumentoSustento;
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

    // Datos de la empresa recibidos inline en el request de creación — este
    // servicio ya no mantiene una copia local de Empresa (ver ARQUITECTURA.md,
    // "Empresa sin ID"); no hay ningún empresaId de firma-electronica-service.
    private String empresaRuc;
    private String empresaRazonSocial;
    private String empresaNombreComercial;
    private String empresaDireccionMatriz;
    private boolean empresaObligadoContabilidad;
    private boolean empresaContribuyenteEspecial;
    private String empresaNumeroContribuyenteEspecial;
    private boolean empresaEsGranContribuyente;
    private String empresaNumeroResolucionGranContribuyente;
    /** Anexo 21: número de resolución con que fue designado agente de retención (sin ceros a la izquierda). */
    private String empresaAgenteRetencion;
    /** Anexo 22: régimen RIMPE del emisor; {@code null} si no aplica. */
    private RegimenRimpe empresaRegimenRimpe;
    /** RUC del proveedor de software/servicio de facturación electrónica (Anexo 26, Res. NAC-DGERCGC26-00000027) — {@code configuracion_empresa}, clave {@code sri.proveedor.ruc}; {@code null} si la empresa no la configuró. */
    private String proveedorRuc;

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
    /** "SI"|"NO" — si el sujeto retenido es parte relacionada (XSD v2.0.0, {@code infoCompRetencion.parteRel}); null en los otros 5 tipos. */
    private String parteRelacionada;
    /**
     * Un {@code docSustento} por documento retenido (XSD v2.0.0), cada uno
     * con su propio impuesto y forma de pago — transitorio: solo se usa
     * para generar el XML dentro de {@code ComprobanteService.construirRetencion},
     * no se persiste (ver {@code ComprobanteMapper}); vacío en los otros 5 tipos.
     */
    private List<DocumentoSustento> documentosSustento = List.of();

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

    public String getEmpresaRuc() {
        return empresaRuc;
    }

    public void setEmpresaRuc(String empresaRuc) {
        this.empresaRuc = empresaRuc;
    }

    public String getEmpresaRazonSocial() {
        return empresaRazonSocial;
    }

    public void setEmpresaRazonSocial(String empresaRazonSocial) {
        this.empresaRazonSocial = empresaRazonSocial;
    }

    public String getEmpresaNombreComercial() {
        return empresaNombreComercial;
    }

    public void setEmpresaNombreComercial(String empresaNombreComercial) {
        this.empresaNombreComercial = empresaNombreComercial;
    }

    public String getEmpresaDireccionMatriz() {
        return empresaDireccionMatriz;
    }

    public void setEmpresaDireccionMatriz(String empresaDireccionMatriz) {
        this.empresaDireccionMatriz = empresaDireccionMatriz;
    }

    public boolean isEmpresaObligadoContabilidad() {
        return empresaObligadoContabilidad;
    }

    public void setEmpresaObligadoContabilidad(boolean empresaObligadoContabilidad) {
        this.empresaObligadoContabilidad = empresaObligadoContabilidad;
    }

    public boolean isEmpresaContribuyenteEspecial() {
        return empresaContribuyenteEspecial;
    }

    public void setEmpresaContribuyenteEspecial(boolean empresaContribuyenteEspecial) {
        this.empresaContribuyenteEspecial = empresaContribuyenteEspecial;
    }

    public String getEmpresaNumeroContribuyenteEspecial() {
        return empresaNumeroContribuyenteEspecial;
    }

    public void setEmpresaNumeroContribuyenteEspecial(String empresaNumeroContribuyenteEspecial) {
        this.empresaNumeroContribuyenteEspecial = empresaNumeroContribuyenteEspecial;
    }

    public boolean isEmpresaEsGranContribuyente() {
        return empresaEsGranContribuyente;
    }

    public void setEmpresaEsGranContribuyente(boolean empresaEsGranContribuyente) {
        this.empresaEsGranContribuyente = empresaEsGranContribuyente;
    }

    public String getEmpresaNumeroResolucionGranContribuyente() {
        return empresaNumeroResolucionGranContribuyente;
    }

    public void setEmpresaNumeroResolucionGranContribuyente(String empresaNumeroResolucionGranContribuyente) {
        this.empresaNumeroResolucionGranContribuyente = empresaNumeroResolucionGranContribuyente;
    }

    public String getEmpresaAgenteRetencion() {
        return empresaAgenteRetencion;
    }

    public void setEmpresaAgenteRetencion(String empresaAgenteRetencion) {
        this.empresaAgenteRetencion = empresaAgenteRetencion;
    }

    public RegimenRimpe getEmpresaRegimenRimpe() {
        return empresaRegimenRimpe;
    }

    public void setEmpresaRegimenRimpe(RegimenRimpe empresaRegimenRimpe) {
        this.empresaRegimenRimpe = empresaRegimenRimpe;
    }

    public String getProveedorRuc() {
        return proveedorRuc;
    }

    public void setProveedorRuc(String proveedorRuc) {
        this.proveedorRuc = proveedorRuc;
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

    public String getParteRelacionada() {
        return parteRelacionada;
    }

    public void setParteRelacionada(String parteRelacionada) {
        this.parteRelacionada = parteRelacionada;
    }

    public List<DocumentoSustento> getDocumentosSustento() {
        return documentosSustento;
    }

    public void setDocumentosSustento(List<DocumentoSustento> documentosSustento) {
        this.documentosSustento = documentosSustento;
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

    // --- Ciclo de vida --------------------------------------------------------------------------------------------
    // Las transiciones viven en el agregado: cada una valida que el cambio sea legal (EstadoComprobante.puedePasarA)
    // y deja juntos el estado y los datos que le corresponden, para que ningún servicio los combine mal.

    /** Si el ciclo de vida permite pasar del estado actual a {@code destino}. */
    public boolean puedePasarA(EstadoComprobante destino) {
        return estado.puedePasarA(destino);
    }

    /** Si {@code destino} está adelante en el ciclo pero se saltaría pasos intermedios (ver {@link EstadoComprobante}). */
    public boolean esSaltoDePasos(EstadoComprobante destino) {
        return estado.esSaltoDePasos(destino);
    }

    /** BORRADOR → FIRMADO: guarda el XML firmado devuelto por firma-electronica-service. */
    public void marcarFirmado(String xmlFirmado) {
        exigirTransicionA(EstadoComprobante.FIRMADO);
        this.xmlFirmado = xmlFirmado;
        this.estado = EstadoComprobante.FIRMADO;
    }

    /** BORRADOR → FIRMA_FALLIDA: no se pudo firmar; el motivo queda para corregir y reintentar. */
    public void marcarFirmaFallida(String motivo) {
        exigirTransicionA(EstadoComprobante.FIRMA_FALLIDA);
        this.motivoRechazo = motivo;
        this.estado = EstadoComprobante.FIRMA_FALLIDA;
    }

    /** BORRADOR → STOCK_INSUFICIENTE: el inventario no pudo reservar el stock; el motivo indica los ítems. */
    public void marcarStockInsuficiente(String motivo) {
        exigirTransicionA(EstadoComprobante.STOCK_INSUFICIENTE);
        this.motivoRechazo = motivo;
        this.estado = EstadoComprobante.STOCK_INSUFICIENTE;
    }

    /** FIRMADO → NO_AUTORIZADO: el SRI lo rechazó; se corrige y se reenvía con la misma clave de acceso. */
    public void marcarNoAutorizado(String motivo) {
        exigirTransicionA(EstadoComprobante.NO_AUTORIZADO);
        this.motivoRechazo = motivo;
        this.estado = EstadoComprobante.NO_AUTORIZADO;
    }

    /** FIRMADO → AUTORIZADO: guarda el bloque {@code <autorizacion>} completo y los datos de la autorización. */
    public void marcarAutorizado(String xmlAutorizado, String numeroAutorizacion, Instant fechaAutorizacion) {
        exigirTransicionA(EstadoComprobante.AUTORIZADO);
        this.xmlAutorizado = xmlAutorizado;
        this.numeroAutorizacion = numeroAutorizacion;
        this.fechaAutorizacion = fechaAutorizacion;
        this.estado = EstadoComprobante.AUTORIZADO;
    }

    /** Adjunta el RIDE; solo tiene sentido con el comprobante autorizado (recién entonces hay número y fecha reales). */
    public void adjuntarRide(byte[] ridePdf) {
        if (estado != EstadoComprobante.AUTORIZADO) {
            throw new IllegalStateException("El RIDE solo se adjunta a un comprobante AUTORIZADO; estado actual: " + estado);
        }
        this.ridePdf = ridePdf;
    }

    /** AUTORIZADO → NOTIFICADO: el comprobante ya se envió por correo al receptor. */
    public void marcarNotificado() {
        exigirTransicionA(EstadoComprobante.NOTIFICADO);
        this.estado = EstadoComprobante.NOTIFICADO;
    }

    private void exigirTransicionA(EstadoComprobante destino) {
        if (!estado.puedePasarA(destino)) {
            throw new IllegalStateException(
                "El comprobante " + id + " está " + estado + " y no puede pasar a " + destino);
        }
    }
}
