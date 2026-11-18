package com.facturacion.identidad.application.service;

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

import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.exception.NotFoundException;
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
import com.facturacion.identidad.domain.port.HashDePassword;
import com.facturacion.identidad.domain.repository.UsuarioEmpresaRolRepository;
import com.facturacion.identidad.domain.repository.UsuarioRepository;
import com.facturacion.identidad.shared.error.IdentidadError;
import com.facturacion.identidad.shared.exception.EmailDuplicadoException;
import com.facturacion.identidad.shared.exception.RolInsuficienteException;

/**
 * Dueño del agregado Usuario y de su asociación con empresas (ruc). El hash y la verificación de contraseñas salen
 * por el puerto {@link HashDePassword}: aquí no hay código bloqueante ni hilos.
 */
@ApplicationScoped
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRolRepository empresaRolRepository;
    private final UsuarioResponseMapper mapper;
    private final UsuarioEmpresaRolResponseMapper empresaRolMapper;
    private final EmisorTokens jwtService;
    private final ApiKeyService apiKeyService;
    private final HashDePassword hashDePassword;

    @Inject
    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioEmpresaRolRepository empresaRolRepository,
            UsuarioResponseMapper mapper,
            UsuarioEmpresaRolResponseMapper empresaRolMapper, EmisorTokens jwtService, ApiKeyService apiKeyService,
            HashDePassword hashDePassword) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRolRepository = empresaRolRepository;
        this.mapper = mapper;
        this.empresaRolMapper = empresaRolMapper;
        this.jwtService = jwtService;
        this.apiKeyService = apiKeyService;
        this.hashDePassword = hashDePassword;
    }

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
                return hashDePassword.generar(request.getPassword())
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
                // Sin cuenta el puerto igual gasta el tiempo de una verificación real (no delata qué correos existen).
                return hashDePassword.verificar(request.getPassword(), usuario == null ? null : usuario.getPasswordHash())
                    .onItem().transformToUni(passwordCorrecta -> {
                        if (usuario == null || !usuario.isActivo() || !passwordCorrecta) {
                            return Uni.createFrom().<TokenResponse>failure(
                                new ErrorNegocioException(IdentidadError.CREDENCIALES_INVALIDAS));
                        }
                        String token = jwtService.generarTokenSesion(usuario.getId(), usuario.getEmail());
                        return Uni.createFrom().item(new TokenResponse(token));
                    });
            });
    }

    @WithSession
    public Uni<UsuarioResponse> obtenerPorId(UUID id) {
        return usuarioRepository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException(IdentidadError.USUARIO_NO_ENCONTRADO, id))
            .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<UsuarioResponse> actualizarPerfil(UUID usuarioId, @Valid ActualizarPerfilRequest request) {
        return usuarioRepository.findById(usuarioId)
            .onItem().ifNull().failWith(() -> new NotFoundException(IdentidadError.USUARIO_NO_ENCONTRADO, usuarioId))
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
            .onItem().ifNull().failWith(() -> new NotFoundException(IdentidadError.USUARIO_NO_ENCONTRADO, usuarioId))
            .onItem().transformToUni(usuario -> {
                return hashDePassword.verificar(request.getPasswordActual(), usuario.getPasswordHash())
                    .onItem().transformToUni(coincide -> {
                        if (!coincide) {
                            return Uni.createFrom().<Usuario>failure(
                                new ErrorNegocioException(IdentidadError.PASSWORD_ACTUAL_INCORRECTA));
                        }
                        return hashDePassword.generar(request.getPasswordNueva())
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
                () -> new NotFoundException(IdentidadError.USUARIO_SIN_REGISTRO_POR_CORREO, email))
            .onItem().transformToUni(usuario -> empresaRolRepository.findByUsuarioIdAndRuc(usuario.getId(), ruc)
                .onItem().transformToUni(existente -> existente != null
                    ? Uni.<UsuarioEmpresaRol>createFrom().failure(new ConflictoException(IdentidadError.USUARIO_YA_TIENE_ACCESO, email, ruc,
                        usuario.getId()))
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
            .onItem().ifNull().failWith(() -> new NotFoundException(IdentidadError.NO_ASOCIADO_AL_RUC, ruc))
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
                () -> new NotFoundException(IdentidadError.MIEMBRO_NO_ASOCIADO_AL_RUC, miembroId, ruc))
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
                () -> new NotFoundException(IdentidadError.MIEMBRO_NO_ASOCIADO_AL_RUC, miembroId, ruc))
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
                        new ConflictoException(IdentidadError.ULTIMO_ADMIN, ruc));
                }
                return Uni.createFrom().voidItem();
            });
    }
}
