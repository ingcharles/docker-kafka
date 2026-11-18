package com.facturacion.comprobantes.domain.port;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.FormatoTicket;
import com.facturacion.comprobantes.domain.model.vo.AnchoTicket;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;

/** Puerto de salida: arma el ticket (recibo corto para impresora térmica) con los datos mínimos del comprobante. */
public interface GeneradorTicket {

    byte[] generar(Comprobante comprobante, FormatoTicket formato, AnchoTicket ancho, CatalogosSriAFecha catalogos);
}
