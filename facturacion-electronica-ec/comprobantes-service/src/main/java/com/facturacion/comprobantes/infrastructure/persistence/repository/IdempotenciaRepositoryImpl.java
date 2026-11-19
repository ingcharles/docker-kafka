package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.vo.RegistroIdempotencia;
import com.facturacion.comprobantes.domain.repository.IdempotenciaRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.IdempotenciaEmisionEntity;

@ApplicationScoped
public class IdempotenciaRepositoryImpl implements IdempotenciaRepository {

    /**
     * {@code ON CONFLICT DO NOTHING} espera a que termine la transacción que tiene la misma clave: si confirmó no inserta
     * nada (devuelve 0 filas) y si se deshizo inserta esta.
     */
    private static final String SQL_RESERVAR = "insert into idempotencia_emision (id, ruc, clave, huella, creado_en) "
        + "values (?1, ?2, ?3, ?4, ?5) on conflict (ruc, clave) do nothing";
    private static final String CONSULTA_POR_CLAVE = "ruc = ?1 and clave = ?2";
    private static final String ACTUALIZAR_COMPROBANTE = "comprobanteId = ?1 where ruc = ?2 and clave = ?3";
    private static final String BORRAR_ANTERIORES = "creadoEn < ?1";

    private final IdempotenciaEmisionPanacheRepository repository;

    @Inject
    public IdempotenciaRepositoryImpl(IdempotenciaEmisionPanacheRepository repository) {
        this.repository = repository;
    }

    @Override
    public Uni<Boolean> reservar(String ruc, String clave, String huella) {
        return Panache.getSession()
            .chain(sesion -> sesion.createNativeQuery(SQL_RESERVAR)
                .setParameter(1, UUID.randomUUID())
                .setParameter(2, ruc)
                .setParameter(3, clave)
                .setParameter(4, huella)
                .setParameter(5, Instant.now())
                .executeUpdate())
            .map(filas -> filas > 0);
    }

    @Override
    public Uni<RegistroIdempotencia> buscar(String ruc, String clave) {
        return repository.find(CONSULTA_POR_CLAVE, ruc, clave)
            .<IdempotenciaEmisionEntity>firstResult()
            .map(entidad -> entidad == null ? null : new RegistroIdempotencia(entidad.huella, entidad.comprobanteId));
    }

    @Override
    public Uni<Void> asociarComprobante(String ruc, String clave, UUID comprobanteId) {
        // El comprobante se acaba de persistir pero su INSERT sale al hacer flush; sin él, la actualización (que apunta al
        // comprobante con una clave foránea) correría antes de que exista la fila.
        return Panache.flush()
            .chain(() -> repository.update(ACTUALIZAR_COMPROBANTE, comprobanteId, ruc, clave))
            .replaceWithVoid();
    }

    @Override
    public Uni<Long> purgarCreadasAntesDe(Instant limite) {
        return repository.delete(BORRAR_ANTERIORES, limite);
    }
}
