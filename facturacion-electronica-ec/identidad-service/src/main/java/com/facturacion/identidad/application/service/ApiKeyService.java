package com.facturacion.identidad.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.application.dto.ApiKeyCreadaResponse;
import com.facturacion.identidad.application.dto.ApiKeyResponse;
import com.facturacion.identidad.application.dto.CrearApiKeyRequest;
import com.facturacion.identidad.domain.model.entity.ApiKey;
import com.facturacion.identidad.domain.repository.ApiKeyRepository;
import com.facturacion.identidad.domain.repository.EstablecimientoLocalRepository;
import com.facturacion.identidad.domain.repository.PuntoEmisionLocalRepository;
import com.facturacion.identidad.domain.repository.UsuarioEmpresaRolRepository;
import com.facturacion.identidad.infrastructure.crypto.JwtService;
import com.facturacion.identidad.infrastructure.persistence.mapper.ApiKeyMapper;
import com.facturacion.identidad.shared.exception.ConflictoException;
import com.facturacion.identidad.shared.exception.NotFoundException;
import com.facturacion.identidad.shared.exception.SinAsociacionException;

/**
 * Dueño del agregado ApiKey. La key en sí es un JWT (ver {@link JwtService})
 * — este servicio nunca la vuelve a mostrar después de crearla, solo guarda
 * su {@code jti} para poder listarla/revocarla (revocación de mejor
 * esfuerzo, ver V1__init.sql).
 */
