package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ComprobanteEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.SecuencialComprobanteEntity;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.ComprobanteMapper;

@ApplicationScoped
public class ComprobanteRepositoryImpl implements ComprobanteRepository {

    private static final String CONSULTA_POR_NUMERO = "empresaRuc = ?1 and tipoComprobante = ?2 and establecimiento = ?3 "
        + "and puntoEmision = ?4 and secuencial = ?5";
    private static final String SUMA_NOTAS_CREDITO = "select sum(c.importeTotal) from ComprobanteEntity c "
        + "where c.empresaRuc = :ruc and c.tipoComprobante = :tipo and c.docModificadoTipo = :docTipo "
        + "and c.docModificadoNumero = :docNumero and c.estado in :estados";

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
    public Uni<Comprobante> buscarParaModificar(String ruc, TipoComprobante tipo, String establecimiento,
            String puntoEmision, String secuencial) {
        return repository.find(CONSULTA_POR_NUMERO, ruc, tipo.name(), establecimiento, puntoEmision, secuencial)
            .withLock(LockModeType.PESSIMISTIC_WRITE)
            .<ComprobanteEntity>firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    public Uni<BigDecimal> sumarNotasCreditoVigentes(String ruc, String docModificadoTipo, String docModificadoNumero) {
        List<String> estadosVigentes = Arrays.stream(EstadoComprobante.values())
            .filter(EstadoComprobante::cuentaComoEmitido).map(EstadoComprobante::name).collect(Collectors.toList());
        return Panache.getSession()
            .chain(sesion -> sesion.createQuery(SUMA_NOTAS_CREDITO, BigDecimal.class)
                .setParameter("ruc", ruc)
                .setParameter("tipo", TipoComprobante.NOTA_CREDITO.name())
                .setParameter("docTipo", docModificadoTipo)
                .setParameter("docNumero", docModificadoNumero)
                .setParameter("estados", estadosVigentes)
                .getSingleResult())
            .map(suma -> suma == null ? BigDecimal.ZERO : suma);
    }

    @Override
    public Uni<PageResult<Comprobante>> findPaged(int page, int limit, EstadoComprobante estado, String empresaRuc) {
        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (estado != null) {
            query.append(" and estado = :estado");
            params.put("estado", estado.name());
        }
        if (empresaRuc != null) {
            query.append(" and empresaRuc = :empresaRuc");
            params.put("empresaRuc", empresaRuc);
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
    public Uni<Long> obtenerSiguienteSecuencial(String ruc, TipoComprobante tipo, String establecimiento,
            String puntoEmision) {
        return secuencialRepository.update(
                "ultimoSecuencial = ultimoSecuencial + 1 "
                    + "where ruc = ?1 and tipoComprobante = ?2 and establecimiento = ?3 and puntoEmision = ?4",
                ruc, tipo.name(), establecimiento, puntoEmision)
            .onItem().transformToUni(filasActualizadas -> {
                if (filasActualizadas == 0) {
                    SecuencialComprobanteEntity nuevo = new SecuencialComprobanteEntity();
                    nuevo.id = UUID.randomUUID();
                    nuevo.ruc = ruc;
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
                        "ruc = ?1 and tipoComprobante = ?2 and establecimiento = ?3 and puntoEmision = ?4",
                        ruc, tipo.name(), establecimiento, puntoEmision)
                    .<SecuencialComprobanteEntity>firstResult()
                    .map(entity -> entity.ultimoSecuencial);
            });
    }
}
