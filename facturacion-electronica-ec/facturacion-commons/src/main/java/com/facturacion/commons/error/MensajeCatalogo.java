package com.facturacion.commons.error;

/**
 * Una entrada del catálogo con código estable y texto en un {@code .properties}. Es lo mínimo que necesita
 * {@link Mensajes}; los errores que se devuelven por HTTP usan la extensión {@link ErrorCatalogo}, que añade el
 * estado. Lo implementan también catálogos sin respuesta HTTP (p. ej. los motivos de la saga del orquestador).
 */
public interface MensajeCatalogo {

    /** Código estable que ve el cliente o la bitácora; no cambia aunque cambie el texto. */
    String codigo();

    /** Clave del texto en {@code mensajes.properties} (del servicio) o {@code mensajes-comunes.properties}. */
    String claveMensaje();
}
