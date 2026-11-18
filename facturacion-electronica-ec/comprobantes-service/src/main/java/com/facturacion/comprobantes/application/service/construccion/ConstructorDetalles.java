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
import com.facturacion.comprobantes.application.dto.ImpuestoRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.entity.ItemLocal;
import com.facturacion.comprobantes.domain.model.enums.ImpuestoSri;
import com.facturacion.comprobantes.domain.model.enums.TipoTarifa;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.repository.ItemLocalRepository;
import com.facturacion.comprobantes.domain.service.CatalogosValidador;
import com.facturacion.comprobantes.domain.service.CatalogosValidador.TarifaResuelta;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Arma las líneas de detalle de los comprobantes con ítems (factura, nota de crédito, liquidación de compra): trae en
 * lote la copia local de los ítems, deriva sus impuestos del catálogo y calcula los totales.
 */
@ApplicationScoped
public class ConstructorDetalles {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    /** Decimales de un valor en dólares (base y valor de un impuesto). */
    private static final int DECIMALES_DINERO = 2;

    /** Máximo de {@code <detAdicional>} por línea que admite el XSD del SRI. */
    private static final int MAX_DETALLES_ADICIONALES = 3;

    private final ItemLocalRepository itemLocalRepository;

    @Inject
    public ConstructorDetalles(ItemLocalRepository itemLocalRepository) {
        this.itemLocalRepository = itemLocalRepository;
    }

    public void aplicarTotales(Comprobante comprobante, List<DetalleComprobante> detalles) {
        BigDecimal subtotal = detalles.stream().map(DetalleComprobante::calcularPrecioTotalSinImpuesto)
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
            .filter(d -> d.getItemId() != null)
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
        detalle.setPrecioSinSubsidio(request.getPrecioSinSubsidio());

        BigDecimal baseImponible = detalle.calcularPrecioTotalSinImpuesto();
        List<Impuesto> impuestos;
        if (request.getImpuestos() != null && !request.getImpuestos().isEmpty()) {
            impuestos = calcularImpuestos(baseImponible, request.getCantidad(), request.getImpuestos().stream()
                .map(impRequest -> resolverTarifaDelRequest(impRequest, catalogos))
                .collect(Collectors.toList()));
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
            impuestos = construirImpuestosDesdeItem(item, baseImponible, request.getCantidad(), catalogos);
        }
        detalle.setImpuestos(impuestos);
        detalle.setDetallesAdicionales(armarDetallesAdicionales(request,
            request.getItemId() == null ? null : itemsPorId.get(request.getItemId())));
        return detalle;
    }

    /**
     * Los detalles adicionales que mandó el llamador (hasta 3) y, si el ítem tiene unidad de medida y no se mandó uno
     * "Medida", ese primero: "Medida: UN".
     */
    private List<CampoAdicional> armarDetallesAdicionales(DetalleComprobanteRequest request, ItemLocal item) {
        List<CampoAdicional> detalles = new ArrayList<>();
        if (request.getDetallesAdicionales() != null) {
            request.getDetallesAdicionales()
                .forEach(d -> detalles.add(new CampoAdicional(d.getNombre(), d.getValor())));
        }
        boolean hayMedida = detalles.stream().anyMatch(d -> "Medida".equalsIgnoreCase(d.nombre()));
        if (item != null && item.getUnidadMedida() != null && !item.getUnidadMedida().isBlank() && !hayMedida
                && detalles.size() < MAX_DETALLES_ADICIONALES) {
            detalles.add(0, new CampoAdicional("Medida", item.getUnidadMedida()));
        }
        return detalles;
    }

    /** Resuelve la tarifa (y su tipo) de un impuesto pedido en el request contra el catálogo a la fecha de emisión. */
    private static TarifaDeImpuesto resolverTarifaDelRequest(ImpuestoRequest impRequest, CatalogosSriAFecha catalogos) {
        if (impRequest.getBaseImponible() != null && ImpuestoSri.IVA.esCodigo(impRequest.getCodigo())) {
            throw new ErrorNegocioException(CodigoError.BASE_IMPONIBLE_NO_APLICA, impRequest.getCodigo());
        }
        TarifaResuelta resuelta = CatalogosValidador.resolverTarifaConTipo(catalogos, impRequest.getCodigo(),
            impRequest.getCodigoPorcentaje(), impRequest.getTarifa(), impRequest.getTipoTarifa());
        return new TarifaDeImpuesto(impRequest.getCodigo(), impRequest.getCodigoPorcentaje(), resuelta.tarifa(),
            resuelta.tipo(), impRequest.getBaseImponible());
    }

