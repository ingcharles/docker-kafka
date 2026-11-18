package com.facturacion.inventario.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.application.event.ItemCantidadPayload;
import com.facturacion.inventario.domain.model.entity.ReservaStock;
import com.facturacion.inventario.domain.model.enums.EstadoReserva;
import com.facturacion.inventario.domain.model.enums.TipoItem;
import com.facturacion.inventario.domain.repository.ItemRepository;
import com.facturacion.inventario.domain.repository.ReservaStockRepository;

/**
 * Reserva/confirma/libera stock para los comandos que llegan desde
 * {@code facturacion-orchestrator-service} (ver
 * {@code infrastructure.messaging.consumer.StockCommandConsumer}). Una
 * reserva descuenta {@code cantidadDisponible} de inmediato, pero <b>no</b>
 * escribe kardex — el kardex (el rastro contable real) solo se escribe al
 * confirmar, cuando el SRI ya autorizó el comprobante. Patrón "reservar → confirmar/liberar" con
 * compensación dentro del mismo intento, que además deja ese rastro contable al confirmar.
 * <p>
 * Ninguno de los tres métodos públicos lleva su propia anotación
 * {@code @WithTransaction} a propósito: siempre se invocan desde
 * {@code StockCommandConsumer}, que administra una única transacción por
 * comando — reutiliza esa sesión en vez de anidar una nueva (mismo motivo
 * que {@code FirmaXadesService} en firma-electronica-service).
 */
@ApplicationScoped
public class StockReservationService {

    private final ItemRepository itemRepository;
    private final ReservaStockRepository reservaRepository;
    private final KardexService kardexService;

    @Inject
    public StockReservationService(ItemRepository itemRepository, ReservaStockRepository reservaRepository,
            KardexService kardexService) {
        this.itemRepository = itemRepository;
        this.reservaRepository = reservaRepository;
        this.kardexService = kardexService;
    }

    /**
     * Reserva el stock de los PRODUCTO de la lista; los SERVICIO no tienen stock y se ignoran (si no, facturar un
     * servicio daría "stock insuficiente"). Un ítem que no existe sí cuenta como insuficiente.
     */
    public Uni<ResultadoReserva> reservarTodos(UUID comprobanteId, List<ItemCantidadPayload> items) {
        return excluirServicios(items).onItem().transformToUni(productos -> productos.isEmpty()
            ? Uni.createFrom().item(new ResultadoReserva(true, List.of()))
            : reservarProductos(comprobanteId, productos));
    }

    private Uni<List<ItemCantidadPayload>> excluirServicios(List<ItemCantidadPayload> items) {
        return Multi.createFrom().iterable(items)
            .onItem().transformToUniAndConcatenate(item -> itemRepository.findById(item.itemId())
                .map(encontrado -> new Revisado(item, encontrado != null && encontrado.getTipo() != TipoItem.PRODUCTO)))
            .collect().asList()
            .map(revisados -> revisados.stream().filter(revisado -> !revisado.esServicio())
                .map(Revisado::item).toList());
    }

    private Uni<ResultadoReserva> reservarProductos(UUID comprobanteId, List<ItemCantidadPayload> items) {
        List<UUID> reservados = Collections.synchronizedList(new ArrayList<>());
        List<UUID> insuficientes = Collections.synchronizedList(new ArrayList<>());

        return Multi.createFrom().iterable(items)
            .onItem().transformToUniAndConcatenate(item -> itemRepository
                .intentarReservar(item.itemId(), item.cantidad())
                .onItem().invoke(filas -> {
                    if (filas != null && filas > 0) {
                        reservados.add(item.itemId());
                    } else {
                        insuficientes.add(item.itemId());
                    }
                }))
            .collect().last()
            .onItem().transformToUni(ignorado -> {
                if (!insuficientes.isEmpty()) {
                    return compensarReservasParciales(items, reservados)
                        .replaceWith(new ResultadoReserva(false, insuficientes));
                }
                return persistirReservas(comprobanteId, items).replaceWith(new ResultadoReserva(true, List.of()));
            });
    }

    public Uni<Void> confirmarTodos(UUID comprobanteId) {
        return reservaRepository.findByComprobanteIdAndEstado(comprobanteId, EstadoReserva.RESERVADO)
            .onItem().transformToMulti(reservas -> Multi.createFrom().iterable(reservas))
            .onItem().transformToUniAndConcatenate(reserva -> itemRepository
                .confirmarReserva(reserva.getItemId(), reserva.getCantidad())
                .chain(ignorado -> kardexService.registrarSalidaConfirmada(reserva.getItemId(), reserva.getCantidad(),
                    "comprobante:" + comprobanteId))
                .chain(ignorado -> reservaRepository.actualizarEstado(reserva.getId(), EstadoReserva.CONFIRMADO,
                    EstadoReserva.RESERVADO)))
            .collect().last()
            .replaceWithVoid();
    }

    public Uni<Void> liberarTodos(UUID comprobanteId) {
        return reservaRepository.findByComprobanteIdAndEstado(comprobanteId, EstadoReserva.RESERVADO)
            .onItem().transformToMulti(reservas -> Multi.createFrom().iterable(reservas))
            .onItem().transformToUniAndConcatenate(reserva -> itemRepository
                .liberarReserva(reserva.getItemId(), reserva.getCantidad())
                .chain(ignorado -> reservaRepository.actualizarEstado(reserva.getId(), EstadoReserva.LIBERADO,
                    EstadoReserva.RESERVADO)))
            .collect().last()
            .replaceWithVoid();
    }

    private Uni<Void> compensarReservasParciales(List<ItemCantidadPayload> items, List<UUID> reservados) {
        List<ItemCantidadPayload> aLiberar = items.stream().filter(item -> reservados.contains(item.itemId()))
            .toList();
        return Multi.createFrom().iterable(aLiberar)
            .onItem().transformToUniAndConcatenate(item -> itemRepository.liberarReserva(item.itemId(),
                item.cantidad()))
            .collect().last()
            .replaceWithVoid();
    }

    private Uni<Void> persistirReservas(UUID comprobanteId, List<ItemCantidadPayload> items) {
        return Multi.createFrom().iterable(items)
            .onItem().transformToUniAndConcatenate(item -> reservaRepository.persist(new ReservaStock(
                UUID.randomUUID(), comprobanteId, item.itemId(), item.cantidad(), EstadoReserva.RESERVADO,
                Instant.now())))
            .collect().last()
            .replaceWithVoid();
    }

    private record Revisado(ItemCantidadPayload item, boolean esServicio) {
    }

    public record ResultadoReserva(boolean exitoso, List<UUID> itemsInsuficientes) {
    }
}
