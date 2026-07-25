package com.ecommerce.notification.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

import com.ecommerce.notification.domain.model.entity.Notification;
import com.ecommerce.notification.domain.model.vo.PageResult;
import com.ecommerce.notification.domain.repository.NotificationRepository;
import com.ecommerce.notification.infrastructure.persistence.entity.NotificationEntity;
import com.ecommerce.notification.infrastructure.persistence.mapper.NotificationMapper;

@ApplicationScoped
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationPanacheRepository repository;
    private final NotificationMapper mapper;

    @Inject
    public NotificationRepositoryImpl(NotificationPanacheRepository repository,
        NotificationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Notification> persist(Notification domain) {
        return repository.persist(mapper.toEntity(domain)).map(mapper::toDomain);
    }

    @Override
    public Uni<List<Notification>> findByOrderId(UUID orderId) {
        return repository.list("orderId", Sort.descending("createdAt"), orderId)
            .map(entities -> entities.stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public Uni<PageResult<Notification>> findPaged(int page, int limit) {
        var query = repository.findAll(Sort.descending("createdAt")).page(Page.of(page, limit));
        Uni<List<NotificationEntity>> itemsUni = query.list();
        Uni<Long> countUni = query.count();
        return Uni.combine().all().unis(itemsUni, countUni).asTuple()
            .map(tuple -> new PageResult<>(
                tuple.getItem1().stream().map(mapper::toDomain).collect(Collectors.toList()),
                tuple.getItem2()));
    }
}
