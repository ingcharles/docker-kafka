package com.facturacion.comprobantes.domain.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.facturacion.comprobantes.domain.model.enums.TipoIdentificacionSri;
import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

/**
 * Coherencia entre el tipo de identificación (Tabla 6 del SRI) y el número de identificación; las reglas de cada
 * tipo están en {@link TipoIdentificacionSri}. Lanza {@link ErrorNegocioException} (400).
 * <ul>
 *   <li>Un tipo con número fijo (consumidor final = 13 nueves) debe llevar exactamente ese número, y ese número
 *       solo es válido con ese tipo.</li>
 *   <li>RUC: 13 dígitos. Cédula: 10 dígitos (máscaras de la Tabla 5 de la ficha).</li>
 *   <li>Pasaporte e identificación del exterior: formato libre (lo define el país emisor).</li>
 * </ul>
 * El tipo que no es de la Tabla 6 no se valida aquí: eso lo hace el catálogo ({@code CatalogosValidador}).
 */
public final class IdentificacionValidador {

    private IdentificacionValidador() {
    }

    /**
     * Una factura por encima de {@code montoMaximo} no puede ir a consumidor final: el SRI exige los datos del
     * adquirente (ficha técnica, 8.10: 50 USD; el monto máximo se ha actualizado en el historial de la ficha, por eso
     * es configurable: {@code comprobantes.consumidor-final.monto-maximo}). Se comprueba aquí para devolver un 400
     * claro en vez de enviar al SRI algo que va a rechazar.
     */
    public static void validarMontoConsumidorFinal(String codigoTipo, BigDecimal importeTotal, BigDecimal montoMaximo,
            String campo) {
        boolean esConsumidorFinal = TipoIdentificacionSri.deCodigo(codigoTipo)
            .filter(t -> t == TipoIdentificacionSri.CONSUMIDOR_FINAL).isPresent();
        if (esConsumidorFinal && importeTotal != null && importeTotal.compareTo(montoMaximo) > 0) {
            throw new ErrorNegocioException(CodigoError.CONSUMIDOR_FINAL_EXCEDE_MONTO, campo,
                montoMaximo.toPlainString(), importeTotal.toPlainString());
        }
    }

    public static void validar(String codigoTipo, String identificacion, String campo) {
        if (codigoTipo == null || identificacion == null) {
            return; // @NotBlank del DTO ya lo exige
        }
        Optional<TipoIdentificacionSri> tipo = TipoIdentificacionSri.deCodigo(codigoTipo);

        Optional<TipoIdentificacionSri> duenoDelNumero = TipoIdentificacionSri.conNumeroFijo(identificacion);
        if (duenoDelNumero.isPresent() && duenoDelNumero.get() != tipo.orElse(null)) {
            throw new ErrorNegocioException(CodigoError.IDENTIFICACION_FIJA_CON_OTRO_TIPO, campo, identificacion,
                duenoDelNumero.get().codigo(), duenoDelNumero.get().name());
        }
        if (tipo.isEmpty()) {
            return;
        }
        TipoIdentificacionSri t = tipo.get();
        if (t.numeroFijo().isPresent() && !t.numeroFijo().get().equals(identificacion)) {
            throw new ErrorNegocioException(CodigoError.IDENTIFICACION_FIJA_INCORRECTA, campo, t.codigo(), t.name(),
                t.numeroFijo().get());
        }
        if (t.numeroFijo().isEmpty() && t.longitudExacta() > 0
            && (identificacion.length() != t.longitudExacta()
                || !identificacion.chars().allMatch(Character::isDigit))) {
            throw new ErrorNegocioException(CodigoError.IDENTIFICACION_LONGITUD_INVALIDA, campo, t.name(),
                t.longitudExacta(), identificacion);
        }
    }
}
