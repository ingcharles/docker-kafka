package com.facturacion.comprobantes.domain.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.domain.model.enums.ImpuestoSri;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoTarifa;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.RetencionRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.TarifaRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.TipoIdentificacionRow;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Reglas que cruzan un request con los catálogos del SRI (tablas propias, ver {@link CatalogosSriAFecha}).
 * Todas lanzan {@link ErrorNegocioException} con su {@link CodigoError} (la API responde 400).
 * <p>
 * Regla común: si el catálogo (o la parte del catálogo que corresponde) no tiene filas, el valor solo se acepta
 * por formato y no se valida contra el catálogo — así un catálogo que la documentación del proyecto no trae
 * (códigos de retención de Renta, codSustento) no bloquea la emisión, y se vuelve estricto solo cuando alguien
 * lo carga con PUT /catalogos/{catalogo}.
 */
public final class CatalogosValidador {

    private CatalogosValidador() {
    }

    /**
     * Devuelve la tarifa a usar para un impuesto de una línea.
     *
     * @param tarifaSolicitada la que mandó el cliente; {@code null} si pide que se tome del catálogo
     */
    public static BigDecimal resolverTarifa(CatalogosSriAFecha catalogos, String impuestoCodigo,
            String codigoPorcentaje, BigDecimal tarifaSolicitada) {
        return resolverTarifaConTipo(catalogos, impuestoCodigo, codigoPorcentaje, tarifaSolicitada, null).tarifa();
    }

    /** La tarifa de una línea y cómo se calcula su valor ({@link TipoTarifa}). */
    public record TarifaResuelta(BigDecimal tarifa, TipoTarifa tipo) {
    }

    /**
     * Como {@link #resolverTarifa}, pero también dice si la tarifa es porcentual o específica (Tabla 18). Sin
     * {@code tipoSolicitado} se usa la porcentual si el código la tiene y, si no, la específica; con el catálogo de ese
     * impuesto vacío el tipo es porcentual salvo para el IRBPNR, que por definición es una tarifa por unidad.
     */
    public static TarifaResuelta resolverTarifaConTipo(CatalogosSriAFecha catalogos, String impuestoCodigo,
            String codigoPorcentaje, BigDecimal tarifaSolicitada, TipoTarifa tipoSolicitado) {
        if (catalogos.hayImpuestos() && !catalogos.impuestoExiste(impuestoCodigo)) {
            throw new ErrorNegocioException(CodigoError.IMPUESTO_NO_EXISTE, impuestoCodigo,
                catalogos.codigosImpuesto());
        }
        if (catalogos.hayTarifas(impuestoCodigo)) {
            if (catalogos.codigosPorcentaje(impuestoCodigo).stream().noneMatch(codigoPorcentaje::equals)) {
                throw new ErrorNegocioException(CodigoError.PORCENTAJE_IMPUESTO_NO_VIGENTE, codigoPorcentaje,
                    impuestoCodigo, catalogos.codigosPorcentaje(impuestoCodigo));
            }
            TarifaRow fila = catalogos.tarifaVigente(impuestoCodigo, codigoPorcentaje, tipoSolicitado).orElseThrow(
                () -> new ErrorNegocioException(CodigoError.TIPO_TARIFA_NO_VIGENTE, impuestoCodigo,
                    codigoPorcentaje, tipoSolicitado, catalogos.tiposTarifaVigentes(impuestoCodigo, codigoPorcentaje)));
            if (tarifaSolicitada != null && fila.tarifa().compareTo(tarifaSolicitada) != 0) {
                throw new ErrorNegocioException(CodigoError.TARIFA_NO_COINCIDE, tarifaSolicitada.toPlainString(),
                    fila.tarifa().toPlainString(), impuestoCodigo, codigoPorcentaje);
            }
            return new TarifaResuelta(fila.tarifa(), fila.tipoTarifa());
        }
        if (tarifaSolicitada == null) {
            throw new ErrorNegocioException(CodigoError.TARIFA_NO_DISPONIBLE, impuestoCodigo, codigoPorcentaje);
        }
        TipoTarifa tipo = tipoSolicitado != null ? tipoSolicitado
            : ImpuestoSri.IRBPNR.esCodigo(impuestoCodigo) ? TipoTarifa.ESPECIFICA : TipoTarifa.PORCENTAJE;
        return new TarifaResuelta(tarifaSolicitada, tipo);
    }