@ApplicationScoped
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UsuarioEmpresaRolRepository empresaRolRepository;
    private final EstablecimientoLocalRepository establecimientoLocalRepository;
    private final PuntoEmisionLocalRepository puntoEmisionLocalRepository;
    private final ApiKeyMapper mapper;
    private final JwtService jwtService;
    private final int maxDiasExpiracion;

    @Inject
    public ApiKeyService(ApiKeyRepository apiKeyRepository, UsuarioEmpresaRolRepository empresaRolRepository,
            EstablecimientoLocalRepository establecimientoLocalRepository,
            PuntoEmisionLocalRepository puntoEmisionLocalRepository, ApiKeyMapper mapper, JwtService jwtService,
            @ConfigProperty(name = "identidad.jwt.apikey.max-dias") int maxDiasExpiracion) {
        this.apiKeyRepository = apiKeyRepository;
        this.empresaRolRepository = empresaRolRepository;
        this.establecimientoLocalRepository = establecimientoLocalRepository;
        this.puntoEmisionLocalRepository = puntoEmisionLocalRepository;
        this.mapper = mapper;
        this.jwtService = jwtService;
        this.maxDiasExpiracion = maxDiasExpiracion;
    }

    @WithTransaction
    public Uni<ApiKeyCreadaResponse> crear(UUID usuarioId, @Valid CrearApiKeyRequest request) {
        if (request.getPuntoEmision() != null && request.getEstablecimiento() == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("puntoEmision requiere que también se indique establecimiento"));
        }
        return empresaRolRepository.findByUsuarioIdAndRuc(usuarioId, request.getRuc())
            .onItem().transformToUni(asociacion -> {
                if (asociacion == null) {
                    return Uni.createFrom().failure(new SinAsociacionException(request.getRuc()));
                }
                return validarAlcance(request).onItem().transformToUni(ignorado -> apiKeyRepository
                    .existsByUsuarioIdAndNombre(usuarioId, request.getNombre())
                    .onItem().transformToUni(existe -> {
                        if (Boolean.TRUE.equals(existe)) {
                            return Uni.createFrom().failure(
                                new ConflictoException("Ya existe una api-key llamada '" + request.getNombre() + "' para este usuario"));
                        }
                        int dias = Math.min(request.getExpiracionEnDias() == null || request.getExpiracionEnDias() <= 0
                            ? maxDiasExpiracion
                            : request.getExpiracionEnDias(), maxDiasExpiracion);
                        Instant ahora = Instant.now();
                        Instant expiracion = ahora.plusSeconds(dias * 86400L);
                        String jti = UUID.randomUUID().toString();
                        String valor = jwtService.generarApiKey(usuarioId, request.getRuc(), asociacion.getRol(),
                            request.getEstablecimiento(), request.getPuntoEmision(), jti, expiracion);

                        ApiKey apiKey = new ApiKey();
                        apiKey.setId(UUID.randomUUID());
                        apiKey.setUsuarioId(usuarioId);
                        apiKey.setNombre(request.getNombre());
                        apiKey.setRuc(request.getRuc());
                        apiKey.setRol(asociacion.getRol());
                        apiKey.setEstablecimiento(request.getEstablecimiento());
                        apiKey.setPuntoEmision(request.getPuntoEmision());
                        apiKey.setJti(jti);
                        apiKey.setFechaCreacion(ahora);
                        apiKey.setFechaExpiracion(expiracion);
                        apiKey.setRevocada(false);

                        return apiKeyRepository.persist(apiKey).map(guardada -> {
                            ApiKeyCreadaResponse response = new ApiKeyCreadaResponse();
                            response.setId(guardada.getId());
                            response.setNombre(guardada.getNombre());
                            response.setRuc(guardada.getRuc());
                            response.setRol(guardada.getRol().name());
                            response.setEstablecimiento(guardada.getEstablecimiento());
                            response.setPuntoEmision(guardada.getPuntoEmision());
                            response.setValor(valor);
                            response.setFechaCreacion(guardada.getFechaCreacion());
                            response.setFechaExpiracion(guardada.getFechaExpiracion());
                            return response;
                        });
                    }));
            });
    }

    /**
     * Si el request no pide acotar la key (caso normal, sin cambios respecto a
     * antes de que este campo existiera), no valida nada. Si pide acotar,
     * confirma que el establecimiento/punto de emisión sean reales y estén
     * activos para ese ruc — ver {@code EstablecimientoLocal}/{@code PuntoEmisionLocal}.
     */
    private Uni<Void> validarAlcance(CrearApiKeyRequest request) {
        if (request.getEstablecimiento() == null) {
            return Uni.createFrom().voidItem();
        }
        return establecimientoLocalRepository.findByRucAndCodigo(request.getRuc(), request.getEstablecimiento())
            .onItem().transformToUni(establecimiento -> {
                if (establecimiento == null || !establecimiento.isActivo()) {
                    return Uni.createFrom().failure(new NotFoundException(
                        "No existe un establecimiento activo '" + request.getEstablecimiento() + "' para el ruc "
                            + request.getRuc()));
                }
                if (request.getPuntoEmision() == null) {
                    return Uni.createFrom().voidItem();
                }
                return puntoEmisionLocalRepository.findByRucAndEstablecimientoCodigoAndCodigo(request.getRuc(),
                        request.getEstablecimiento(), request.getPuntoEmision())
                    .onItem().transformToUni(puntoEmision -> {
                        if (puntoEmision == null || !puntoEmision.isActivo()) {
                            return Uni.createFrom().failure(new NotFoundException(
                                "No existe un punto de emisión activo '" + request.getPuntoEmision()
                                    + "' para el establecimiento '" + request.getEstablecimiento() + "' del ruc "
                                    + request.getRuc()));
                        }
                        return Uni.createFrom().voidItem();
                    });
            });
    }

    @WithSession
    public Uni<List<ApiKeyResponse>> listar(UUID usuarioId) {
        return apiKeyRepository.findByUsuarioId(usuarioId)
            .map(lista -> lista.stream().map(mapper::toResponse).collect(Collectors.toList()));
    }

    @WithTransaction
    public Uni<Void> revocar(UUID usuarioId, UUID apiKeyId) {
        return apiKeyRepository.revocar(apiKeyId, usuarioId)
            .onItem().transformToUni(filas -> filas == 0
                ? Uni.createFrom().failure(new NotFoundException(
                    "No hay una api-key activa " + apiKeyId + " para este usuario"))
                : Uni.createFrom().voidItem());
    }
}
