package com.facturacion.inventario.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.inventario.domain.model.entity.ReservaStock;
import com.facturacion.inventario.domain.model.enums.EstadoReserva;
import com.facturacion.inventario.domain.repository.ReservaStockRepository;
import com.facturacion.inventario.infrastructure.persistence.entity.ReservaStockEntity;
import com.facturacion.inventario.infrastructure.persistence.mapper.ReservaStockMapper;

@ApplicationScoped
public class ReservaStockRepositoryImpl implements ReservaStockRepository {

    private final ReservaStockPanacheRepository repository;
    private final ReservaStockMapper mapper;

    @Inject
    public ReservaStockRepositoryImpl(ReservaStockPanacheRepository repository, ReservaStockMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<ReservaStock> persist(ReservaStock domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<List<ReservaStock>> findByComprobanteIdAndEstado(UUID comprobanteId, EstadoReserva estado) {
        return repository.find("comprobanteId = ?1 and estado = ?2", comprobanteId, estado.name())
            .<ReservaStockEntity>list()
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<Integer> actualizarEstado(UUID id, EstadoReserva nuevoEstado, EstadoReserva estadoEsperado) {
        return repository.update("estado = ?1 where id = ?2 and estado = ?3",
            nuevoEstado.name(), id, estadoEsperado.name());
    }
}
