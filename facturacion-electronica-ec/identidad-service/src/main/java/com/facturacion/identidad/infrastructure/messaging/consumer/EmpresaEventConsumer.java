package com.facturacion.identidad.infrastructure.messaging.consumer;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.identidad.application.event.EmpresaEventMessage;
import com.facturacion.identidad.domain.model.entity.EmpresaLocal;
import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;
import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.identidad.domain.repository.EmpresaLocalRepository;
import com.facturacion.identidad.domain.repository.UsuarioEmpresaRolRepository;

/**
 * Mantiene al día la vista materializada {@code empresa_local} — sin inbox
 * de idempotencia porque un upsert por id ya es naturalmente idempotente
 * (mismo criterio que {@code ItemEventConsumer} en comprobantes-service).
 * <p>
 * Además, si el evento es {@code EmpresaRegistrada} y trae
 * {@code usuarioCreadorId} (empresa creada vía {@code PermiteSesion} en
 * firma-electronica-service — ver su javadoc), otorga automáticamente el rol
 * {@code ADMIN} a ese usuario para ese ruc — sin esto, una empresa recién
 * creada por sesión (sin api-key todavía, porque no puede haber una para un
 * ruc que no existía) quedaría sin ningún usuario asociado y sin forma de
 * conseguir una: el ciclo cerrado que este mecanismo rompe.
 */
@ApplicationScoped
public class EmpresaEventConsumer {

    private static final String EVENT_TYPE_EMPRESA_REGISTRADA = "EmpresaRegistrada";

    private final EmpresaLocalRepository repository;
    private final UsuarioEmpresaRolRepository empresaRolRepository;

    @Inject
    public EmpresaEventConsumer(EmpresaLocalRepository repository,
            UsuarioEmpresaRolRepository empresaRolRepository) {
        this.repository = repository;
        this.empresaRolRepository = empresaRolRepository;
    }

    @Incoming("empresa-events-in")
    @WithTransaction
    public Uni<Void> onEvent(EmpresaEventMessage evento) {
        EmpresaLocal empresa = new EmpresaLocal();
        empresa.setId(evento.empresaId());
        empresa.setRuc(evento.ruc());
        empresa.setRazonSocial(evento.razonSocial());
        empresa.setActivo(evento.activo());
        empresa.setActualizadoEn(evento.occurredAt());
        return repository.upsert(empresa)
            .chain(guardada -> otorgarAdminSiAplica(evento));
    }

    private Uni<Void> otorgarAdminSiAplica(EmpresaEventMessage evento) {
        if (!EVENT_TYPE_EMPRESA_REGISTRADA.equals(evento.tipoEvento()) || evento.usuarioCreadorId() == null) {
            return Uni.createFrom().voidItem();
        }
        UUID usuarioId = evento.usuarioCreadorId();
        return empresaRolRepository.findByUsuarioIdAndRuc(usuarioId, evento.ruc())
            .onItem().transformToUni(existente -> {
                // Ya asociado (replay del evento, o ya se había asociado a mano) — no lo pisa.
                if (existente != null) {
                    return Uni.createFrom().voidItem();
                }
                UsuarioEmpresaRol nueva = new UsuarioEmpresaRol();
                nueva.setId(UUID.randomUUID());
                nueva.setUsuarioId(usuarioId);
                nueva.setRuc(evento.ruc());
                nueva.setRol(Rol.ADMIN);
                nueva.setCreadoEn(Instant.now());
                return empresaRolRepository.persist(nueva).replaceWithVoid();
            });
    }
}
