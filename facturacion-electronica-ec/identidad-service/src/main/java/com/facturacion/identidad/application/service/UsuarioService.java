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

import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.commons.reactive.TrabajoBloqueante;
import com.facturacion.identidad.application.dto.ActualizarPerfilRequest;
import com.facturacion.identidad.application.dto.CambiarPasswordRequest;
import com.facturacion.identidad.application.dto.CrearUsuarioRequest;
import com.facturacion.identidad.application.dto.EmpresaRolResponse;
import com.facturacion.identidad.application.dto.LoginRequest;
import com.facturacion.identidad.application.dto.MiembroEmpresaResponse;
import com.facturacion.identidad.application.dto.TokenResponse;
import com.facturacion.identidad.application.dto.UsuarioResponse;
import com.facturacion.identidad.application.mapper.UsuarioEmpresaRolResponseMapper;
import com.facturacion.identidad.application.mapper.UsuarioResponseMapper;
import com.facturacion.identidad.domain.model.entity.Usuario;
import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;
import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.identidad.domain.port.EmisorTokens;
import com.facturacion.identidad.domain.repository.UsuarioEmpresaRolRepository;
import com.facturacion.identidad.domain.repository.UsuarioRepository;
import com.facturacion.identidad.shared.exception.CredencialesInvalidasException;
import com.facturacion.identidad.shared.exception.EmailDuplicadoException;
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
    private final UsuarioResponseMapper mapper;
    private final UsuarioEmpresaRolResponseMapper empresaRolMapper;
    private final EmisorTokens jwtService;
    private final ApiKeyService apiKeyService;

    @Inject
    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioEmpresaRolRepository empresaRolRepository,
            UsuarioResponseMapper mapper,
            UsuarioEmpresaRolResponseMapper empresaRolMapper, EmisorTokens jwtService, ApiKeyService apiKeyService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRolRepository = empresaRolRepository;
        this.mapper = mapper;
        this.empresaRolMapper = empresaRolMapper;
        this.jwtService = jwtService;
        this.apiKeyService = apiKeyService;
    }

    /** Hash de una contraseña que nadie conoce: sirve para gastar el mismo tiempo cuando el correo no existe. */
    private static final String HASH_DESCARTABLE = BcryptUtil.bcryptHash("contraseña-descartable-sin-cuenta");

    /** El correo se guarda y se busca sin espacios y en minúsculas: "Ana@X.com" y "ana@x.com" son la misma cuenta. */
    static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @WithTransaction
    public Uni<UsuarioResponse> crear(@Valid CrearUsuarioRequest request) {
        String email = normalizarEmail(request.getEmail());
        return usuarioRepository.existsByEmail(email)
            .onItem().transformToUni(existe -> {
                if (Boolean.TRUE.equals(existe)) {
                    return Uni.createFrom().failure(new EmailDuplicadoException(email));
                }
                // bcrypt es CPU-bound (~100 ms): fuera del event loop, y de vuelta al contexto antes de tocar la BD.
                return TrabajoBloqueante.ejecutar(() -> BcryptUtil.bcryptHash(request.getPassword()))
                    .onItem().transformToUni(hash -> {
                        Instant ahora = Instant.now();
                        Usuario usuario = new Usuario();
                        usuario.setId(UUID.randomUUID());
                        usuario.setEmail(email);
                        usuario.setPasswordHash(hash);
                        usuario.setNombres(request.getNombres());
                        usuario.setApellidos(request.getApellidos());
                        usuario.setActivo(true);
                        usuario.setCreadoEn(ahora);
                        usuario.setActualizadoEn(ahora);
                        return usuarioRepository.persist(usuario).map(mapper::toResponse);
                    });
            });
    }

    @WithSession
    public Uni<TokenResponse> iniciarSesion(@Valid LoginRequest request) {
        return usuarioRepository.findByEmail(normalizarEmail(request.getEmail()))
            .onItem().transformToUni(usuario -> {
                // Con un correo que no existe también se calcula un bcrypt (contra un hash descartable): si no, la
                // respuesta tardaría menos y delataría qué correos tienen cuenta.
                String hash = usuario == null ? HASH_DESCARTABLE : usuario.getPasswordHash();
                return TrabajoBloqueante.ejecutar(() -> BcryptUtil.matches(request.getPassword(), hash))
                    .onItem().transformToUni(passwordCorrecta -> {
                        if (usuario == null || !usuario.isActivo() || !passwordCorrecta) {
                            return Uni.createFrom().<TokenResponse>failure(
                                new CredencialesInvalidasException("Email o contraseña incorrectos"));
                        }
                        String token = jwtService.generarTokenSesion(usuario.getId(), usuario.getEmail());
                        return Uni.createFrom().item(new TokenResponse(token));
                    });
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
                return TrabajoBloqueante.ejecutar(
                        () -> BcryptUtil.matches(request.getPasswordActual(), usuario.getPasswordHash()))
                    .onItem().transformToUni(coincide -> {
                        if (!coincide) {
                            return Uni.createFrom().<Usuario>failure(
                                new CredencialesInvalidasException("La contraseña actual no es correcta"));
                        }
                        return TrabajoBloqueante.ejecutar(() -> BcryptUtil.bcryptHash(request.getPasswordNueva()))
                            .onItem().transformToUni(nuevoHash -> {
                                usuario.setPasswordHash(nuevoHash);
                                usuario.setActualizadoEn(Instant.now());
                                return usuarioRepository.update(usuario);
                            });
                    });
            })
            .replaceWithVoid();
    }

    private static UsuarioEmpresaRol crearAsociacion(UUID usuarioId, String ruc, Rol rol) {
        UsuarioEmpresaRol nueva = new UsuarioEmpresaRol();
        nueva.setId(UUID.randomUUID());
        nueva.setUsuarioId(usuarioId);
        nueva.setRuc(ruc);
        nueva.setRol(rol);
        nueva.setCreadoEn(Instant.now());
        return nueva;
    }

    /**
     * Un ADMIN del ruc da acceso a un usuario ya registrado (por su correo). Es la forma de sumar gente a una
     * empresa que ya tiene dueño: es la única vía de acceso además del alta de la empresa (el creador queda ADMIN).
     */
    @WithTransaction
    public Uni<EmpresaRolResponse> agregarMiembro(UUID solicitanteId, String ruc, String email, String rolTexto) {
        Rol rol = Rol.valueOf(rolTexto);
        return exigirAdmin(solicitanteId, ruc)
            .onItem().transformToUni(ignorado -> usuarioRepository.findByEmail(normalizarEmail(email)))
            .onItem().ifNull().failWith(
                () -> new NotFoundException("No hay ningún usuario registrado con el correo " + email))
            .onItem().transformToUni(usuario -> empresaRolRepository.findByUsuarioIdAndRuc(usuario.getId(), ruc)
                .onItem().transformToUni(existente -> existente != null
                    ? Uni.<UsuarioEmpresaRol>createFrom().failure(new ConflictoException("El usuario " + email
                        + " ya tiene acceso al ruc " + ruc + " (para cambiarle el rol: PATCH /empresas/" + ruc
                        + "/usuarios/" + usuario.getId() + ")"))
                    : empresaRolRepository.persist(crearAsociacion(usuario.getId(), ruc, rol))))
            .map(empresaRolMapper::toResponse);
    }

    @WithSession
    public Uni<List<EmpresaRolResponse>> listarEmpresas(UUID usuarioId) {
        return empresaRolRepository.findByUsuarioId(usuarioId)
            .map(lista -> lista.stream().map(empresaRolMapper::toResponse).collect(Collectors.toList()));
    }

    /**
     * El propio usuario abandona un ruc al que tiene acceso — misma
     * protección de {@link #asegurarNoUltimoAdmin} que la revocación hecha
     * por un ADMIN ({@link #revocarMiembro}): no puede irse si es el único
     * ADMIN que le queda a esa empresa.
     */
    @WithTransaction
    public Uni<Void> salirDeEmpresa(UUID usuarioId, String ruc) {
        return empresaRolRepository.findByUsuarioIdAndRuc(usuarioId, ruc)
            .onItem().ifNull().failWith(() -> new NotFoundException("No estás asociado al ruc " + ruc))
            .onItem().transformToUni(existente -> asegurarNoUltimoAdmin(ruc, existente, null)
                .chain(() -> empresaRolRepository.delete(existente))
                .chain(() -> apiKeyService.revocarDeUsuarioEnRuc(usuarioId, ruc)));
    }

    /**
     * Gestión de miembros de una empresa — solo un ADMIN de ese ruc puede
     * ver, cambiar el rol de, o revocar a otros usuarios asociados a él
     * (no hay autoasociación: el acceso lo da el creador de la empresa o un ADMIN, ver {@link #agregarMiembro}).
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
                    // Las api-keys llevan el rol de cuando se emitieron: si el rol cambia hay que invalidarlas,
                    // para que quien baja de ADMIN a LECTURA no conserve el acceso anterior.
                    boolean cambia = existente.getRol() != nuevoRol;
                    existente.setRol(nuevoRol);
                    return empresaRolRepository.update(existente)
                        .call(() -> cambia ? apiKeyService.revocarDeUsuarioEnRuc(miembroId, ruc)
                            : Uni.createFrom().voidItem());
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
                .chain(() -> empresaRolRepository.delete(existente))
                .chain(() -> apiKeyService.revocarDeUsuarioEnRuc(miembroId, ruc)));
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
