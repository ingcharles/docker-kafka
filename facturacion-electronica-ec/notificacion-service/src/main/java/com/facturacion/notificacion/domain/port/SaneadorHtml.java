package com.facturacion.notificacion.domain.port;

/** Puerto de salida: deja en un HTML solo lo que es seguro mandar por correo. */
public interface SaneadorHtml {

    /**
     * Devuelve el HTML sin lo peligroso: scripts, formularios, marcos, manejadores de eventos ({@code onclick}...), estilos
     * con valores fuera de lista y direcciones que no sean https, mailto o cid. Conserva el texto, las tablas, los estilos en
     * línea seguros, los enlaces y las imágenes.
     */
    String sanear(String html);
}
