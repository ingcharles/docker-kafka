package com.facturacion.inventario.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.application.dto.MovimientoKardexResponse;
import com.facturacion.inventario.application.dto.RegistrarAjusteKardexRequest;
import com.facturacion.inventario.application.dto.RegistrarEntradaKardexRequest;
import com.facturacion.inventario.domain.model.entity.Item;
import com.facturacion.inventario.domain.model.entity.MovimientoKardex;
import com.facturacion.inventario.domain.model.enums.TipoMovimientoKardex;
import com.facturacion.inventario.domain.model.vo.PageResult;
import com.facturacion.inventario.domain.repository.ItemRepository;
import com.facturacion.inventario.domain.repository.MovimientoKardexRepository;
import com.facturacion.inventario.domain.service.KardexCalculador;
import com.facturacion.inventario.infrastructure.persistence.mapper.MovimientoKardexMapper;
import com.facturacion.inventario.shared.exception.ItemNoEsProductoException;
import com.facturacion.inventario.shared.exception.NotFoundException;
import com.facturacion.inventario.shared.exception.StockInsuficienteException;
import com.facturacion.inventario.shared.responses.PagedResponse;

/**
 * Kardex de costeo por promedio ponderado (ver {@link KardexCalculador}).
 * Invariante que mantiene todo este servicio: en todo momento,
 * {@code movimiento_kardex.cantidad_saldo} (el último movimiento de un
 * ítem) es igual a {@code item.cantidad_disponible + item.cantidad_reservada}
 * — una reserva de stock (ver {@code StockReservationService}) mueve
 * cantidad entre esos dos baldes sin escribir kardex, porque nada salió
 * físicamente todavía; el kardex solo se escribe cuando el movimiento es
 * real: una entrada, un ajuste, o la confirmación de una venta.
 */
@ApplicationScoped
public class KardexService {

    private final ItemRepository itemRepository;
    private final MovimientoKardexRepository kardexRepository;
    private final MovimientoKardexMapper mapper;

    @Inject
    public KardexService(ItemRepository itemRepository, MovimientoKardexRepository kardexRepository,
            MovimientoKardexMapper mapper) {
        this.itemRepository = itemRepository;
        this.kardexRepository = kardexRepository;
        this.mapper = mapper;
    }

    @WithTransaction
    public Uni<MovimientoKardexResponse> registrarEntrada(UUID itemId, @Valid RegistrarEntradaKardexRequest request) {
        return cargarProducto(itemId)
            .onItem().transformToUni(item -> saldoActual(itemId)
                .onItem().transformToUni(saldo -> {
                    KardexCalculador.ResultadoMovimiento resultado = KardexCalculador.calcularEntrada(
                        saldo.cantidadSaldo(), saldo.costoPromedioSaldo(), request.getCantidad(),
                        request.getCostoUnitario());
                    return itemRepository.incrementarDisponible(itemId, request.getCantidad())
                        .chain(ignorado -> persistirMovimiento(itemId, TipoMovimientoKardex.ENTRADA,
                            request.getCantidad(), request.getCostoUnitario(), resultado,
                            request.getReferenciaDocumento(), request.getMotivo()));
                }));
    }

