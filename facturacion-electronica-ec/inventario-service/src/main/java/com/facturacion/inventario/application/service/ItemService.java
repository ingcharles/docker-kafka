package com.facturacion.inventario.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.application.dto.ActualizarItemRequest;
import com.facturacion.inventario.application.dto.CrearItemRequest;
import com.facturacion.inventario.application.dto.ItemResponse;
import com.facturacion.inventario.application.dto.RegistrarEntradaKardexRequest;
import com.facturacion.inventario.application.event.ItemEventMessage;
import com.facturacion.inventario.domain.model.entity.Categoria;
import com.facturacion.inventario.domain.model.entity.Item;
import com.facturacion.inventario.domain.model.enums.TipoCategoria;
import com.facturacion.inventario.domain.model.enums.TipoItem;
import com.facturacion.inventario.domain.model.vo.PageResult;
import com.facturacion.inventario.domain.repository.CategoriaRepository;
import com.facturacion.inventario.domain.repository.ItemRepository;
import com.facturacion.inventario.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.inventario.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.inventario.infrastructure.persistence.mapper.ItemMapper;
import com.facturacion.inventario.shared.constant.MessagingConstants;
import com.facturacion.inventario.shared.exception.CategoriaIncompatibleException;
import com.facturacion.inventario.shared.exception.NotFoundException;
import com.facturacion.inventario.shared.responses.PagedResponse;

/**
 * {@code Item.tipo} existe como campo propio — deliberadamente no se deriva
 * de {@code Categoria.tipo} — por dos razones concretas:
 * <ol>
 *   <li>{@code categoriaId} es opcional (nullable): un ítem sin categoría
 *       asignada igual necesita saber si es PRODUCTO o SERVICIO para que el
 *       kardex, las reservas de stock y la validación de imágenes
 *       funcionen — esas reglas no pueden depender de un dato que puede no
 *       existir.</li>
 *   <li>{@code Categoria.tipo} admite {@code AMBOS} — una categoría
 *       "AMBOS" no alcanza para saber si un ítem concreto que la usa es un
 *       PRODUCTO o un SERVICIO; el tipo de la categoría solo restringe qué
 *       categorías puede usar cada ítem, nunca sustituye al tipo del ítem.</li>
 * </ol>
 * Lo que sí faltaba (y este servicio ya valida) es la regla inversa: que la
 * categoría elegida sea compatible con el tipo del ítem — ver
 * {@link #validarCategoriaCompatible}.
 */
@ApplicationScoped
public class ItemService {

