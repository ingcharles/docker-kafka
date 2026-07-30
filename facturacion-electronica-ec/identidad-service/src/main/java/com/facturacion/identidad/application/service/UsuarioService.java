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

import com.facturacion.identidad.application.dto.ActualizarPerfilRequest;
import com.facturacion.identidad.application.dto.AsociarEmpresaRequest;
import com.facturacion.identidad.application.dto.CambiarPasswordRequest;
import com.facturacion.identidad.application.dto.CrearUsuarioRequest;
import com.facturacion.identidad.application.dto.EmpresaRolResponse;
import com.facturacion.identidad.application.dto.LoginRequest;
import com.facturacion.identidad.application.dto.MiembroEmpresaResponse;
import com.facturacion.identidad.application.dto.TokenResponse;
import com.facturacion.identidad.application.dto.UsuarioResponse;
import com.facturacion.identidad.domain.model.entity.EmpresaLocal;
import com.facturacion.identidad.domain.model.entity.Usuario;
import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;
import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.identidad.domain.repository.EmpresaLocalRepository;
import com.facturacion.identidad.domain.repository.UsuarioEmpresaRolRepository;
import com.facturacion.identidad.domain.repository.UsuarioRepository;
import com.facturacion.identidad.infrastructure.crypto.JwtService;
import com.facturacion.identidad.infrastructure.persistence.mapper.UsuarioEmpresaRolMapper;
import com.facturacion.identidad.infrastructure.persistence.mapper.UsuarioMapper;
import com.facturacion.identidad.shared.exception.ConflictoException;
import com.facturacion.identidad.shared.exception.CredencialesInvalidasException;
import com.facturacion.identidad.shared.exception.EmailDuplicadoException;
import com.facturacion.identidad.shared.exception.NotFoundException;
import com.facturacion.identidad.shared.exception.RolInsuficienteException;

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
    private final EmpresaLocalRepository empresaLocalRepository;
    private final UsuarioMapper mapper;
    private final UsuarioEmpresaRolMapper empresaRolMapper;
    private final JwtService jwtService;

    @Inject
    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioEmpresaRolRepository empresaRolRepository,
            EmpresaLocalRepository empresaLocalRepository, UsuarioMapper mapper,
            UsuarioEmpresaRolMapper empresaRolMapper, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRolRepository = empresaRolRepository;
        this.empresaLocalRepository = empresaLocalRepository;
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

    @WithTransaction
    public Uni<UsuarioResponse> actualizarPerfil(UUID usuarioId, @Valid ActualizarPerfilRequest request) {
        return usuarioRepository.findById(usuarioId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Usuario no encontrado: " + usuarioId))
            .onItem().transformToUni(usuario -> {
                usuario.setNombres(request.getNombres());
                usuario.setApellidos(request.getApellidos());
                usuario.setActualizadoEn(Instant.now());
                return usuarioRepository.update(usuario);
            })
            .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> cambiarPassword(UUID usuarioId, @Valid CambiarPasswordRequest request) {
        return usuarioRepository.findById(usuarioId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Usuario no encontrado: " + usuarioId))
            .onItem().transformToUni(usuario -> {
                if (!BcryptUtil.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
                    return Uni.createFrom().failure(
                        new CredencialesInvalidasException("La contraseña actual no es correcta"));
                }
                usuario.setPasswordHash(BcryptUtil.bcryptHash(request.getPasswordNueva()));
                usuario.setActualizadoEn(Instant.now());
                return usuarioRepository.update(usuario);
            })
            .replaceWithVoid();
    }

    /**
     * Sin flujo de invitación/aprobación — cualquier usuario autenticado
     * puede pedir asociarse a cualquier ruc con cualquier rol, siempre que
     * ese ruc corresponda a un Empresa real y activo (ver {@link EmpresaLocal}
     * — copia local alimentada por {@code facturacion.empresa.eventos.v1}).
     * No verifica *titularidad* real sobre la empresa, solo que exista.
     * Si ya existía la asociación, actualiza el rol.
     */
    @WithTransaction
    public Uni<EmpresaRolResponse> asociarEmpresa(UUID usuarioId, @Valid AsociarEmpresaRequest request) {
        Rol rol = Rol.valueOf(request.getRol());
        return empresaLocalRepository.findByRuc(request.getRuc())
            .onItem().transformToUni(empresa -> {
                if (empresa == null || !empresa.isActivo()) {
                    return Uni.createFrom().failure(
                        new NotFoundException("No existe una empresa activa con el ruc " + request.getRuc()));
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

    /**
     * El propio usuario abandona un ruc al que se había autoasociado — misma
     * protección de {@link #asegurarNoUltimoAdmin} que la revocación hecha
     * por un ADMIN ({@link #revocarMiembro}): no puede irse si es el único
     * ADMIN que le queda a esa empresa.
     */
    @WithTransaction
    public Uni<Void> salirDeEmpresa(UUID usuarioId, String ruc) {
        return empresaRolRepository.findByUsuarioIdAndRuc(usuarioId, ruc)
            .onItem().ifNull().failWith(() -> new NotFoundException("No estás asociado al ruc " + ruc))
            .onItem().transformToUni(existente -> asegurarNoUltimoAdmin(ruc, existente, null)
                .chain(() -> empresaRolRepository.delete(existente)));
    }

    /**
     * Gestión de miembros de una empresa — solo un ADMIN de ese ruc puede
     * ver, cambiar el rol de, o revocar a otros usuarios asociados a él
     * (cierra el hueco de "cualquiera se autoasocia con cualquier rol y
     * nadie más puede gestionarlo" documentado en {@link #asociarEmpresa}).
     */
    @WithSession
    public Uni<List<MiembroEmpresaResponse>> listarMiembros(UUID solicitanteId, String ruc) {
        return exigirAdmin(solicitanteId, ruc)
            .onItem().transformToUni(ignorado -> empresaRolRepository.findByRuc(ruc))
            .onItem().transformToUni(miembros -> {
                List<UUID> usuarioIds = miembros.stream().map(UsuarioEmpresaRol::getUsuarioId)
                    .collect(Collectors.toList());
                return usuarioRepository.findByIds(usuarioIds).map(usuarios -> {
                    java.util.Map<UUID, Usuario> porId = usuarios.stream()
                        .collect(Collectors.toMap(Usuario::getId, u -> u));
                    return miembros.stream()
                        .map(m -> {
                            Usuario u = porId.get(m.getUsuarioId());
                            return new MiembroEmpresaResponse(m.getUsuarioId(), u.getEmail(), u.getNombres(),
                                u.getApellidos(), m.getRol().name(), m.getCreadoEn());
                        })
                        .collect(Collectors.toList());
                });
            });
    }

    @WithTransaction
    public Uni<EmpresaRolResponse> cambiarRolMiembro(UUID solicitanteId, String ruc, UUID miembroId,
            String nuevoRolTexto) {
        Rol nuevoRol = Rol.valueOf(nuevoRolTexto);
        return exigirAdmin(solicitanteId, ruc)
            .onItem().transformToUni(ignorado -> empresaRolRepository.findByUsuarioIdAndRuc(miembroId, ruc))
            .onItem().ifNull().failWith(
                () -> new NotFoundException("El usuario " + miembroId + " no está asociado al ruc " + ruc))
            .onItem().transformToUni(existente -> asegurarNoUltimoAdmin(ruc, existente, nuevoRol)
                .chain(() -> {
                    existente.setRol(nuevoRol);
                    return empresaRolRepository.update(existente);
                }))
            .map(empresaRolMapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> revocarMiembro(UUID solicitanteId, String ruc, UUID miembroId) {
        return exigirAdmin(solicitanteId, ruc)
            .onItem().transformToUni(ignorado -> empresaRolRepository.findByUsuarioIdAndRuc(miembroId, ruc))
            .onItem().ifNull().failWith(
                () -> new NotFoundException("El usuario " + miembroId + " no está asociado al ruc " + ruc))
            .onItem().transformToUni(existente -> asegurarNoUltimoAdmin(ruc, existente, null)
                .chain(() -> empresaRolRepository.delete(existente)));
    }

    /** 403 si el usuario autenticado no es ADMIN de ese ruc — mismo criterio que las demás excepciones de este servicio. */
    private Uni<UsuarioEmpresaRol> exigirAdmin(UUID usuarioId, String ruc) {
        return empresaRolRepository.findByUsuarioIdAndRuc(usuarioId, ruc)
            .onItem().transformToUni(asociacion -> {
                if (asociacion == null || asociacion.getRol() != Rol.ADMIN) {
                    return Uni.createFrom().failure(new RolInsuficienteException(ruc));
                }
                return Uni.createFrom().item(asociacion);
            });
    }

    /**
     * 409 si la operación (cambiar de rol a algo distinto de ADMIN, o
     * revocar del todo — {@code nuevoRol == null}) dejaría a la empresa sin
     * ningún ADMIN. No importa quién la pida, ni siquiera el propio ADMIN
     * afectado puede autodegradarse si es el último.
     */
    private Uni<Void> asegurarNoUltimoAdmin(String ruc, UsuarioEmpresaRol existente, Rol nuevoRol) {
        if (existente.getRol() != Rol.ADMIN || nuevoRol == Rol.ADMIN) {
            return Uni.createFrom().voidItem();
        }
        return empresaRolRepository.contarPorRucYRol(ruc, Rol.ADMIN)
            .onItem().transformToUni(cantidad -> {
                if (cantidad <= 1) {
                    return Uni.createFrom().failure(
                        new ConflictoException("No se puede dejar al ruc " + ruc + " sin ningún ADMIN"));
                }
                return Uni.createFrom().voidItem();
            });
    }
}
