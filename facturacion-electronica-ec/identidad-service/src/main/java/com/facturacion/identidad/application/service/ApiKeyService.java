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

import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.identidad.application.dto.ApiKeyCreadaResponse;
import com.facturacion.identidad.application.dto.ApiKeyResponse;
import com.facturacion.identidad.application.dto.CrearApiKeyRequest;
import com.facturacion.identidad.application.mapper.ApiKeyResponseMapper;
import com.facturacion.identidad.domain.model.entity.ApiKey;
import com.facturacion.identidad.domain.port.EmisorTokens;
import com.facturacion.identidad.domain.port.RevocacionApiKeysPublicador;
import com.facturacion.identidad.domain.repository.ApiKeyRepository;
import com.facturacion.identidad.domain.repository.EstablecimientoLocalRepository;
import com.facturacion.identidad.domain.repository.PuntoEmisionLocalRepository;
import com.facturacion.identidad.domain.repository.UsuarioEmpresaRolRepository;
import com.facturacion.identidad.shared.exception.SinAsociacionException;

/**
 * Dueño del agregado ApiKey. La key en sí es un JWT (ver {@link EmisorTokens})
 * — este servicio nunca la vuelve a mostrar después de crearla, solo guarda
 * su {@code jti} para poder listarla y revocarla. Al revocar se publica el jti
 * en {@code facturacion.apikey.revocaciones.v1} y cada servicio la rechaza sin esperar a que venza.
 */
@ApplicationScoped
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UsuarioEmpresaRolRepository empresaRolRepository;
    private final EstablecimientoLocalRepository establecimientoLocalRepository;
    private final PuntoEmisionLocalRepository puntoEmisionLocalRepository;
    private final ApiKeyResponseMapper mapper;
    private final EmisorTokens jwtService;
    private final RevocacionApiKeysPublicador publicador;
    private final int maxDiasExpiracion;

    @Inject
    public ApiKeyService(ApiKeyRepository apiKeyRepository, UsuarioEmpresaRolRepository empresaRolRepository,
            EstablecimientoLocalRepository establecimientoLocalRepository,
            PuntoEmisionLocalRepository puntoEmisionLocalRepository, ApiKeyResponseMapper mapper, EmisorTokens jwtService,
            RevocacionApiKeysPublicador publicador,
            @ConfigProperty(name = "identidad.jwt.apikey.max-dias") int maxDiasExpiracion) {
        this.apiKeyRepository = apiKeyRepository;
        this.empresaRolRepository = empresaRolRepository;
        this.establecimientoLocalRepository = establecimientoLocalRepository;
        this.puntoEmisionLocalRepository = puntoEmisionLocalRepository;
        this.mapper = mapper;
        this.jwtService = jwtService;
        this.publicador = publicador;
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

    /**
     * Revoca una api-key del usuario y avisa al resto de servicios para que la rechacen de inmediato. Es idempotente:
     * si ya estaba revocada y aún no vence, reenvía el aviso (por si el anterior no llegó) y responde igual que la
     * primera vez.
     */
    @WithTransaction
    public Uni<Void> revocar(UUID usuarioId, UUID apiKeyId) {
        return apiKeyRepository.findById(apiKeyId)
            .onItem().transformToUni(apiKey -> {
                if (apiKey == null || !apiKey.getUsuarioId().equals(usuarioId)) {
                    return Uni.createFrom().failure(new NotFoundException(
                        "No hay una api-key " + apiKeyId + " para este usuario"));
                }
                return revocarYAvisar(apiKey);
            });
    }

    /**
     * Revoca todas las api-keys vigentes de un usuario sobre un ruc. Se usa cuando pierde el acceso a la empresa o le
     * cambian el rol: el token lleva el rol de cuando se emitió, así que sin esto seguiría operando con el rol
     * anterior hasta que la key venza (hasta un año).
     */
    @WithTransaction
    public Uni<Void> revocarDeUsuarioEnRuc(UUID usuarioId, String ruc) {
        return apiKeyRepository.findVigentesByUsuarioIdAndRuc(usuarioId, ruc)
            .onItem().transformToUni(claves -> {
                Uni<Void> cadena = Uni.createFrom().voidItem();
                for (ApiKey clave : claves) {
                    cadena = cadena.chain(() -> revocarYAvisar(clave));
                }
                return cadena;
            });
    }

    private Uni<Void> revocarYAvisar(ApiKey apiKey) {
        Uni<Integer> marca = apiKey.isRevocada()
            ? Uni.createFrom().item(0)
            : apiKeyRepository.revocar(apiKey.getId(), apiKey.getUsuarioId());
        // Una key que ya venció no necesita aviso: ningún servicio la aceptaría de todos modos.
        if (!apiKey.getFechaExpiracion().isAfter(Instant.now())) {
            return marca.replaceWithVoid();
        }
        return marca.chain(() -> publicador.publicar(apiKey.getJti(), apiKey.getFechaExpiracion()));
    }
}
