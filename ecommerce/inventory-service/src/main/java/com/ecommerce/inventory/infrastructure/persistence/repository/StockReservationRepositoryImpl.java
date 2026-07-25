package com.ecommerce.inventory.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.ecommerce.inventory.domain.model.entity.StockReservation;
import com.ecommerce.inventory.domain.model.enums.ReservationStatus;
import com.ecommerce.inventory.domain.repository.StockReservationRepository;
import com.ecommerce.inventory.infrastructure.persistence.mapper.StockReservationMapper;

@ApplicationScoped
public class StockReservationRepositoryImpl implements StockReservationRepository {

    private final StockReservationPanacheRepository repository;
    private final StockReservationMapper mapper;

    @Inject
    public StockReservationRepositoryImpl(StockReservationPanacheRepository repository,
        StockReservationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<StockReservation> persist(StockReservation domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<List<StockReservation>> findByOrderId(UUID orderId) {
        return repository.list("orderId", orderId)
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<List<StockReservation>> findByProductId(UUID productId) {
        return repository.list("productId", productId)
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<Void> releaseAllForOrder(UUID orderId) {
        return repository.update("status = ?1 where orderId = ?2 and status = ?3",
                ReservationStatus.RELEASED.name(), orderId, ReservationStatus.RESERVED.name())
            .replaceWithVoid();
    }
}
