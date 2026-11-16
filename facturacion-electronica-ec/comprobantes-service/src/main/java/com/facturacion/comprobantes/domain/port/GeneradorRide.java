package com.facturacion.comprobantes.domain.port;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;

/** Puerto de salida: genera el RIDE (representación impresa, PDF) del comprobante. */
public interface GeneradorRide {

    /**
     * @param colorRide color hex opcional del número del documento
     * @param logo      imagen PNG/JPEG opcional del emisor (la que la empresa subió); {@code null} si no tiene
     */
    byte[] generar(Comprobante comprobante, String colorRide, byte[] logo, CatalogosSriAFecha catalogos);
}
