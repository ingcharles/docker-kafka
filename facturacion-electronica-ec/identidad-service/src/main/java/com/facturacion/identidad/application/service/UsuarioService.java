package com.facturacion.identidad.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.application.dto.AsociarEmpresaRequest;
import com.facturacion.identidad.application.dto.CrearUsuarioRequest;
import com.facturacion.identidad.application.dto.EmpresaRolResponse;
import com.facturacion.identidad.application.dto.LoginRequest;
import com.facturacion.identidad.application.dto.TokenResponse;
import com.facturacion.identidad.application.dto.UsuarioResponse;
import com.facturacion.identidad.domain.model.entity.EmisorLocal;
import com.facturacion.identidad.domain.model.entity.Usuario;
import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;
import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.identidad.domain.repository.EmisorLocalRepository;
import com.facturacion.identidad.domain.repository.UsuarioEmpresaRolRepository;
import com.facturacion.identidad.domain.repository.UsuarioRepository;
import com.facturacion.identidad.infrastructure.crypto.JwtService;
import com.facturacion.identidad.infrastructure.persistence.mapper.UsuarioEmpresaRolMapper;
import com.facturacion.identidad.infrastructure.persistence.mapper.UsuarioMapper;
import com.facturacion.identidad.shared.exception.CredencialesInvalidasException;
import com.facturacion.identidad.shared.exception.EmailDuplicadoException;
import com.facturacion.identidad.shared.exception.NotFoundException;

/**
 * Dueño del agregado Usuario y de su asociación con empresas (ruc). El
 * hasheo de contraseña usa {@link BcryptUtil} (utilidad estándar de
 * Quarkus) — no hay motivo para reinventar bcrypt, a diferencia de la firma
 * XAdES-BES en firma-electronica-service, donde sí no existe una librería
 * que cubra el perfil exacto que exige el SRI.
 */
@ApplicationScoped
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRolRepository empresaRolRepository;
    private final EmisorLocalRepository emisorLocalRepository;
    private final UsuarioMapper mapper;
    private final UsuarioEmpresaRolMapper empresaRolMapper;
    private final JwtService jwtService;

    @Inject
    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioEmpresaRolRepository empresaRolRepository,
            EmisorLocalRepository emisorLocalRepository, UsuarioMapper mapper,
            UsuarioEmpresaRolMapper empresaRolMapper, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRolRepository = empresaRolRepository;
        this.emisorLocalRepository = emisorLocalRepository;
        this.mapper = mapper;
        this.empresaRolMapper = empresaRolMapper;
        this.jwtService = jwtService;
    }

    @WithTransaction
    public Uni<UsuarioResponse> crear(@Valid CrearUsuarioRequest request) {
        return usuarioRepository.existsByEmail(request.getEmail())
            .onItem().transformToUni(existe -> {
                if (Boolean.TRUE.equals(existe)) {
                    return Uni.createFrom().failure(new EmailDuplicadoException(request.getEmail()));
                }
                Instant ahora = Instant.now();
                Usuario usuario = new Usuario();
                usuario.setId(UUID.randomUUID());
                usuario.setEmail(request.getEmail());
                usuario.setPasswordHash(BcryptUtil.bcryptHash(request.getPassword()));
                usuario.setNombres(request.getNombres());
                usuario.setApellidos(request.getApellidos());
                usuario.setActivo(true);
                usuario.setCreadoEn(ahora);
                usuario.setActualizadoEn(ahora);
                return usuarioRepository.persist(usuario).map(mapper::toResponse);
            });
    }

    @WithSession
    public Uni<TokenResponse> login(@Valid LoginRequest request) {
        return usuarioRepository.findByEmail(request.getEmail())
            .onItem().transformToUni(usuario -> {
                if (usuario == null || !usuario.isActivo()
                        || !BcryptUtil.matches(request.getPassword(), usuario.getPasswordHash())) {
                    return Uni.createFrom().failure(
                        new CredencialesInvalidasException("Email o contraseña incorrectos"));
                }
                String token = jwtService.generarTokenSesion(usuario.getId(), usuario.getEmail());
                return Uni.createFrom().item(new TokenResponse(token));
            });
    }

    @WithSession
    public Uni<UsuarioResponse> obtenerPorId(UUID id) {
        return usuarioRepository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Usuario no encontrado: " + id))
            .map(mapper::toResponse);
    }

    /**
     * Sin flujo de invitación/aprobación — cualquier usuario autenticado
     * puede pedir asociarse a cualquier ruc con cualquier rol, siempre que
     * ese ruc corresponda a un Emisor real y activo (ver {@link EmisorLocal}
     * — copia local alimentada por {@code facturacion.emisor.eventos.v1}).
     * No verifica *titularidad* real sobre la empresa, solo que exista.
     * Si ya existía la asociación, actualiza el rol.
     */
    @WithTransaction
    public Uni<EmpresaRolResponse> asociarEmpresa(UUID usuarioId, @Valid AsociarEmpresaRequest request) {
        Rol rol = Rol.valueOf(request.getRol());
        return emisorLocalRepository.findByRuc(request.getRuc())
            .onItem().transformToUni(emisor -> {
                if (emisor == null || !emisor.isActivo()) {
                    return Uni.createFrom().failure(
                        new NotFoundException("No existe un emisor activo con el ruc " + request.getRuc()));
                }
                return empresaRolRepository.findByUsuarioIdAndRuc(usuarioId, request.getRuc());
            })
            .onItem().transformToUni(existente -> {
                if (existente != null) {
                    existente.setRol(rol);
                    return empresaRolRepository.update(existente);
                }
                UsuarioEmpresaRol nueva = new UsuarioEmpresaRol();
                nueva.setId(UUID.randomUUID());
                nueva.setUsuarioId(usuarioId);
                nueva.setRuc(request.getRuc());
                nueva.setRol(rol);
                nueva.setCreadoEn(Instant.now());
                return empresaRolRepository.persist(nueva);
            })
            .map(empresaRolMapper::toResponse);
    }

    @WithSession
    public Uni<List<EmpresaRolResponse>> listarEmpresas(UUID usuarioId) {
        return empresaRolRepository.findByUsuarioId(usuarioId)
            .map(lista -> lista.stream().map(empresaRolMapper::toResponse).collect(Collectors.toList()));
    }
}
