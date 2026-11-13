package com.facturacion.commons.error;

/**
 * Entrada del catálogo de errores HTTP: {@code codigo} estable + {@code estadoHttp} + texto. Cada servicio declara
 * sus errores en un enum que implementa esta interfaz (p. ej. {@code CodigoError}), y {@link ErrorComun} trae los
 * genéricos (404, 401, 403, 409, 500...). Una excepción ({@code ApiException}) lleva su entrada y el
 * {@code ApiExceptionMapper} la convierte en la respuesta; ningún código arma respuestas HTTP a mano.
 * <p>
 * Convención del código: {@code <PREFIJO>-<ESTADO>-<NNN>} (p. ej. {@code COMP-400-101}); el estado del código debe
 * coincidir con {@link #estadoHttp()} (lo verifica {@code CatalogoErroresTest}).
 */
public interface ErrorCatalogo extends MensajeCatalogo {

    /** Estado HTTP de la respuesta (400, 401, 403, 404, 409, 500...). */
    int estadoHttp();
}