    private final ItemRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final ItemMapper mapper;
    private final KardexService kardexService;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public ItemService(ItemRepository repository, CategoriaRepository categoriaRepository, ItemMapper mapper,
            KardexService kardexService, OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.mapper = mapper;
        this.kardexService = kardexService;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Si el ítem es un PRODUCTO y viene con {@code cantidadInicial > 0}, se
     * persiste primero con existencias en cero y de inmediato se le aplica
     * una entrada de kardex — así el primer movimiento del kardex siempre
     * queda registrado (nunca se "aparece" stock sin rastro contable).
     */
    @WithTransaction
    public Uni<ItemResponse> crear(@Valid CrearItemRequest request) {
        TipoItem tipo = TipoItem.valueOf(request.getTipo());
        Instant ahora = Instant.now();
        Item item = new Item(UUID.randomUUID(), request.getRuc(), tipo, request.getCategoriaId(),
            request.getCodigoPrincipal(), request.getCodigoAuxiliar(), request.getDescripcion(),
            request.getPrecioUnitario(), request.getUnidadMedida(), BigDecimal.ZERO, BigDecimal.ZERO,
            tipo == TipoItem.PRODUCTO ? request.getStockMinimo() : BigDecimal.ZERO, true,
            request.getCodigoImpuesto(), request.getCodigoPorcentaje(), request.getTarifaIva(),
            request.isTieneIce(), request.getCodigoPorcentajeIce(), request.getTarifaIce(), ahora, ahora);

        return validarCategoriaCompatible(tipo, request.getCategoriaId())
            .chain(() -> repository.persist(item))
            .onItem().transformToUni(guardado -> {
                boolean requiereEntradaInicial = tipo == TipoItem.PRODUCTO
                    && request.getCantidadInicial() != null
                    && request.getCantidadInicial().compareTo(BigDecimal.ZERO) > 0;
                if (!requiereEntradaInicial) {
                    return Uni.createFrom().item(guardado);
                }
                RegistrarEntradaKardexRequest entrada = new RegistrarEntradaKardexRequest();
                entrada.setCantidad(request.getCantidadInicial());
                entrada.setCostoUnitario(request.getCostoInicial());
                entrada.setMotivo("Existencia inicial");
                // OJO: no se vuelve a leer el ítem con repository.findById tras el
                // UPDATE masivo (bulk) que hace incrementarDisponible — dentro de la
                // misma sesión de Hibernate Reactive, ese UPDATE no pasa por el
                // caché de primer nivel, así que un findById devolvería la MISMA
                // instancia ya cargada en memoria, con el valor viejo (0). Como ya
                // sabemos exactamente cuánto se sumó, se refleja aquí directamente.
                return kardexService.registrarEntrada(guardado.getId(), entrada)
                    .onItem().transform(ignorado -> {
                        guardado.setCantidadDisponible(request.getCantidadInicial());
                        return guardado;
                    });
            })
            .chain(guardado -> publicarEvento(guardado, MessagingConstants.EVENT_TYPE_ITEM_CREADO)
                .replaceWith(guardado))
            .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<ItemResponse> actualizar(UUID id, @Valid ActualizarItemRequest request) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Ítem no encontrado: " + id))
            .onItem().transformToUni(existente -> validarCategoriaCompatible(existente.getTipo(),
                    request.getCategoriaId())
                .chain(() -> {
                    existente.setCategoriaId(request.getCategoriaId());
                    existente.setDescripcion(request.getDescripcion());
                    existente.setPrecioUnitario(request.getPrecioUnitario());
                    existente.setUnidadMedida(request.getUnidadMedida());
                    if (existente.esProducto() && request.getStockMinimo() != null) {
                        existente.setStockMinimo(request.getStockMinimo());
                    }
                    existente.setActivo(request.isActivo());
                    existente.setCodigoImpuesto(request.getCodigoImpuesto());
                    existente.setCodigoPorcentaje(request.getCodigoPorcentaje());
                    existente.setTarifaIva(request.getTarifaIva());
                    existente.setTieneIce(request.isTieneIce());
                    existente.setCodigoPorcentajeIce(request.getCodigoPorcentajeIce());
                    existente.setTarifaIce(request.getTarifaIce());
                    return repository.update(existente);
                }))
            .chain(guardado -> publicarEvento(guardado, MessagingConstants.EVENT_TYPE_ITEM_ACTUALIZADO)
                .replaceWith(guardado))
            .map(mapper::toResponse);
    }

    private Uni<Void> publicarEvento(Item item, String eventType) {
        ItemEventMessage mensaje = new ItemEventMessage(
            UUID.randomUUID(),
            item.getId(),
            eventType,
            item.getRuc(),
            item.getTipo().name(),
            item.getCodigoPrincipal(),
            item.getCodigoAuxiliar(),
            item.getDescripcion(),
            item.getCodigoImpuesto(),
            item.getCodigoPorcentaje(),
            item.getTarifaIva(),
            item.isTieneIce(),
            item.getCodigoPorcentajeIce(),
            item.getTarifaIce(),
            item.isActivo(),
            Instant.now());
        OutboxEventEntity evento = OutboxEventEntity.of(item.getId(), eventType,
            MessagingConstants.ITEM_EVENTS_TOPIC, writeValueAsString(mensaje));
        return outboxRepository.persist(evento).replaceWithVoid();
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }

    /**
     * Una categoría {@code tipo=AMBOS} admite cualquier ítem; una categoría
     * {@code tipo=PRODUCTO} o {@code tipo=SERVICIO} solo admite ítems de ese
     * mismo tipo. Sin categoría asignada ({@code categoriaId == null}) no
     * hay nada que validar.
     */
    private Uni<Void> validarCategoriaCompatible(TipoItem tipoItem, UUID categoriaId) {
        if (categoriaId == null) {
            return Uni.createFrom().voidItem();
        }
        return categoriaRepository.findById(categoriaId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Categoría no encontrada: " + categoriaId))
            .onItem().transform(Categoria::getTipo)
            .onItem().transformToUni(tipoCategoria -> {
                boolean compatible = tipoCategoria == TipoCategoria.AMBOS
                    || tipoCategoria.name().equals(tipoItem.name());
                if (!compatible) {
                    return Uni.createFrom().failure(new CategoriaIncompatibleException(tipoItem, tipoCategoria));
                }
                return Uni.createFrom().voidItem();
            });
    }

    @WithSession
    public Uni<ItemResponse> obtenerPorId(UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Ítem no encontrado: " + id))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<PagedResponse<ItemResponse>> listarPaginado(int page, int limit, TipoItem tipo, UUID categoriaId,
            String ruc) {
        return repository.findPaged(page, limit, tipo, categoriaId, ruc)
            .map((PageResult<Item> pageResult) -> {
                List<ItemResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }
}
