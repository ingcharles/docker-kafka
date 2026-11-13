package com.facturacion.comprobantes.domain.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Los catálogos del SRI (tablas propias de este servicio, ver ARQUITECTURA.md 7.6) tal como están
 * y evaluados A UNA FECHA — la de emisión del comprobante. Es inmutable y no toca la base: las
 * validaciones de {@code CatalogosValidador} y {@code PagosValidador} operan
 * sobre ella en memoria.
 * <p>
 * Convención de "catálogo cargado": los {@code hayXxx()} dicen si la tabla
 * tiene filas. Los catálogos que dependen de datos que la documentación del
 * proyecto no trae (códigos de Renta, codSustento) pueden estar vacíos; en
 * ese caso el validador solo comprueba el formato. Un registro aplica en la
 * fecha F si está activo y {@code desde <= F <= hasta (o sin hasta)}.
 */
public final class CatalogosSriAFecha {

    public record FormaPagoRow(String codigo, String descripcion, LocalDate desde, LocalDate hasta,
            boolean activo) {
    }

    public record ImpuestoRow(String codigo, String descripcion, boolean activo) {
    }

    public record TarifaRow(String impuestoCodigo, String codigoPorcentaje, String descripcion, BigDecimal tarifa,
            LocalDate desde, LocalDate hasta, boolean activo) {
    }

    public record TipoIdentificacionRow(String codigo, String descripcion, boolean soloFactura, boolean activo) {
    }

    public record RetencionRow(String impuestoCodigo, String codigo, LocalDate desde, String descripcion,
            BigDecimal porcentaje, LocalDate hasta, boolean activo) {
    }

    public record SustentoRow(String codigo, String descripcion, boolean activo) {
    }

    private final LocalDate fecha;
    private final List<FormaPagoRow> formasPago;
    private final List<ImpuestoRow> impuestos;
    private final List<TarifaRow> tarifas;
    private final List<TipoIdentificacionRow> tiposIdentificacion;
    private final List<ImpuestoRow> impuestosRetencion;
    private final List<RetencionRow> retenciones;
    private final List<SustentoRow> sustentos;

    public CatalogosSriAFecha(LocalDate fecha, List<FormaPagoRow> formasPago, List<ImpuestoRow> impuestos,
            List<TarifaRow> tarifas, List<TipoIdentificacionRow> tiposIdentificacion,
            List<ImpuestoRow> impuestosRetencion, List<RetencionRow> retenciones, List<SustentoRow> sustentos) {
        this.fecha = fecha;
        this.formasPago = List.copyOf(formasPago);
        this.impuestos = List.copyOf(impuestos);
        this.tarifas = List.copyOf(tarifas);
        this.tiposIdentificacion = List.copyOf(tiposIdentificacion);
        this.impuestosRetencion = List.copyOf(impuestosRetencion);
        this.retenciones = List.copyOf(retenciones);
        this.sustentos = List.copyOf(sustentos);
    }

    public LocalDate fecha() {
        return fecha;
    }

    private boolean vigente(boolean activo, LocalDate desde, LocalDate hasta) {
        return activo && !desde.isAfter(fecha) && (hasta == null || !hasta.isBefore(fecha));
    }

    // ---- Tabla 24: formas de pago ----

    /** Códigos de forma de pago activos y vigentes a la fecha de emisión. */
    public Set<String> formasPagoVigentes() {
        return formasPago.stream().filter(f -> vigente(f.activo(), f.desde(), f.hasta()))
            .map(FormaPagoRow::codigo).collect(Collectors.toCollection(TreeSet::new));
    }

    /** Descripción para el RIDE; un código desconocido (dato histórico) se muestra como "FORMA DE PAGO xx". */
    public String descripcionFormaPago(String codigo) {
        return formasPago.stream().filter(f -> f.codigo().equals(codigo)).map(FormaPagoRow::descripcion)
            .findFirst().orElse("FORMA DE PAGO " + codigo);
    }

    // ---- Tabla 16/17: impuestos y tarifas ----

    public boolean hayImpuestos() {
        return !impuestos.isEmpty();
    }

