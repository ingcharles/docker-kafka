package com.facturacion.comprobantes.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import com.facturacion.comprobantes.shared.constant.ZonaHoraria;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;

/**
 * Clave de acceso de 49 dígitos (Tabla 1 de la ficha técnica del SRI) y su
 * dígito verificador por módulo 11. En el método offline (el que implementa
 * todo este proyecto) esta clave <b>es</b> el número de autorización del
 * comprobante — la empresa la genera, no el SRI.
 * <p>
 * Lógica de dominio pura, sin I/O, para poder razonar sobre ella y probarla
 * de forma aislada — mismo criterio que {@code KardexCalculador} en
 * inventario-service.
 */
public final class ClaveAccesoGenerador {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final String TIPO_EMISION_NORMAL = "1";

    private ClaveAccesoGenerador() {
    }

    public static String generar(LocalDate fechaEmision, TipoComprobante tipo, String ruc, AmbienteSri ambiente,
            String establecimiento, String puntoEmision, long secuencial) {
        String fecha = fechaEmision.format(FORMATO_FECHA);
        String serie = establecimiento + puntoEmision;
        String secuencialFormateado = String.format("%09d", secuencial);
        String codigoNumerico = generarCodigoNumerico();

        String base48 = fecha + tipo.codigo() + ruc + ambiente.codigo() + serie + secuencialFormateado
            + codigoNumerico + TIPO_EMISION_NORMAL;

        if (base48.length() != 48) {
            throw new IllegalStateException("La clave de acceso sin dígito verificador debe tener 48 dígitos, "
                + "tiene " + base48.length() + ": " + base48);
        }
        return base48 + digitoVerificadorModulo11(base48);
    }

    /**
     * Factor de chequeo ponderado 2 (cicla 2,3,4,5,6,7), aplicado de derecha
     * a izquierda sobre los 48 dígitos — ejemplo completo en la sección 5.2
     * de la ficha técnica. Caso especial: si el resultado da 11, el dígito
     * es 0; si da 10, el dígito es 1.
     */
    static String digitoVerificadorModulo11(String cadena48Digitos) {
        int suma = 0;
        int peso = 2;
        for (int i = cadena48Digitos.length() - 1; i >= 0; i--) {
            int digito = Character.getNumericValue(cadena48Digitos.charAt(i));
            suma += digito * peso;
            peso = (peso == 7) ? 2 : peso + 1;
        }
        int resultado = 11 - (suma % 11);
        if (resultado == 11) {
            return "0";
        }
        if (resultado == 10) {
            return "1";
        }
        return String.valueOf(resultado);
    }

    private static String generarCodigoNumerico() {
        int valor = ThreadLocalRandom.current().nextInt(0, 100_000_000);
        return String.format("%08d", valor);
    }

    public static LocalDate hoyEnEcuador() {
        return LocalDate.now(ZonaHoraria.ECUADOR);
    }
}
