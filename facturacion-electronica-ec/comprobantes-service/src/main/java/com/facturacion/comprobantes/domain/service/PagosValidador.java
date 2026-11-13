package com.facturacion.comprobantes.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import com.facturacion.comprobantes.domain.model.enums.FormaPagoSri;
import com.facturacion.comprobantes.domain.model.vo.Pago;
import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

/**
 * Reglas de negocio de {@code <pagos>} que el XSD del SRI no cubre (el XSD solo exige un código {@code 01}–{@code 21},
 * {@code total} decimal, y {@code plazo}/{@code unidadTiempo} sueltos). Lanza {@link ErrorNegocioException} con su
 * {@link CodigoError} (la API responde 400): mejor que dejar pasar un comprobante que el SRI rechazaría o, peor,
 * autorizaría con pagos que no cuadran.
 * <p>
 * Reglas:
 * <ol>
 *   <li>{@code formaPago} debe existir y estar vigente en la Tabla 24 (tabla {@code forma_pago}, ver
 *       {@code CatalogosSriAFecha#formasPagoVigentes}).</li>
 *   <li>{@code plazo} y {@code unidadTiempo} van juntos o ninguno (la ficha técnica los marca "obligatorio cuando
 *       corresponda"); {@code plazo} numérico &gt;= 0 con hasta 2 decimales.</li>
 *   <li>La suma de los {@code total} (cada uno a 2 decimales, como se imprime en el XML) debe ser igual al
 *       {@code importeTotal} impreso. Regla de este sistema: la ficha técnica no la dice expresamente.</li>
 *   <li>Retención v2.0.0: el {@code pago} del XSD solo tiene {@code formaPago} y {@code total}, así que
 *       {@code plazo}/{@code unidadTiempo} se rechazan.</li>
 *   <li>Factura Comercial Negociable (Anexo 11): si algún pago es {@link FormaPagoSri#ENDOSO_TITULOS} debe traer
 *       {@code plazo} y {@code unidadTiempo}, y la factura debe llevar {@code direccionComprador}.</li>
 * </ol>
 */
public final class PagosValidador {

    private static final int MAX_UNIDAD_TIEMPO = 10;
    private static final int MAX_DECIMALES_PLAZO = 2;

    private PagosValidador() {
    }

    /**
     * @param admitePlazo {@code false} para Comprobante de Retención (su XSD no tiene plazo/unidadTiempo en {@code pago}).
     * @param formasPagoVigentes códigos de la Tabla 24 activos y vigentes a la fecha de emisión
     */
    public static void validar(List<Pago> pagos, BigDecimal importeTotal, boolean admitePlazo,
            Set<String> formasPagoVigentes) {
        BigDecimal suma = BigDecimal.ZERO;
        for (int i = 0; i < pagos.size(); i++) {
            Pago pago = pagos.get(i);
            String ref = referencia(i);

            if (!formasPagoVigentes.contains(pago.formaPago())) {
                throw new ErrorNegocioException(CodigoError.PAGO_FORMA_NO_VIGENTE, ref, pago.formaPago(),
                    formasPagoVigentes);
            }

            boolean hayPlazo = hayTexto(pago.plazo());
            boolean hayUnidad = hayTexto(pago.unidadTiempo());
            if (!admitePlazo && (hayPlazo || hayUnidad)) {
                throw new ErrorNegocioException(CodigoError.PAGO_RETENCION_SIN_PLAZO, ref);
            }
            if (hayPlazo && !hayUnidad) {
                throw new ErrorNegocioException(CodigoError.PAGO_PLAZO_SIN_UNIDAD, ref);
            }
            if (hayUnidad && !hayPlazo) {
                throw new ErrorNegocioException(CodigoError.PAGO_UNIDAD_SIN_PLAZO, ref);
            }
            if (hayPlazo) {
                validarPlazo(ref, pago.plazo());
                if (pago.unidadTiempo().length() > MAX_UNIDAD_TIEMPO) {
                    throw new ErrorNegocioException(CodigoError.PAGO_UNIDAD_TIEMPO_LARGA, ref, MAX_UNIDAD_TIEMPO);
                }
            }

            suma = suma.add(aDosDecimales(pago.total()));
        }

        BigDecimal esperado = aDosDecimales(importeTotal);
        if (suma.compareTo(esperado) != 0) {
            throw new ErrorNegocioException(CodigoError.PAGOS_NO_CUADRAN, suma.toPlainString(),
                esperado.toPlainString());
        }
    }

    /** Anexo 11 — solo aplica a Factura. {@code compradorDireccion} es {@code direccionComprador} del XML. */
    public static void validarFacturaNegociable(List<Pago> pagos, String compradorDireccion) {
        String endoso = FormaPagoSri.ENDOSO_TITULOS.codigo();
        for (int i = 0; i < pagos.size(); i++) {
            Pago pago = pagos.get(i);
            if (!endoso.equals(pago.formaPago())) {
                continue;
            }
            if (!hayTexto(pago.plazo()) || !hayTexto(pago.unidadTiempo())) {
                throw new ErrorNegocioException(CodigoError.NEGOCIABLE_SIN_PLAZO, referencia(i), endoso);
            }
            if (!hayTexto(compradorDireccion)) {
                throw new ErrorNegocioException(CodigoError.NEGOCIABLE_SIN_DIRECCION, endoso);
            }
        }
    }

    private static void validarPlazo(String ref, String plazo) {
        BigDecimal valor;
        try {
            valor = new BigDecimal(plazo.trim());
        } catch (NumberFormatException e) {
            throw new ErrorNegocioException(CodigoError.PAGO_PLAZO_NO_NUMERICO, ref, plazo);
        }
        if (valor.signum() < 0 || valor.stripTrailingZeros().scale() > MAX_DECIMALES_PLAZO) {
            throw new ErrorNegocioException(CodigoError.PAGO_PLAZO_FUERA_DE_RANGO, ref, plazo);
        }
    }

    private static String referencia(int indice) {
        return "pagos[" + indice + "]";
    }

    private static BigDecimal aDosDecimales(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean hayTexto(String s) {
        return s != null && !s.isBlank();
    }
}
