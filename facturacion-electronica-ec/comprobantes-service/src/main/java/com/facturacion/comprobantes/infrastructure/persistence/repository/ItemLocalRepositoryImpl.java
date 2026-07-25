package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.ItemLocal;
import com.facturacion.comprobantes.domain.repository.ItemLocalRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ItemLocalEntity;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.ItemLocalMapper;

@ApplicationScoped
public class ItemLocalRepositoryImpl implements ItemLocalRepository {

    private final ItemLocalPanacheRepository repository;
    private final ItemLocalMapper mapper;

    @Inject
    public ItemLocalRepositoryImpl(ItemLocalPanacheRepository repository, ItemLocalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<ItemLocal> upsert(ItemLocal domain) {
        return repository.<ItemLocalEntity>findById(domain.getId())
            .onItem().transformToUni(existente -> {
                if (existente == null) {
                    return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
                }
                existente.ruc = domain.getRuc();
                existente.codigoPrincipal = domain.getCodigoPrincipal();
                existente.descripcion = domain.getDescripcion();
                existente.codigoImpuesto = domain.getCodigoImpuesto();
                existente.codigoPorcentaje = domain.getCodigoPorcentaje();
                existente.tarifaIva = domain.getTarifaIva();
                existente.tieneIce = domain.isTieneIce();
                existente.codigoPorcentajeIce = domain.getCodigoPorcentajeIce();
                existente.tarifaIce = domain.getTarifaIce();
                existente.activo = domain.isActivo();
                existente.actualizadoEn = domain.getActualizadoEn();
                return Uni.createFrom().item(mapper.toDomain(existente));
            });
    }

    @Override
    public Uni<Map<UUID, ItemLocal>> findByIdsAndRuc(Set<UUID> ids, String ruc) {
        if (ids.isEmpty()) {
            return Uni.createFrom().item(Map.of());
        }
        return repository.list("id in ?1 and ruc = ?2", ids, ruc)
            .map(entities -> entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(ItemLocal::getId, item -> item)));
    }
}
