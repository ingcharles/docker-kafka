package com.facturacion.comprobantes.application.service.construccion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.application.dto.DetalleComprobanteRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.entity.ItemLocal;
import com.facturacion.comprobantes.domain.model.enums.ImpuestoSri;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.repository.ItemLocalRepository;
import com.facturacion.comprobantes.domain.service.CatalogosValidador;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Arma las líneas de detalle de los comprobantes con ítems (factura, nota de crédito, liquidación de compra): trae en
 * lote la copia local de los ítems, deriva sus impuestos del catálogo y calcula los totales.
 */
@ApplicationScoped
public class ConstructorDetalles {

    private final ItemLocalRepository itemLocalRepository;

    @Inject
    public ConstructorDetalles(ItemLocalRepository itemLocalRepository) {
        this.itemLocalRepository = itemLocalRepository;
    }

    public void aplicarTotales(Comprobante comprobante, List<DetalleComprobante> detalles) {
        BigDecimal subtotal = detalles.stream().map(DetalleComprobante::precioTotalSinImpuesto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDescuento = detalles.stream()
            .map(d -> d.getDescuento() == null ? BigDecimal.ZERO : d.getDescuento())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalImpuestos = detalles.stream()
            .flatMap(d -> d.getImpuestos().stream())
            .map(Impuesto::valor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        comprobante.setSubtotalSinImpuestos(subtotal);
        comprobante.setTotalDescuento(totalDescuento);
        comprobante.setImporteTotal(subtotal.add(totalImpuestos));
    }

    /**
     * Trae de una sola vez (batch) la copia local de todos los ítems que
     * necesitarán derivar su impuesto automáticamente — es decir, las
     * líneas que no traen {@code impuestos} explícitos pero sí un
     * {@code itemId}. El catálogo es multi-tenant: solo se consideran
     * ítems que pertenezcan al {@code ruc} de la empresa que está emitiendo
     * este comprobante — el ítem de otra empresa nunca hace match, aunque el
     * UUID exista.
     */
    public Uni<Map<UUID, ItemLocal>> cargarItems(List<DetalleComprobanteRequest> detalles,
            String ruc) {
        Set<UUID> ids = detalles.stream()
            .filter(d -> (d.getImpuestos() == null || d.getImpuestos().isEmpty()) && d.getItemId() != null)
            .map(DetalleComprobanteRequest::getItemId)
            .collect(Collectors.toSet());
        return itemLocalRepository.findByIdsAndRuc(ids, ruc);
    }

    public DetalleComprobante construir(DetalleComprobanteRequest request, Map<UUID, ItemLocal> itemsPorId,
            CatalogosSriAFecha catalogos) {
        DetalleComprobante detalle = new DetalleComprobante();
        detalle.setId(UUID.randomUUID());
        detalle.setItemId(request.getItemId());
        detalle.setCodigoPrincipal(request.getCodigoPrincipal());
        detalle.setCodigoAuxiliar(request.getCodigoAuxiliar());
        detalle.setDescripcion(request.getDescripcion());
        detalle.setCantidad(request.getCantidad());
        detalle.setPrecioUnitario(request.getPrecioUnitario());
        detalle.setDescuento(request.getDescuento() == null ? BigDecimal.ZERO : request.getDescuento());

        BigDecimal baseImponible = detalle.precioTotalSinImpuesto();
        List<Impuesto> impuestos;
        if (request.getImpuestos() != null && !request.getImpuestos().isEmpty()) {
            impuestos = request.getImpuestos().stream()
                .map(impRequest -> {
                    BigDecimal tarifa = CatalogosValidador.resolverTarifa(catalogos, impRequest.getCodigo(),
                        impRequest.getCodigoPorcentaje(), impRequest.getTarifa());
                    return new Impuesto(
                        impRequest.getCodigo(),
                        impRequest.getCodigoPorcentaje(),
                        tarifa,
                        baseImponible,
                        baseImponible.multiply(tarifa).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                })
                .collect(Collectors.toList());
        } else {
            ItemLocal item = request.getItemId() == null ? null : itemsPorId.get(request.getItemId());
            if (item == null) {
                throw request.getItemId() == null
                    ? new ErrorNegocioException(CodigoError.LINEA_SIN_IMPUESTOS_NI_ITEM, request.getDescripcion())
                    : new ErrorNegocioException(CodigoError.ITEM_NO_ENCONTRADO_EN_CATALOGO_LOCAL,
                        request.getDescripcion(), request.getItemId());
            }
            if (!item.isActivo()) {
                throw new ErrorNegocioException(CodigoError.ITEM_DADO_DE_BAJA, item.getCodigoPrincipal(), item.getId());
            }
            impuestos = construirImpuestosDesdeItem(item, baseImponible, catalogos);
        }
        detalle.setImpuestos(impuestos);
        return detalle;
    }

    /**
     * Deriva el/los impuesto(s) de una línea a partir de la clasificación
     * tributaria del ítem en inventario-service — el caso normal es un solo
     * impuesto (IVA); si el ítem tiene ICE se agrega una segunda línea.
     * Simplificación deliberada: ambos se calculan sobre la misma
     * {@code baseImponible} (el precio de la línea); la ley ecuatoriana en
     * rigor calcula el IVA sobre base+ICE para ítems con ICE — afinar eso
     * queda fuera del alcance de esta versión de referencia (mismo criterio
     * que el bloque de reembolso omitido en Factura/Liquidación de Compra).
     */
    private List<Impuesto> construirImpuestosDesdeItem(ItemLocal item, BigDecimal baseImponible,
            CatalogosSriAFecha catalogos) {
        List<Impuesto> impuestos = new ArrayList<>();
        // El ítem solo guarda codigoImpuesto + codigoPorcentaje: la tarifa sale del catálogo vigente a la
        // fecha de emisión (tabla tarifa_impuesto), no de una copia por ítem que habría que corregir a mano.
        BigDecimal tarifaIva = CatalogosValidador.resolverTarifa(catalogos, item.getCodigoImpuesto(),
            item.getCodigoPorcentaje(), null);
        BigDecimal valorIva = baseImponible.multiply(tarifaIva)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        impuestos.add(new Impuesto(item.getCodigoImpuesto(), item.getCodigoPorcentaje(), tarifaIva,
            baseImponible, valorIva));
        if (item.isTieneIce()) {
            BigDecimal valorIce = baseImponible.multiply(item.getTarifaIce())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            impuestos.add(new Impuesto(ImpuestoSri.ICE.codigo(), item.getCodigoPorcentajeIce(), item.getTarifaIce(), baseImponible,
                valorIce));
        }
        return impuestos;
    }
}
