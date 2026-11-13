package com.facturacion.comprobantes.application.service.construccion;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;

/**
 * Arma el agregado {@link Comprobante} de UN tipo de comprobante a partir de su request (un implementador por tipo:
 * factura, nota de crédito, nota de débito, guía de remisión, retención y liquidación de compra). Agregar un tipo
 * nuevo es agregar una clase, sin tocar el flujo de emisión de {@code ComprobanteService}.
 *
 * @param <R> el request propio de ese tipo
 */
public interface ConstructorComprobante<R> {

    /**
     * @param secuencial ya reservado (atómicamente) para este comprobante
     * @param catalogos  los catálogos del SRI evaluados a la fecha de emisión
     */
    Uni<Comprobante> construir(R request, long secuencial, CatalogosSriAFecha catalogos);
}