    public Set<String> codigosImpuesto() {
        return impuestos.stream().filter(ImpuestoRow::activo).map(ImpuestoRow::codigo)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean impuestoExiste(String codigo) {
        return impuestos.stream().anyMatch(i -> i.activo() && i.codigo().equals(codigo));
    }

    /** ¿Hay tarifas cargadas para este impuesto? (el ICE y el IRBPNR no las tienen: su tarifa viaja con el ítem/request) */
    public boolean hayTarifas(String impuestoCodigo) {
        return tarifas.stream().anyMatch(t -> t.impuestoCodigo().equals(impuestoCodigo));
    }

    public Set<String> codigosPorcentaje(String impuestoCodigo) {
        return tarifas.stream().filter(t -> t.impuestoCodigo().equals(impuestoCodigo)
                && vigente(t.activo(), t.desde(), t.hasta()))
            .map(TarifaRow::codigoPorcentaje).collect(Collectors.toCollection(TreeSet::new));
    }

    public Optional<BigDecimal> tarifa(String impuestoCodigo, String codigoPorcentaje) {
        return tarifas.stream().filter(t -> t.impuestoCodigo().equals(impuestoCodigo)
                && t.codigoPorcentaje().equals(codigoPorcentaje) && vigente(t.activo(), t.desde(), t.hasta()))
            .map(TarifaRow::tarifa).findFirst();
    }

    // ---- Tabla 6: tipos de identificación ----

    public boolean hayTiposIdentificacion() {
        return !tiposIdentificacion.isEmpty();
    }

    public Set<String> codigosTipoIdentificacion() {
        return tiposIdentificacion.stream().filter(TipoIdentificacionRow::activo)
            .map(TipoIdentificacionRow::codigo).collect(Collectors.toCollection(TreeSet::new));
    }

    public Optional<TipoIdentificacionRow> tipoIdentificacion(String codigo) {
        return tiposIdentificacion.stream().filter(t -> t.activo() && t.codigo().equals(codigo)).findFirst();
    }

    // ---- Tabla 19/20: impuestos a retener y códigos de retención ----

    public boolean hayImpuestosRetencion() {
        return !impuestosRetencion.isEmpty();
    }

    public boolean impuestoRetencionExiste(String codigo) {
        return impuestosRetencion.stream().anyMatch(i -> i.activo() && i.codigo().equals(codigo));
    }

    public Set<String> codigosImpuestoRetencion() {
        return impuestosRetencion.stream().filter(ImpuestoRow::activo).map(ImpuestoRow::codigo)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean hayRetenciones(String impuestoCodigo) {
        return retenciones.stream().anyMatch(r -> r.impuestoCodigo().equals(impuestoCodigo));
    }

    /** ¿Existe el código para ese impuesto en alguna vigencia (aunque hoy no aplique)? */
    public boolean retencionExiste(String impuestoCodigo, String codigo) {
        return retenciones.stream().anyMatch(r -> r.impuestoCodigo().equals(impuestoCodigo)
            && r.codigo().equals(codigo));
    }

    public Optional<RetencionRow> retencionVigente(String impuestoCodigo, String codigo) {
        return retenciones.stream().filter(r -> r.impuestoCodigo().equals(impuestoCodigo)
                && r.codigo().equals(codigo) && vigente(r.activo(), r.desde(), r.hasta()))
            .findFirst();
    }

    public Set<String> codigosRetencionVigentes(String impuestoCodigo) {
        return retenciones.stream().filter(r -> r.impuestoCodigo().equals(impuestoCodigo)
                && vigente(r.activo(), r.desde(), r.hasta()))
            .map(RetencionRow::codigo).collect(Collectors.toCollection(TreeSet::new));
    }

    // ---- codSustento ----

    public boolean haySustentos() {
        return !sustentos.isEmpty();
    }

    public boolean sustentoExiste(String codigo) {
        return sustentos.stream().anyMatch(s -> s.activo() && s.codigo().equals(codigo));
    }

    public Set<String> codigosSustento() {
        return sustentos.stream().filter(SustentoRow::activo).map(SustentoRow::codigo)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
