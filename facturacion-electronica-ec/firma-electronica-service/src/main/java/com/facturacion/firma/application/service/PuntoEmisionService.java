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

import com.facturacion.firma.application.dto.ActualizarPuntoEmisionRequest;
import com.facturacion.firma.application.dto.CrearPuntoEmisionRequest;
import com.facturacion.firma.application.dto.PuntoEmisionResponse;
import com.facturacion.firma.application.event.EstablecimientoPuntoEmisionEventMessage;
import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.model.entity.Establecimiento;
import com.facturacion.firma.domain.model.entity.PuntoEmision;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.domain.repository.EstablecimientoRepository;
import com.facturacion.firma.domain.repository.PuntoEmisionRepository;
import com.facturacion.firma.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.firma.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.firma.infrastructure.persistence.mapper.PuntoEmisionMapper;
import com.facturacion.firma.shared.constant.MessagingConstants;
import com.facturacion.firma.shared.exception.CodigoDuplicadoException;
import com.facturacion.firma.shared.exception.NotFoundException;

/** CRUD de puntos de emisión de un {@link Establecimiento} — ver {@link EstablecimientoService}. */
@ApplicationScoped
public class PuntoEmisionService {

    private final PuntoEmisionRepository repository;
    private final EstablecimientoRepository establecimientoRepository;
    private final EmpresaRepository empresaRepository;
    private final PuntoEmisionMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public PuntoEmisionService(PuntoEmisionRepository repository, EstablecimientoRepository establecimientoRepository,
            EmpresaRepository empresaRepository, PuntoEmisionMapper mapper, OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.establecimientoRepository = establecimientoRepository;
        this.empresaRepository = empresaRepository;
        this.mapper = mapper;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<PuntoEmisionResponse> crear(UUID empresaId, UUID establecimientoId,
            @Valid CrearPuntoEmisionRequest request) {
        return resolverEstablecimientoYEmpresa(empresaId, establecimientoId)
            .onItem().transformToUni(par -> repository
                .existsByEstablecimientoIdAndCodigo(establecimientoId, request.getCodigo())
                .onItem().transformToUni(existe -> {
                    if (Boolean.TRUE.equals(existe)) {
                        return Uni.createFrom().failure(new CodigoDuplicadoException(
                            "Ya existe un punto de emisión activo con el código " + request.getCodigo()
                                + " para este establecimiento"));
                    }
                    Instant ahora = Instant.now();
                    PuntoEmision puntoEmision = new PuntoEmision(UUID.randomUUID(), establecimientoId,
                        request.getCodigo(), request.getDescripcion(), true, ahora, ahora);
                    return guardarYPublicar(par.empresa(), par.establecimiento(), puntoEmision, true);
                }));
    }

    @WithTransaction
    public Uni<PuntoEmisionResponse> actualizar(UUID empresaId, UUID establecimientoId, UUID id,
            @Valid ActualizarPuntoEmisionRequest request) {
        return resolverEstablecimientoYEmpresa(empresaId, establecimientoId)
            .onItem().transformToUni(par -> repository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Punto de emisión no encontrado: " + id))
                .onItem().transformToUni(existente -> {
                    if (!existente.getEstablecimientoId().equals(establecimientoId)) {
                        return Uni.createFrom().failure(
                            new NotFoundException("Punto de emisión no encontrado: " + id));
                    }
                    existente.setDescripcion(request.getDescripcion());
                    existente.setActivo(request.isActivo());
                    return guardarYPublicar(par.empresa(), par.establecimiento(), existente, false);
                }));
    }

    @WithSession
    public Uni<List<PuntoEmisionResponse>> listarPorEstablecimiento(UUID establecimientoId) {
        return repository.findByEstablecimientoId(establecimientoId)
            .map(items -> items.stream().map(mapper::toResponse).collect(Collectors.toList()));
    }

    @WithSession
    public Uni<PuntoEmisionResponse> obtener(UUID establecimientoId, UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Punto de emisión no encontrado: " + id))
            .onItem().transformToUni(puntoEmision -> {
                if (!puntoEmision.getEstablecimientoId().equals(establecimientoId)) {
                    return Uni.createFrom().failure(new NotFoundException("Punto de emisión no encontrado: " + id));
                }
                return Uni.createFrom().item(mapper.toResponse(puntoEmision));
            });
    }

    private record EmpresaEstablecimiento(Empresa empresa, Establecimiento establecimiento) {
    }

    private Uni<EmpresaEstablecimiento> resolverEstablecimientoYEmpresa(UUID empresaId, UUID establecimientoId) {
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + empresaId))
            .onItem().transformToUni(empresa -> establecimientoRepository.findById(establecimientoId)
                .onItem().ifNull().failWith(() ->
                    new NotFoundException("Establecimiento no encontrado: " + establecimientoId))
                .onItem().transformToUni(establecimiento -> {
                    if (!establecimiento.getEmpresaId().equals(empresaId)) {
                        return Uni.createFrom().failure(
                            new NotFoundException("Establecimiento no encontrado: " + establecimientoId));
                    }
                    return Uni.createFrom().item(new EmpresaEstablecimiento(empresa, establecimiento));
                }));
    }

    private Uni<PuntoEmisionResponse> guardarYPublicar(Empresa empresa, Establecimiento establecimiento,
            PuntoEmision puntoEmision, boolean esNuevo) {
        String tipoEvento = esNuevo
            ? MessagingConstants.EVENT_TYPE_PUNTO_EMISION_REGISTRADO
            : MessagingConstants.EVENT_TYPE_PUNTO_EMISION_ACTUALIZADO;
        Uni<PuntoEmision> guardado = esNuevo ? repository.persist(puntoEmision) : repository.update(puntoEmision);
        return guardado
            .chain(persistido -> outboxRepository
                .persist(construirEventoOutbox(empresa.getRuc(), establecimiento.getCodigo(), persistido, tipoEvento))
                .replaceWith(persistido))
            .map(mapper::toResponse);
    }

    private OutboxEventEntity construirEventoOutbox(String ruc, String establecimientoCodigo,
            PuntoEmision puntoEmision, String tipoEvento) {
        EstablecimientoPuntoEmisionEventMessage mensaje = new EstablecimientoPuntoEmisionEventMessage(
            UUID.randomUUID(),
            tipoEvento,
            ruc,
            establecimientoCodigo,
            null,
            null,
            puntoEmision.getCodigo(),
            puntoEmision.getDescripcion(),
            puntoEmision.isActivo(),
            Instant.now());
        return OutboxEventEntity.of(puntoEmision.getId(), tipoEvento,
            MessagingConstants.ESTABLECIMIENTO_PUNTO_EMISION_EVENTS_TOPIC, writeValueAsString(mensaje));
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