    @WithTransaction
    public Uni<MovimientoKardexResponse> registrarAjuste(UUID itemId, @Valid RegistrarAjusteKardexRequest request) {
        return cargarProducto(itemId)
            .onItem().transformToUni(item -> {
                if (request.isPositivo()) {
                    return saldoActual(itemId).onItem().transformToUni(saldo -> {
                        KardexCalculador.ResultadoMovimiento resultado = KardexCalculador.calcularEntrada(
                            saldo.cantidadSaldo(), saldo.costoPromedioSaldo(), request.getCantidad(),
                            saldo.costoPromedioSaldo());
                        return itemRepository.incrementarDisponible(itemId, request.getCantidad())
                            .chain(ignorado -> persistirMovimiento(itemId, TipoMovimientoKardex.AJUSTE_POSITIVO,
                                request.getCantidad(), saldo.costoPromedioSaldo(), resultado, null,
                                request.getMotivo()));
                    });
                }
                return itemRepository.decrementarDisponible(itemId, request.getCantidad())
                    .onItem().transformToUni(filas -> {
                        if (filas == 0) {
                            return Uni.createFrom().failure(new StockInsuficienteException(itemId));
                        }
                        return saldoActual(itemId).onItem().transformToUni(saldo -> {
                            KardexCalculador.ResultadoMovimiento resultado = KardexCalculador.calcularSalida(
                                saldo.cantidadSaldo(), saldo.costoPromedioSaldo(), request.getCantidad());
                            return persistirMovimiento(itemId, TipoMovimientoKardex.AJUSTE_NEGATIVO,
                                request.getCantidad(), saldo.costoPromedioSaldo(), resultado, null,
                                request.getMotivo());
                        });
                    });
            });
    }

    /**
     * Usado por {@code StockReservationService} al confirmar una venta: el
     * stock ya salió de {@code cantidadDisponible} en el momento de la
     * reserva, así que aquí solo se escribe el rastro contable (no se
     * vuelve a tocar la tabla {@code item}).
     */
    public Uni<MovimientoKardex> registrarSalidaConfirmada(UUID itemId, BigDecimal cantidad,
            String referenciaDocumento) {
        return saldoActual(itemId).onItem().transformToUni(saldo -> {
            KardexCalculador.ResultadoMovimiento resultado = KardexCalculador.calcularSalida(
                saldo.cantidadSaldo(), saldo.costoPromedioSaldo(), cantidad);
            MovimientoKardex movimiento = new MovimientoKardex(UUID.randomUUID(), itemId,
                TipoMovimientoKardex.SALIDA, cantidad, resultado.costoUnitarioAplicado(), resultado.cantidadSaldo(),
                resultado.costoPromedioSaldo(), resultado.valorSaldo(), referenciaDocumento,
                "Confirmación de venta", Instant.now());
            return kardexRepository.persist(movimiento);
        });
    }

    @WithSession
    public Uni<PagedResponse<MovimientoKardexResponse>> listarMovimientos(UUID itemId, int page, int limit) {
        return kardexRepository.findPagedByItemId(itemId, page, limit)
            .map((PageResult<MovimientoKardex> pageResult) -> {
                List<MovimientoKardexResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }

    private Uni<Item> cargarProducto(UUID itemId) {
        return itemRepository.findById(itemId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Ítem no encontrado: " + itemId))
            .onItem().transform(item -> {
                if (!item.esProducto()) {
                    throw new ItemNoEsProductoException(itemId);
                }
                return item;
            });
    }

    private Uni<KardexCalculador.ResultadoMovimiento> saldoActual(UUID itemId) {
        return kardexRepository.findUltimoByItemId(itemId)
            .map(ultimo -> ultimo == null
                ? new KardexCalculador.ResultadoMovimiento(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO)
                : new KardexCalculador.ResultadoMovimiento(ultimo.getCantidadSaldo(), ultimo.getCostoPromedioSaldo(),
                    ultimo.getValorSaldo(), BigDecimal.ZERO));
    }

    private Uni<MovimientoKardexResponse> persistirMovimiento(UUID itemId, TipoMovimientoKardex tipo,
            BigDecimal cantidad, BigDecimal costoUnitario, KardexCalculador.ResultadoMovimiento resultado,
            String referenciaDocumento, String motivo) {
        MovimientoKardex movimiento = new MovimientoKardex(UUID.randomUUID(), itemId, tipo, cantidad, costoUnitario,
            resultado.cantidadSaldo(), resultado.costoPromedioSaldo(), resultado.valorSaldo(), referenciaDocumento,
            motivo, Instant.now());
        return kardexRepository.persist(movimiento).map(mapper::toResponse);
    }
}
