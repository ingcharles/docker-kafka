package com.ecommerce.notification.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import com.ecommerce.notification.application.dto.NotificationResponse;
import com.ecommerce.notification.domain.model.entity.Notification;
import com.ecommerce.notification.domain.model.vo.PageResult;
import com.ecommerce.notification.domain.repository.NotificationRepository;
import com.ecommerce.notification.infrastructure.persistence.mapper.NotificationMapper;
import com.ecommerce.notification.shared.responses.PagedResponse;

@ApplicationScoped
public class NotificationQueryService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Inject
    public NotificationQueryService(NotificationRepository repository, NotificationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @WithSession
    public Uni<List<NotificationResponse>> listByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(mapper::toResponses);
    }

    @WithSession
    public Uni<PagedResponse<NotificationResponse>> listPaged(int page, int limit) {
        return repository.findPaged(page, limit)
            .map((PageResult<Notification> pageResult) -> {
                List<NotificationResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }
}
