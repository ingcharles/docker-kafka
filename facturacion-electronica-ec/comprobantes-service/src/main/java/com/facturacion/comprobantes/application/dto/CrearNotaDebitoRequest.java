package com.facturacion.comprobantes.application.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Cobra un valor adicional sobre un comprobante ya emitido (intereses, mora,
 * etc. — Anexo de Nota de Débito). A diferencia de Factura/Nota de Crédito
 * no tiene {@code <detalles>} de ítems, sino {@code <motivos>} (razón +
 * valor) y un único bloque {@code <impuestos>} a nivel de encabezado
 * calculado sobre la suma de los motivos.
 */
public class CrearNotaDebitoRequest {

    @NotNull
    @Valid
    private EmpresaRequest empresa;

    @NotBlank
    private String compradorTipoIdentificacion;

    @NotBlank
    private String compradorIdentificacion;

    @NotBlank
    private String compradorRazonSocial;

    private String compradorDireccion;

    private String compradorCorreo;

    @NotBlank
    private String docModificadoTipo;

    @NotBlank
    private String docModificadoNumero;

    /** dd/mm/aaaa */
    @NotBlank
    private String docModificadoFecha;

    private String moneda = "DOLAR";

    @NotEmpty
    @Valid
    private List<MotivoNotaDebitoRequest> motivos;

    /** Impuestos calculados sobre la suma de {@code motivos} — puede ir vacío si el cobro no genera impuesto. */
    @Valid
    private List<ImpuestoRequest> impuestos = List.of();

    /** Opcional — si no se manda, se asume una sola forma de pago "01" por el valor total. */
    @Valid
    private List<PagoRequest> pagos;

    @Valid
    private List<CampoAdicionalRequest> campoAdicional = List.of();

    public EmpresaRequest getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaRequest empresa) {
        this.empresa = empresa;
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

    public String getDocModificadoFecha() {
        return docModificadoFecha;
    }

    public void setDocModificadoFecha(String docModificadoFecha) {
        this.docModificadoFecha = docModificadoFecha;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public List<MotivoNotaDebitoRequest> getMotivos() {
        return motivos;
    }

    public void setMotivos(List<MotivoNotaDebitoRequest> motivos) {
        this.motivos = motivos;
    }

    public List<ImpuestoRequest> getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(List<ImpuestoRequest> impuestos) {
        this.impuestos = impuestos;
    }

    public List<PagoRequest> getPagos() {
        return pagos;
    }

    public void setPagos(List<PagoRequest> pagos) {
        this.pagos = pagos;
    }

    public List<CampoAdicionalRequest> getCampoAdicional() {
        return campoAdicional;
    }

    public void setCampoAdicional(List<CampoAdicionalRequest> campoAdicional) {
        this.campoAdicional = campoAdicional;
    }
}