    /**
     * Deriva los impuestos de una línea a partir de la clasificación tributaria del ítem en inventario-service: el
     * IVA siempre y, si el ítem los tiene, el ICE y el IRBPNR. El ítem solo guarda códigos; la tarifa y su tipo salen
     * del catálogo vigente a la fecha de emisión, así un cambio de normativa no obliga a tocar ítems.
     */
    private List<Impuesto> construirImpuestosDesdeItem(ItemLocal item, BigDecimal baseImponible,
            BigDecimal cantidad, CatalogosSriAFecha catalogos) {
        List<TarifaDeImpuesto> tarifas = new ArrayList<>();
        tarifas.add(resolverTarifaDelCatalogo(ImpuestoSri.IVA, item.getCodigoPorcentajeIva(), catalogos));
        if (item.getCodigoIce() != null) {
            tarifas.add(resolverTarifaDelCatalogo(ImpuestoSri.ICE, item.getCodigoIce(), catalogos));
        }
        if (item.getCodigoIrbpnr() != null) {
            tarifas.add(resolverTarifaDelCatalogo(ImpuestoSri.IRBPNR, item.getCodigoIrbpnr(), catalogos));
        }
        return calcularImpuestos(baseImponible, cantidad, tarifas);
    }

    private static TarifaDeImpuesto resolverTarifaDelCatalogo(ImpuestoSri impuesto, String codigoPorcentaje,
            CatalogosSriAFecha catalogos) {
        TarifaResuelta resuelta = CatalogosValidador.resolverTarifaConTipo(catalogos, impuesto.codigo(),
            codigoPorcentaje, null, null);
        return new TarifaDeImpuesto(impuesto.codigo(), codigoPorcentaje, resuelta.tarifa(), resuelta.tipo(), null);
    }

    /**
     * Calcula base y valor de los impuestos de una línea (ficha técnica del SRI, Tabla 21 y ejemplos de la factura):
     * <ul>
     * <li>Tarifa {@link TipoTarifa#PORCENTAJE}: valor = base × tarifa / 100; la base es el subtotal de la línea, o la
     * que indique el emisor (base por PVP, art. 76 LRTI).</li>
     * <li>Tarifa {@link TipoTarifa#ESPECIFICA} (USD por unidad: IRBPNR, ICE específico): la base son las unidades, por
     * defecto la cantidad de la línea, y valor = base × tarifa.</li>
     * <li>IVA: porcentaje sobre {@code subtotal + ICE} (art. 58 LRTI: el valor del ICE forma parte de su base
     * imponible). El IRBPNR no entra en esa base.</li>
     * </ul>
     */
    static List<Impuesto> calcularImpuestos(BigDecimal baseImponible, BigDecimal cantidad,
            List<TarifaDeImpuesto> tarifas) {
        List<Impuesto> sinIva = new ArrayList<>();
        BigDecimal valorIce = BigDecimal.ZERO;
        for (TarifaDeImpuesto t : tarifas) {
            if (ImpuestoSri.IVA.esCodigo(t.codigo())) {
                continue;
            }
            Impuesto calculado = calcularNoIva(t, baseImponible, cantidad);
            sinIva.add(calculado);
            if (ImpuestoSri.ICE.esCodigo(t.codigo())) {
                valorIce = valorIce.add(calculado.valor());
            }
        }
        BigDecimal baseIva = baseImponible.add(valorIce);
        List<Impuesto> impuestos = new ArrayList<>();
        for (TarifaDeImpuesto t : tarifas) {
            if (ImpuestoSri.IVA.esCodigo(t.codigo())) {
                impuestos.add(new Impuesto(t.codigo(), t.codigoPorcentaje(), t.tarifa(), baseIva,
                    calcularPorcentaje(baseIva, t.tarifa())));
            } else {
                impuestos.add(sinIva.remove(0));
            }
        }
        return impuestos;
    }

    private static Impuesto calcularNoIva(TarifaDeImpuesto t, BigDecimal baseLinea, BigDecimal cantidad) {
        if (t.tipo() == TipoTarifa.ESPECIFICA) {
            BigDecimal unidades = t.baseIndicada() != null ? t.baseIndicada() : cantidad;
            return new Impuesto(t.codigo(), t.codigoPorcentaje(), t.tarifa(), unidades,
                unidades.multiply(t.tarifa()).setScale(DECIMALES_DINERO, RoundingMode.HALF_UP));
        }
        BigDecimal base = t.baseIndicada() != null ? t.baseIndicada() : baseLinea;
        return new Impuesto(t.codigo(), t.codigoPorcentaje(), t.tarifa(), base, calcularPorcentaje(base, t.tarifa()));
    }

    private static BigDecimal calcularPorcentaje(BigDecimal base, BigDecimal tarifa) {
        return base.multiply(tarifa).divide(CIEN, DECIMALES_DINERO, RoundingMode.HALF_UP);
    }

    /**
     * Un impuesto de la línea con su tarifa ya resuelta, antes de calcular base y valor.
     *
     * @param baseIndicada base imponible que indicó el emisor (solo ICE/IRBPNR); {@code null} = la de la línea
     */
    record TarifaDeImpuesto(String codigo, String codigoPorcentaje, BigDecimal tarifa, TipoTarifa tipo,
            BigDecimal baseIndicada) {
    }
}
