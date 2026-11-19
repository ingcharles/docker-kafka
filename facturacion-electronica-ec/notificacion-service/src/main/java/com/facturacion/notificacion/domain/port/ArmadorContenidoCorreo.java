package com.facturacion.notificacion.domain.port;

import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.vo.ContenidoCorreo;
import com.facturacion.notificacion.domain.model.vo.PlantillaPorDefecto;

/** Puerto de salida: arma el asunto y el cuerpo del correo de un comprobante. */
public interface ArmadorContenidoCorreo {

    /**
     * El correo de un comprobante: con la plantilla propia de la empresa si tiene una activa y se puede aplicar, y si no con la
     * plantilla por defecto. Nunca falla por una plantilla de empresa defectuosa: usa la de por defecto.
     */
    ContenidoCorreo armar(CorreoSalida.Comprobante comprobante);

    /** La plantilla por defecto de un tipo, con las variables {@code {{...}}} sin reemplazar, para usarla de punto de partida. */
    PlantillaPorDefecto exportarPorDefecto(TipoComprobanteCorreo tipo);
}
