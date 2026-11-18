package com.facturacion.comprobantes.domain.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.CodigoIva;
import com.facturacion.comprobantes.domain.model.enums.EtiquetaTotal;
import com.facturacion.comprobantes.domain.model.enums.ImpuestoSri;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;

/**
 * El resumen de totales que se imprime en el RIDE y en el ticket (Tabla 21 de la ficha técnica): subtotales por tarifa
 * de IVA (siempre incluye 0 %), no objeto y exento, subtotal sin impuestos, descuento, ICE, IRBPNR, IVA de cada tarifa
 * con valor, propina y valor total. Lógica pura: las bases y valores salen de los impuestos de cada línea.
 */
public final class TotalesComprobante {

    /** Símbolo que sigue a la tarifa en el rótulo ("SUBTOTAL 15%"). */
    private static final String SIMBOLO_PORCENTAJE = "%";

    /**
     * Una fila del resumen: qué fila es, la tarifa de IVA a la que se refiere (solo las filas de subtotal e IVA por
     * tarifa; {@code null} en las demás) y su valor en dólares.
     */
    public record Total(EtiquetaTotal tipo, BigDecimal tarifa, BigDecimal valor) {

        /** Rótulo tal como se imprime: "SUBTOTAL 15%", "ICE", "VALOR TOTAL"... */
        public String etiqueta() {
            return tarifa == null ? tipo.rotulo() : tipo.rotulo() + " " + tarifa.toPlainString() + SIMBOLO_PORCENTAJE;
        }
    }

    private TotalesComprobante() {
    }

    public static List<Total> calcular(Comprobante c) {
        Map<BigDecimal, BigDecimal> baseIvaPorTarifa = new TreeMap<>(Comparator.reverseOrder());
        Map<BigDecimal, BigDecimal> valorIvaPorTarifa = new TreeMap<>(Comparator.reverseOrder());
        BigDecimal noObjeto = BigDecimal.ZERO;
        BigDecimal exento = BigDecimal.ZERO;
        BigDecimal ice = BigDecimal.ZERO;
        BigDecimal irbpnr = BigDecimal.ZERO;
        baseIvaPorTarifa.put(BigDecimal.ZERO, BigDecimal.ZERO);
        for (DetalleComprobante d : c.getDetalles()) {
            if (d.getImpuestos() == null) {
                continue;
            }
            // En el XML la base del IVA incluye el ICE de la línea; en el resumen impreso el "SUBTOTAL IVA" es solo el
            // precio de los productos (Tabla 21: el IVA se calcula sobre subtotal + valor ICE).
            BigDecimal iceDeLinea = d.getImpuestos().stream().filter(i -> ImpuestoSri.ICE.esCodigo(i.codigo()))
                .map(i -> i.valor() == null ? BigDecimal.ZERO : i.valor()).reduce(BigDecimal.ZERO, BigDecimal::add);
            for (Impuesto i : d.getImpuestos()) {
                BigDecimal base = i.baseImponible() == null ? BigDecimal.ZERO : i.baseImponible();
                BigDecimal valor = i.valor() == null ? BigDecimal.ZERO : i.valor();
                if (ImpuestoSri.ICE.esCodigo(i.codigo())) {
                    ice = ice.add(valor);
                } else if (ImpuestoSri.IRBPNR.esCodigo(i.codigo())) {
                    irbpnr = irbpnr.add(valor);
                } else if (ImpuestoSri.IVA.esCodigo(i.codigo())) {
                    base = base.subtract(iceDeLinea);
                    if (CodigoIva.NO_OBJETO.esCodigo(i.codigoPorcentaje())) {
                        noObjeto = noObjeto.add(base);
                    } else if (CodigoIva.EXENTO.esCodigo(i.codigoPorcentaje())) {
                        exento = exento.add(base);
                    } else {
                        BigDecimal tarifa = normalizarTarifa(i.tarifa());
                        baseIvaPorTarifa.merge(tarifa, base, BigDecimal::add);
                        valorIvaPorTarifa.merge(tarifa, valor, BigDecimal::add);
                    }
                }
            }
        }
        List<Total> filas = new ArrayList<>();
        baseIvaPorTarifa.forEach((tarifa, base) ->
            filas.add(new Total(EtiquetaTotal.SUBTOTAL_IVA, tarifa, base)));
        if (noObjeto.signum() != 0) {
            filas.add(new Total(EtiquetaTotal.SUBTOTAL_NO_OBJETO_IVA, null, noObjeto));
        }
        if (exento.signum() != 0) {
            filas.add(new Total(EtiquetaTotal.SUBTOTAL_EXENTO_IVA, null, exento));
        }
        filas.add(new Total(EtiquetaTotal.SUBTOTAL_SIN_IMPUESTOS, null, c.getSubtotalSinImpuestos()));
        filas.add(new Total(EtiquetaTotal.DESCUENTO, null, c.getTotalDescuento()));
        filas.add(new Total(EtiquetaTotal.ICE, null, ice));
        if (irbpnr.signum() != 0) {
            filas.add(new Total(EtiquetaTotal.IRBPNR, null, irbpnr));
        }
        valorIvaPorTarifa.forEach((tarifa, valor) -> {
            if (tarifa.signum() != 0) {
                filas.add(new Total(EtiquetaTotal.IVA, tarifa, valor));
            }
        });
        filas.add(new Total(EtiquetaTotal.PROPINA, null, BigDecimal.ZERO));
        filas.add(new Total(EtiquetaTotal.VALOR_TOTAL, null, c.getImporteTotal()));
        return filas;
    }

    /** 15.00 y 15 son la misma tarifa; 0 se escribe siempre como "0". */
    private static BigDecimal normalizarTarifa(BigDecimal tarifa) {
        if (tarifa == null || tarifa.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return tarifa.stripTrailingZeros();
    }
}
