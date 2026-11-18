package com.facturacion.comprobantes.domain.service;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;

/**
 * Puerto de generación de XML — un adaptador por tipo de comprobante (Anexo
 * 1/3/9/10/17 de la ficha técnica, uno por cada uno de los 6 tipos). Hoy
 * solo existe el adaptador de Factura; los otros 5 tipos comparten esta
 * misma interfaz y el mismo {@code Comprobante}/{@code DetalleComprobante}
 * genéricos — agregarlos es un adaptador nuevo, no un rediseño del dominio.
 */
public interface GeneradorXmlComprobante {

    boolean esTipoSoportado(TipoComprobante tipo);

    /** El XML sin firmar, listo para pasar a firma-electronica-service. */
    String generar(Comprobante comprobante);
}
