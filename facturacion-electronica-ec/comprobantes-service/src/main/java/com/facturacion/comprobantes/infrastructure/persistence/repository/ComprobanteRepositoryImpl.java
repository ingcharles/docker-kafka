package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.PageResult;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ComprobanteEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.SecuencialComprobanteEntity;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.ComprobanteMapper;

@ApplicationScoped
public class ComprobanteRepositoryImpl implements ComprobanteRepository {

    private final ComprobantePanacheRepository repository;
    private final SecuencialComprobantePanacheRepository secuencialRepository;
    private final ComprobanteMapper mapper;

    @Inject
    public ComprobanteRepositoryImpl(ComprobantePanacheRepository repository,
            SecuencialComprobantePanacheRepository secuencialRepository, ComprobanteMapper mapper) {
        this.repository = repository;
        this.secuencialRepository = secuencialRepository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Comprobante> persist(Comprobante domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<Comprobante> update(Comprobante domain) {
        return repository.<ComprobanteEntity>findById(domain.getId())
            .onItem().ifNotNull().transformToUni(entity -> {
                entity.estado = mapper.map(domain.getEstado());
                entity.xmlFirmado = domain.getXmlFirmado();
                entity.xmlAutorizado = domain.getXmlAutorizado();
                entity.numeroAutorizacion = domain.getNumeroAutorizacion();
                entity.fechaAutorizacion = domain.getFechaAutorizacion();
                entity.motivoRechazo = domain.getMotivoRechazo();
                entity.ridePdf = domain.getRidePdf();
                entity.actualizadoEn = Instant.now();
                return Uni.createFrom().item(mapper.toDomain(entity));
            });
    }

    @Override
    public Uni<Comprobante> findById(UUID id) {
        return repository.<ComprobanteEntity>findById(id)
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<Comprobante> findByClaveAcceso(String claveAcceso) {
        return repository.find("claveAcceso", claveAcceso)
            .<ComprobanteEntity>firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<PageResult<Comprobante>> findPaged(int page, int limit, EstadoComprobante estado, UUID emisorId) {
        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (estado != null) {
            query.append(" and estado = :estado");
            params.put("estado", estado.name());
        }
        if (emisorId != null) {
            query.append(" and emisorId = :emisorId");
            params.put("emisorId", emisorId);
        }

        var panacheQuery = repository.find(query.toString(), params).page(Page.of(page, limit));
        Uni<List<ComprobanteEntity>> itemsUni = panacheQuery.list();
        Uni<Long> countUni = panacheQuery.count();

        return Uni.combine().all().unis(itemsUni, countUni)
            .asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }

    @Override
    public Uni<Long> siguienteSecuencial(UUID emisorId, TipoComprobante tipo, String establecimiento,
            String puntoEmision) {
        return secuencialRepository.update(
                "ultimoSecuencial = ultimoSecuencial + 1 "
                    + "where emisorId = ?1 and tipoComprobante = ?2 and establecimiento = ?3 and puntoEmision = ?4",
                emisorId, tipo.name(), establecimiento, puntoEmision)
            .onItem().transformToUni(filasActualizadas -> {
                if (filasActualizadas == 0) {
                    SecuencialComprobanteEntity nuevo = new SecuencialComprobanteEntity();
                    nuevo.id = UUID.randomUUID();
                    nuevo.emisorId = emisorId;
                    nuevo.tipoComprobante = tipo.name();
                    nuevo.establecimiento = establecimiento;
                    nuevo.puntoEmision = puntoEmision;
                    nuevo.ultimoSecuencial = 1;
                    return secuencialRepository.persist(nuevo).map(entity -> entity.ultimoSecuencial);
                }
                // Fila ya existía y se acaba de incrementar vía UPDATE masivo — como
                // nunca se cargó antes en esta transacción, esta lectura es segura
                // (no hay caché de primer nivel obsoleto que arrastrar, a diferencia
                // del caso documentado en inventario-service/ItemService).
                return secuencialRepository.find(
                        "emisorId = ?1 and tipoComprobante = ?2 and establecimiento = ?3 and puntoEmision = ?4",
                        emisorId, tipo.name(), establecimiento, puntoEmision)
                    .<SecuencialComprobanteEntity>firstResult()
                    .map(entity -> entity.ultimoSecuencial);
            });
    }
}