    /**
     * Tabla 6. El tipo de consumidor final solo es válido en Factura: la ficha lo deshabilita en liquidaciones de
     * compra y exige identificar al receptor en notas de crédito/débito y retenciones.
     */
    public static void validarTipoIdentificacion(CatalogosSriAFecha catalogos, String codigo, TipoComprobante tipo,
            String campo) {
        if (!catalogos.hayTiposIdentificacion()) {
            return;
        }
        Optional<TipoIdentificacionRow> fila = catalogos.tipoIdentificacion(codigo);
        if (fila.isEmpty()) {
            throw new ErrorNegocioException(CodigoError.TIPO_IDENTIFICACION_NO_EXISTE, campo, codigo,
                catalogos.codigosTipoIdentificacion());
        }
        if (fila.get().soloFactura() && tipo != TipoComprobante.FACTURA) {
            throw new ErrorNegocioException(CodigoError.CONSUMIDOR_FINAL_SOLO_FACTURA, campo, codigo,
                fila.get().descripcion());
        }
    }

    /**
     * Línea de retención: {@code impuestoCodigo} es el impuesto a retener (Tabla 19: 1 Renta, 2 IVA, 6 ISD). Si el
     * catálogo tiene códigos para ese impuesto, el código debe existir y estar vigente, y el porcentaje debe ser el
     * del catálogo; si no los tiene (Renta), solo se valida el formato.
     */
    public static void validarRetencion(CatalogosSriAFecha catalogos, String impuestoCodigo, String codigoRetencion,
            BigDecimal porcentajeRetener) {
        if (catalogos.hayImpuestosRetencion() && !catalogos.impuestoRetencionExiste(impuestoCodigo)) {
            throw new ErrorNegocioException(CodigoError.IMPUESTO_RETENER_NO_EXISTE, impuestoCodigo,
                catalogos.codigosImpuestoRetencion());
        }
        if (!catalogos.hayRetenciones(impuestoCodigo)) {
            return;
        }
        Optional<RetencionRow> fila = catalogos.retencionVigente(impuestoCodigo, codigoRetencion);
        if (fila.isEmpty()) {
            CodigoError motivo = catalogos.retencionExiste(impuestoCodigo, codigoRetencion)
                ? CodigoError.CODIGO_RETENCION_NO_VIGENTE : CodigoError.CODIGO_RETENCION_NO_EXISTE;
            throw new ErrorNegocioException(motivo, codigoRetencion, impuestoCodigo,
                catalogos.codigosRetencionVigentes(impuestoCodigo), catalogos.fecha());
        }
        if (porcentajeRetener != null && fila.get().porcentaje().compareTo(porcentajeRetener) != 0) {
            throw new ErrorNegocioException(CodigoError.PORCENTAJE_RETENER_NO_COINCIDE,
                porcentajeRetener.toPlainString(), fila.get().porcentaje().toPlainString(), codigoRetencion,
                impuestoCodigo);
        }
    }

    /** codSustento (catálogo ATS). Con el catálogo vacío solo rige el formato del XSD. */
    public static void validarSustento(CatalogosSriAFecha catalogos, String codSustento) {
        if (catalogos.haySustentos() && !catalogos.sustentoExiste(codSustento)) {
            throw new ErrorNegocioException(CodigoError.SUSTENTO_NO_EXISTE, codSustento,
                catalogos.codigosSustento());
        }
    }
}
