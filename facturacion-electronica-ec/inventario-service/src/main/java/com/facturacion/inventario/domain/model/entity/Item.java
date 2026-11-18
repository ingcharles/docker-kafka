package com.facturacion.inventario.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.facturacion.inventario.domain.model.enums.TipoItem;

/**
 * Ítem facturable — un producto o un servicio, según {@link #tipo}. Los
 * campos de existencias ({@code cantidadDisponible}, {@code cantidadReservada},
 * {@code stockMinimo}) solo tienen sentido para {@link TipoItem#PRODUCTO};
 * en un {@link TipoItem#SERVICIO} se mantienen en cero y se ignoran — un
 * servicio no se agota.
 */
public class Item {

    private UUID id;
    /** RUC de la empresa dueña de este ítem — el catálogo es multi-tenant, cada ítem pertenece a una sola empresa. */
    private String ruc;
    private TipoItem tipo;
    private UUID categoriaId;
    private String codigoPrincipal;
    private String codigoAuxiliar;
    private String descripcion;
    private BigDecimal precioUnitario;
    private String unidadMedida;
    private BigDecimal cantidadDisponible;
    private BigDecimal cantidadReservada;
    private BigDecimal stockMinimo;
    private boolean activo;

    // Clasificación tributaria: fuente de verdad para que comprobantes-service tome los impuestos cuando una línea solo
    // trae itemId. Solo códigos (Tablas 17 y 18): la tarifa y su tipo salen del catálogo a la fecha de emisión.
    private String codigoPorcentajeIva;
    /** Código de ICE (Tabla 18) o null si el ítem no lleva ICE. */
    private String codigoIce;
    /** Código de IRBPNR (5001) o null si no lo lleva. */
    private String codigoIrbpnr;

    private Instant creadoEn;
    private Instant actualizadoEn;

    public Item() {
    }

    public Item(UUID id, String ruc, TipoItem tipo, UUID categoriaId, String codigoPrincipal, String codigoAuxiliar,
            String descripcion, BigDecimal precioUnitario, String unidadMedida, BigDecimal cantidadDisponible,
            BigDecimal cantidadReservada, BigDecimal stockMinimo, boolean activo,
            String codigoPorcentajeIva, String codigoIce, String codigoIrbpnr, Instant creadoEn,
            Instant actualizadoEn) {
        this.id = id;
        this.ruc = ruc;
        this.tipo = tipo;
        this.categoriaId = categoriaId;
        this.codigoPrincipal = codigoPrincipal;
        this.codigoAuxiliar = codigoAuxiliar;
        this.descripcion = descripcion;
        this.precioUnitario = precioUnitario;
        this.unidadMedida = unidadMedida;
        this.cantidadDisponible = cantidadDisponible;
        this.cantidadReservada = cantidadReservada;
        this.stockMinimo = stockMinimo;
        this.activo = activo;
        this.codigoPorcentajeIva = codigoPorcentajeIva;
        this.codigoIce = codigoIce;
        this.codigoIrbpnr = codigoIrbpnr;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public boolean esProducto() {
        return tipo == TipoItem.PRODUCTO;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public TipoItem getTipo() {
        return tipo;
    }

    public void setTipo(TipoItem tipo) {
        this.tipo = tipo;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(UUID categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCodigoPrincipal() {
        return codigoPrincipal;
    }

    public void setCodigoPrincipal(String codigoPrincipal) {
        this.codigoPrincipal = codigoPrincipal;
    }

    public String getCodigoAuxiliar() {
        return codigoAuxiliar;
    }

    public void setCodigoAuxiliar(String codigoAuxiliar) {
        this.codigoAuxiliar = codigoAuxiliar;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public BigDecimal getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(BigDecimal cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public BigDecimal getCantidadReservada() {
        return cantidadReservada;
    }

    public void setCantidadReservada(BigDecimal cantidadReservada) {
        this.cantidadReservada = cantidadReservada;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getCodigoPorcentajeIva() {
        return codigoPorcentajeIva;
    }

    public void setCodigoPorcentajeIva(String codigoPorcentajeIva) {
        this.codigoPorcentajeIva = codigoPorcentajeIva;
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

    public String getCodigoIce() {
        return codigoIce;
    }

    public void setCodigoIce(String codigoIce) {
        this.codigoIce = codigoIce;
    }

    public String getCodigoIrbpnr() {
        return codigoIrbpnr;
    }

    public void setCodigoIrbpnr(String codigoIrbpnr) {
        this.codigoIrbpnr = codigoIrbpnr;
    }
}
