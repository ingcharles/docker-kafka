package com.facturacion.comprobantes.domain.model.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de identificación de la Tabla 6 de la ficha técnica con las reglas de forma de su número
 * (máscaras de la Tabla 5). Las descripciones y la vigencia del catálogo viven en la tabla {@code tipo_identificacion}; aquí está
 * lo que el código necesita saber para validar el número.
 * <ul>
 *   <li>{@code longitudExacta}: dígitos exactos del número (0 = formato libre, lo define el país emisor);</li>
 *   <li>{@code numeroFijo}: número obligatorio de ese tipo (consumidor final = 13 nueves), {@code null} si no aplica.</li>
 * </ul>
 * No se valida el dígito verificador de RUC/cédula: el algoritmo no está en la ficha técnica.
 */
public enum TipoIdentificacionSri {

    RUC("04", 13, null),
    CEDULA("05", 10, null),
    PASAPORTE("06", 0, null),
    CONSUMIDOR_FINAL("07", 13, "9999999999999"),
    IDENTIFICACION_EXTERIOR("08", 0, null);

    private final String codigo;
    private final int longitudExacta;
    private final String numeroFijo;

    TipoIdentificacionSri(String codigo, int longitudExacta, String numeroFijo) {
        this.codigo = codigo;
        this.longitudExacta = longitudExacta;
        this.numeroFijo = numeroFijo;
    }

    public String codigo() {
        return codigo;
    }

    public int longitudExacta() {
        return longitudExacta;
    }

    public Optional<String> numeroFijo() {
        return Optional.ofNullable(numeroFijo);
    }

    public static Optional<TipoIdentificacionSri> buscarPorCodigo(String codigo) {
        return Arrays.stream(values()).filter(t -> t.codigo.equals(codigo)).findFirst();
    }

    /** El tipo cuyo número es exactamente {@code identificacion} (hoy solo el consumidor final). */
    public static Optional<TipoIdentificacionSri> conNumeroFijo(String identificacion) {
        return Arrays.stream(values()).filter(t -> identificacion.equals(t.numeroFijo)).findFirst();
    }
}
