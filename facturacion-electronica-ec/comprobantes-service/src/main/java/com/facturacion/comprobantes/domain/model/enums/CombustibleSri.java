package com.facturacion.comprobantes.domain.model.enums;

import java.util.List;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Combustibles líquidos derivados de hidrocarburos y biocombustibles, con el {@code codigoPrincipal} que el Anexo 16
 * (Tabla 30) fija para la línea de la factura. Una factura con alguno de estos códigos es una venta de combustible y,
 * por el Anexo 12, debe llevar la placa.
 */
public enum CombustibleSri {

    SUPER("0103", "SÚPER"),
    EXTRA("0101", "EXTRA"),
    EXTRA_CON_ETANOL("0174", "EXTRA CON ETANOL"),
    DIESEL_PREMIUM("0121", "DIESEL PREMIUM"),
    DIESEL_2("0104", "DIESEL 2");

    private final String codigo;
    private final String descripcion;

    CombustibleSri(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String codigo() {
        return codigo;
    }

    public String descripcion() {
        return descripcion;
    }

    public static boolean esCodigo(String codigoPrincipal) {
        for (CombustibleSri combustible : values()) {
            if (combustible.codigo.equals(codigoPrincipal)) {
                return true;
            }
        }
        return false;
    }

    /** Venta de combustible sin placa: el SRI la rechaza (Anexo 12), así que se corta antes de generar el XML. */
    public static void exigirPlacaSiHayCombustible(List<DetalleComprobante> detalles, String placa) {
        if (placa != null && !placa.isBlank()) {
            return;
        }
        for (DetalleComprobante detalle : detalles) {
            if (esCodigo(detalle.getCodigoPrincipal())) {
                throw new ErrorNegocioException(CodigoError.COMBUSTIBLE_SIN_PLACA, detalle.getCodigoPrincipal());
            }
        }
    }
}
