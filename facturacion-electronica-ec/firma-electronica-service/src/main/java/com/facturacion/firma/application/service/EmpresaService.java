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

import com.facturacion.firma.application.dto.ActualizarEmpresaRequest;
import com.facturacion.firma.application.dto.CrearEmpresaRequest;
import com.facturacion.firma.application.dto.EmpresaResponse;
import com.facturacion.firma.application.event.EmpresaEventMessage;
import com.facturacion.firma.application.event.EstablecimientoPuntoEmisionEventMessage;
import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.model.entity.Establecimiento;
import com.facturacion.firma.domain.model.entity.PuntoEmision;
import com.facturacion.firma.domain.model.enums.AmbienteSri;
import com.facturacion.firma.domain.model.vo.PageResult;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.domain.repository.EstablecimientoRepository;
import com.facturacion.firma.domain.repository.PuntoEmisionRepository;
import com.facturacion.firma.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.firma.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.firma.infrastructure.persistence.mapper.EmpresaMapper;
import com.facturacion.firma.shared.constant.MessagingConstants;
import com.facturacion.firma.shared.exception.NotFoundException;
import com.facturacion.firma.shared.exception.RucDuplicadoException;
import com.facturacion.firma.shared.responses.PagedResponse;

@ApplicationScoped
public class EmpresaService {

