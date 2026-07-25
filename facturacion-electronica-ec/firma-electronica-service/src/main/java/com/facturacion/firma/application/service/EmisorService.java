package com.facturacion.firma.application.service;

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

import com.facturacion.firma.application.dto.ActualizarEmisorRequest;
import com.facturacion.firma.application.dto.CrearEmisorRequest;
import com.facturacion.firma.application.dto.EmisorResponse;
import com.facturacion.firma.application.event.EmisorEventMessage;
import com.facturacion.firma.domain.model.entity.Emisor;
import com.facturacion.firma.domain.model.enums.AmbienteSri;
import com.facturacion.firma.domain.model.vo.PageResult;
import com.facturacion.firma.domain.repository.EmisorRepository;
import com.facturacion.firma.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.firma.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.firma.infrastructure.persistence.mapper.EmisorMapper;
import com.facturacion.firma.shared.constant.MessagingConstants;
import com.facturacion.firma.shared.exception.NotFoundException;
import com.facturacion.firma.shared.exception.RucDuplicadoException;
import com.facturacion.firma.shared.responses.PagedResponse;

@ApplicationScoped
public class EmisorService {

    private final EmisorRepository repository;
    private final EmisorMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public EmisorService(EmisorRepository repository, EmisorMapper mapper,
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea el emisor y deja su evento {@code EmisorRegistrado} listo en la
     * tabla outbox, dentro de la misma transacción — mismo patrón que
     * {@code OrderService.createOrder} en ecommerce/.
     */
    @WithTransaction
    public Uni<EmisorResponse> crearEmisor(@Valid CrearEmisorRequest request) {
        return repository.existsByRuc(request.getRuc())
            .onItem().transformToUni(existe -> {
                if (Boolean.TRUE.equals(existe)) {
                    return Uni.createFrom().failure(new RucDuplicadoException(request.getRuc()));
                }
                Instant ahora = Instant.now();
                Emisor emisor = new Emisor(
                    UUID.randomUUID(),
                    request.getRuc(),
                    request.getRazonSocial(),
                    request.getNombreComercial(),
                    request.getDireccionMatriz(),
                    request.isObligadoContabilidad(),
                    request.isContribuyenteEspecial(),
                    request.getNumeroContribuyenteEspecial(),
                    request.isEsGranContribuyente(),
                    request.getNumeroResolucionGranContribuyente(),
                    request.isVendeCombustibles(),
                    AmbienteSri.valueOf(request.getAmbiente()),
                    request.getEstablecimientoDefecto(),
                    request.getPuntoEmisionDefecto(),
                    true,
                    ahora,
                    ahora);
                return repository.persist(emisor)
                    .chain(guardado -> outboxRepository
                        .persist(construirEventoOutbox(guardado, MessagingConstants.EVENT_TYPE_EMISOR_REGISTRADO))
                        .replaceWith(guardado))
                    .map(mapper::toResponse);
            });
    }

    @WithTransaction
    public Uni<EmisorResponse> actualizarEmisor(UUID id, @Valid ActualizarEmisorRequest request) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Emisor no encontrado: " + id))
            .onItem().transformToUni(existente -> {
                existente.setRazonSocial(request.getRazonSocial());
                existente.setNombreComercial(request.getNombreComercial());
                existente.setDireccionMatriz(request.getDireccionMatriz());
                existente.setObligadoContabilidad(request.isObligadoContabilidad());
                existente.setContribuyenteEspecial(request.isContribuyenteEspecial());
                existente.setNumeroContribuyenteEspecial(request.getNumeroContribuyenteEspecial());
                existente.setEsGranContribuyente(request.isEsGranContribuyente());
                existente.setNumeroResolucionGranContribuyente(request.getNumeroResolucionGranContribuyente());
                existente.setVendeCombustibles(request.isVendeCombustibles());
                existente.setAmbiente(AmbienteSri.valueOf(request.getAmbiente()));
                if (request.getEstablecimientoDefecto() != null) {
                    existente.setEstablecimientoDefecto(request.getEstablecimientoDefecto());
                }
                if (request.getPuntoEmisionDefecto() != null) {
                    existente.setPuntoEmisionDefecto(request.getPuntoEmisionDefecto());
                }
                existente.setActivo(request.isActivo());
                return repository.update(existente)
                    .chain(actualizado -> outboxRepository
                        .persist(construirEventoOutbox(actualizado, MessagingConstants.EVENT_TYPE_EMISOR_ACTUALIZADO))
                        .replaceWith(actualizado))
                    .map(mapper::toResponse);
            });
    }

    @WithSession
    public Uni<EmisorResponse> obtenerPorId(UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Emisor no encontrado: " + id))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<PagedResponse<EmisorResponse>> listarPaginado(int page, int limit) {
        return repository.findPaged(page, limit)
            .map((PageResult<Emisor> pageResult) -> {
                List<EmisorResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }

    private OutboxEventEntity construirEventoOutbox(Emisor emisor, String tipoEvento) {
        EmisorEventMessage mensaje = new EmisorEventMessage(
            UUID.randomUUID(),
            emisor.getId(),
            tipoEvento,
            emisor.getRuc(),
            emisor.getRazonSocial(),
            emisor.getNombreComercial(),
            emisor.getDireccionMatriz(),
            emisor.isObligadoContabilidad(),
            emisor.isContribuyenteEspecial(),
            emisor.getNumeroContribuyenteEspecial(),
            emisor.isEsGranContribuyente(),
            emisor.getNumeroResolucionGranContribuyente(),
            emisor.isVendeCombustibles(),
            emisor.getAmbiente().name(),
            emisor.getEstablecimientoDefecto(),
            emisor.getPuntoEmisionDefecto(),
            emisor.isActivo(),
            Instant.now());
        return OutboxEventEntity.of(emisor.getId(), tipoEvento,
            MessagingConstants.EMISOR_EVENTS_TOPIC, writeValueAsString(mensaje));
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
