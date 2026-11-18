package com.facturacion.firma.application.service;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.firma.application.dto.ActualizarEstablecimientoRequest;
import com.facturacion.firma.application.dto.CrearEstablecimientoRequest;
import com.facturacion.firma.application.dto.EstablecimientoResponse;
import com.facturacion.firma.application.event.EstablecimientoPuntoEmisionEventMessage;
import com.facturacion.firma.application.mapper.EstablecimientoResponseMapper;
import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.model.entity.Establecimiento;
import com.facturacion.firma.domain.port.EventoSaliente;
import com.facturacion.firma.domain.port.EventosSalientes;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.domain.repository.EstablecimientoRepository;
import com.facturacion.firma.shared.constant.MessagingConstants;
import com.facturacion.firma.shared.error.FirmaError;

/**
 * CRUD de establecimientos de una {@link Empresa} — antes de esto,
 * {@code establecimiento} era solo un string libre en cada request de
 * creación de comprobante, sin ningún registro real detrás (ver
 * ARQUITECTURA.md, sección de establecimientos/puntos de emisión). Cada
 * mutación publica su evento en el outbox, dentro de la misma transacción,
 * para que {@code comprobantes-service} mantenga su propia copia local y
 * pueda validar antes de emitir.
 */
@ApplicationScoped
public class EstablecimientoService {

    private final EstablecimientoRepository repository;
    private final EmpresaRepository empresaRepository;
    private final EstablecimientoResponseMapper mapper;
    private final EventosSalientes salientes;
    private final ObjectMapper objectMapper;

    @Inject
    public EstablecimientoService(EstablecimientoRepository repository, EmpresaRepository empresaRepository,
            EstablecimientoResponseMapper mapper, EventosSalientes salientes, ObjectMapper objectMapper) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.mapper = mapper;
        this.salientes = salientes;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<EstablecimientoResponse> crear(UUID empresaId, @Valid CrearEstablecimientoRequest request) {
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException(FirmaError.EMPRESA_NO_ENCONTRADA, empresaId))
            .onItem().transformToUni(empresa -> repository.existsByEmpresaIdAndCodigo(empresaId, request.getCodigo())
                .onItem().transformToUni(existe -> {
                    if (Boolean.TRUE.equals(existe)) {
                        return Uni.createFrom().failure(new ConflictoException(
                            FirmaError.ESTABLECIMIENTO_DUPLICADO, request.getCodigo()));
                    }
                    Instant ahora = Instant.now();
                    Establecimiento establecimiento = new Establecimiento(UUID.randomUUID(), empresaId,
                        request.getCodigo(), request.getDireccion(), request.getTipo(), true, ahora, ahora);
                    return guardarYPublicar(empresa, establecimiento, true);
                }));
    }

    @WithTransaction
    public Uni<EstablecimientoResponse> actualizar(UUID empresaId, UUID id,
            @Valid ActualizarEstablecimientoRequest request) {
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException(FirmaError.EMPRESA_NO_ENCONTRADA, empresaId))
            .onItem().transformToUni(empresa -> repository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException(FirmaError.ESTABLECIMIENTO_NO_ENCONTRADO, id))
                .onItem().transformToUni(existente -> {
                    if (!existente.getEmpresaId().equals(empresaId)) {
                        return Uni.createFrom().failure(
                            new NotFoundException(FirmaError.ESTABLECIMIENTO_NO_ENCONTRADO, id));
                    }
                    existente.setDireccion(request.getDireccion());
                    existente.setTipo(request.getTipo());
                    existente.setActivo(request.isActivo());
                    return guardarYPublicar(empresa, existente, false);
                }));
    }

    @WithSession
    public Uni<List<EstablecimientoResponse>> listarPorEmpresa(UUID empresaId) {
        return repository.findByEmpresaId(empresaId)
            .map(items -> items.stream().map(mapper::toResponse).collect(Collectors.toList()));
    }

    @WithSession
    public Uni<EstablecimientoResponse> obtener(UUID empresaId, UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException(FirmaError.ESTABLECIMIENTO_NO_ENCONTRADO, id))
            .onItem().transformToUni(establecimiento -> {
                if (!establecimiento.getEmpresaId().equals(empresaId)) {
                    return Uni.createFrom().failure(new NotFoundException(FirmaError.ESTABLECIMIENTO_NO_ENCONTRADO, id));
                }
                return Uni.createFrom().item(mapper.toResponse(establecimiento));
            });
    }

    private Uni<EstablecimientoResponse> guardarYPublicar(Empresa empresa, Establecimiento establecimiento,
            boolean esNuevo) {
        String tipoEvento = esNuevo
            ? MessagingConstants.EVENT_TYPE_ESTABLECIMIENTO_REGISTRADO
            : MessagingConstants.EVENT_TYPE_ESTABLECIMIENTO_ACTUALIZADO;
        Uni<Establecimiento> guardado = esNuevo ? repository.persist(establecimiento)
            : repository.update(establecimiento);
        return guardado
            .chain(persistido -> salientes
                .registrar(construirEventoOutbox(empresa.getRuc(), persistido, tipoEvento))
                .replaceWith(persistido))
            .map(mapper::toResponse);
    }

    private EventoSaliente construirEventoOutbox(String ruc, Establecimiento establecimiento, String tipoEvento) {
        EstablecimientoPuntoEmisionEventMessage mensaje = new EstablecimientoPuntoEmisionEventMessage(
            UUID.randomUUID(),
            tipoEvento,
            ruc,
            establecimiento.getCodigo(),
            establecimiento.getDireccion(),
            establecimiento.getTipo(),
            null,
            null,
            establecimiento.isActivo(),
            Instant.now());
        return new EventoSaliente(establecimiento.getId(), tipoEvento,
            MessagingConstants.ESTABLECIMIENTO_PUNTO_EMISION_EVENTS_TOPIC, serializarAJson(mensaje));
    }

    private String serializarAJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ErrorInternoException(e, ErrorComun.PAYLOAD_NO_SERIALIZABLE);
        }
    }
}