    private final EmpresaRepository repository;
    private final EstablecimientoRepository establecimientoRepository;
    private final PuntoEmisionRepository puntoEmisionRepository;
    private final EmpresaMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public EmpresaService(EmpresaRepository repository, EstablecimientoRepository establecimientoRepository,
            PuntoEmisionRepository puntoEmisionRepository, EmpresaMapper mapper,
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.establecimientoRepository = establecimientoRepository;
        this.puntoEmisionRepository = puntoEmisionRepository;
        this.mapper = mapper;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea la empresa y, en la misma transacción, su establecimiento y punto
     * de emisión por defecto (código = {@code establecimientoDefecto}/
     * {@code puntoEmisionDefecto}, tipo MATRIZ) — así ninguna empresa nueva
     * necesita un paso manual aparte para poder emitir. Dos eventos más
     * quedan listos en el outbox junto con {@code EmpresaRegistrada}: mismo
     * patrón que {@code OrderService.createOrder} en ecommerce/, con tres
     * filas en vez de una.
     */
    /**
     * @param usuarioCreadorId quien la crea (de {@code TokenPrincipal}, api-key o token de
     *                         sesión — ver {@code EmpresaResource.crear}/{@link PermiteSesion}).
     *                         Viaja en el evento {@code EmpresaRegistrada} para que
     *                         identidad-service lo registre automáticamente como
     *                         {@code ADMIN} de esta empresa — sin esto, una empresa
     *                         recién creada por sesión (sin api-key todavía, porque
     *                         no puede haber una para un ruc que no existía) quedaría
     *                         sin ningún usuario asociado y sin forma de conseguir una.
     */
    @WithTransaction
    public Uni<EmpresaResponse> crearEmpresa(@Valid CrearEmpresaRequest request, UUID usuarioCreadorId) {
        return repository.existsByRuc(request.getRuc())
            .onItem().transformToUni(existe -> {
                if (Boolean.TRUE.equals(existe)) {
                    return Uni.createFrom().failure(new RucDuplicadoException(request.getRuc()));
                }
                Instant ahora = Instant.now();
                Empresa empresa = new Empresa(
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
                return repository.persist(empresa)
                    .chain(guardado -> outboxRepository
                        .persist(construirEventoOutbox(guardado, MessagingConstants.EVENT_TYPE_EMPRESA_REGISTRADA,
                            usuarioCreadorId))
                        .replaceWith(guardado))
                    .chain(this::crearEstablecimientoYPuntoEmisionPorDefecto)
                    .map(mapper::toResponse);
            });
    }

    /**
     * Establecimiento MATRIZ + punto de emisión, ambos con el código por
     * defecto de la empresa — ver {@link EstablecimientoService}/
     * {@link PuntoEmisionService} para el CRUD completo de sucursales y
     * puntos adicionales, que este método no reemplaza.
     */
    private Uni<Empresa> crearEstablecimientoYPuntoEmisionPorDefecto(Empresa empresa) {
        Instant ahora = Instant.now();
        Establecimiento establecimiento = new Establecimiento(UUID.randomUUID(), empresa.getId(),
            empresa.getEstablecimientoDefecto(), empresa.getDireccionMatriz(), "MATRIZ", true, ahora, ahora);
        return establecimientoRepository.persist(establecimiento)
            .chain(establecimientoGuardado -> outboxRepository
                .persist(construirEventoEstablecimientoOutbox(empresa.getRuc(), establecimientoGuardado,
                    MessagingConstants.EVENT_TYPE_ESTABLECIMIENTO_REGISTRADO))
                .replaceWith(establecimientoGuardado))
            .chain(establecimientoGuardado -> {
                PuntoEmision puntoEmision = new PuntoEmision(UUID.randomUUID(), establecimientoGuardado.getId(),
                    empresa.getPuntoEmisionDefecto(), null, true, ahora, ahora);
                return puntoEmisionRepository.persist(puntoEmision)
                    .chain(puntoEmisionGuardado -> outboxRepository
                        .persist(construirEventoPuntoEmisionOutbox(empresa.getRuc(), establecimientoGuardado,
                            puntoEmisionGuardado, MessagingConstants.EVENT_TYPE_PUNTO_EMISION_REGISTRADO))
                        .replaceWithVoid());
            })
            .replaceWith(empresa);
    }

    private OutboxEventEntity construirEventoEstablecimientoOutbox(String ruc, Establecimiento establecimiento,
            String tipoEvento) {
        EstablecimientoPuntoEmisionEventMessage mensaje = new EstablecimientoPuntoEmisionEventMessage(
            UUID.randomUUID(), tipoEvento, ruc, establecimiento.getCodigo(), establecimiento.getDireccion(),
            establecimiento.getTipo(), null, null, establecimiento.isActivo(), Instant.now());
        return OutboxEventEntity.of(establecimiento.getId(), tipoEvento,
            MessagingConstants.ESTABLECIMIENTO_PUNTO_EMISION_EVENTS_TOPIC, writeValueAsString(mensaje));
    }

    private OutboxEventEntity construirEventoPuntoEmisionOutbox(String ruc, Establecimiento establecimiento,
            PuntoEmision puntoEmision, String tipoEvento) {
        EstablecimientoPuntoEmisionEventMessage mensaje = new EstablecimientoPuntoEmisionEventMessage(
            UUID.randomUUID(), tipoEvento, ruc, establecimiento.getCodigo(), null, null, puntoEmision.getCodigo(),
            puntoEmision.getDescripcion(), puntoEmision.isActivo(), Instant.now());
        return OutboxEventEntity.of(puntoEmision.getId(), tipoEvento,
            MessagingConstants.ESTABLECIMIENTO_PUNTO_EMISION_EVENTS_TOPIC, writeValueAsString(mensaje));
    }

    @WithTransaction
    public Uni<EmpresaResponse> actualizarEmpresa(UUID id, @Valid ActualizarEmpresaRequest request) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + id))
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
                        .persist(construirEventoOutbox(actualizado, MessagingConstants.EVENT_TYPE_EMPRESA_ACTUALIZADA,
                            null))
                        .replaceWith(actualizado))
                    .map(mapper::toResponse);
            });
    }

    @WithSession
    public Uni<EmpresaResponse> obtenerPorId(UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + id))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<PagedResponse<EmpresaResponse>> listarPaginado(int page, int limit) {
        return repository.findPaged(page, limit)
            .map((PageResult<Empresa> pageResult) -> {
                List<EmpresaResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }

    private OutboxEventEntity construirEventoOutbox(Empresa empresa, String tipoEvento, UUID usuarioCreadorId) {
        EmpresaEventMessage mensaje = new EmpresaEventMessage(
            UUID.randomUUID(),
            empresa.getId(),
            tipoEvento,
            empresa.getRuc(),
            empresa.getRazonSocial(),
            empresa.getNombreComercial(),
            empresa.getDireccionMatriz(),
            empresa.isObligadoContabilidad(),
            empresa.isContribuyenteEspecial(),
            empresa.getNumeroContribuyenteEspecial(),
            empresa.isEsGranContribuyente(),
            empresa.getNumeroResolucionGranContribuyente(),
            empresa.isVendeCombustibles(),
            empresa.getAmbiente().name(),
            empresa.getEstablecimientoDefecto(),
            empresa.getPuntoEmisionDefecto(),
            empresa.isActivo(),
            usuarioCreadorId,
            Instant.now());
        return OutboxEventEntity.of(empresa.getId(), tipoEvento,
            MessagingConstants.EMPRESA_EVENTS_TOPIC, writeValueAsString(mensaje));
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
