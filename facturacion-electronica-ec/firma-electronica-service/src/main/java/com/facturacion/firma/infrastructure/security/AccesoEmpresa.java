package com.facturacion.firma.infrastructure.security;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.domain.repository.EstablecimientoRepository;

/**
 * Autorización por empresa en la frontera REST: una api-key solo opera sobre su propia empresa (y sus
 * establecimientos, puntos de emisión, configuración y certificado). Una empresa de otra api-key responde igual que
 * una inexistente (404), para no revelar que existe.
 * <p>
 * Es crítico en este servicio: sin esta comprobación, la api-key de cualquier empresa podría subir o revocar el
 * certificado de firma de otra.
 */
@ApplicationScoped
public class AccesoEmpresa {

    private final TokenPrincipal tokenPrincipal;
    private final EmpresaRepository empresaRepository;
    private final EstablecimientoRepository establecimientoRepository;

    @Inject
    public AccesoEmpresa(TokenPrincipal tokenPrincipal, EmpresaRepository empresaRepository,
            EstablecimientoRepository establecimientoRepository) {
        this.tokenPrincipal = tokenPrincipal;
        this.empresaRepository = empresaRepository;
        this.establecimientoRepository = establecimientoRepository;
    }

    /** Falla con 404 si la empresa no existe o no es la de la api-key. */
    @WithSession
    public Uni<Void> exigirEmpresa(UUID empresaId) {
        return empresaRepository.findById(empresaId)
            .onItem().transformToUni(empresa -> empresa != null && tokenPrincipal.esDeLaEmpresa(empresa.getRuc())
                ? Uni.createFrom().voidItem()
                : Uni.createFrom().failure(new NotFoundException("Empresa no encontrada: " + empresaId)));
    }

    /** Como {@link #exigirEmpresa}, pero para operaciones que modifican la empresa: además exige rol ADMIN. */
    public Uni<Void> exigirEmpresaAdmin(UUID empresaId) {
        tokenPrincipal.exigirAdmin();
        return exigirEmpresa(empresaId);
    }

    /** Como {@link #exigirEstablecimiento}, pero para operaciones que modifican: además exige rol ADMIN. */
    public Uni<Void> exigirEstablecimientoAdmin(UUID empresaId, UUID establecimientoId) {
        tokenPrincipal.exigirAdmin();
        return exigirEstablecimiento(empresaId, establecimientoId);
    }

    /** Además de la empresa, exige que el establecimiento sea de esa empresa (la ruta trae ambos ids). */
    @WithSession
    public Uni<Void> exigirEstablecimiento(UUID empresaId, UUID establecimientoId) {
        return exigirEmpresa(empresaId)
            .chain(() -> establecimientoRepository.findById(establecimientoId))
            .onItem().transformToUni(establecimiento ->
                establecimiento != null && empresaId.equals(establecimiento.getEmpresaId())
                    ? Uni.createFrom().voidItem()
                    : Uni.createFrom().failure(
                        new NotFoundException("Establecimiento no encontrado: " + establecimientoId)));
    }
}
