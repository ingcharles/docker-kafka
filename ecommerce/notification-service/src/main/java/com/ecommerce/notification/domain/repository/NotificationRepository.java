package com.ecommerce.notification.domain.repository;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.ecommerce.notification.domain.model.entity.Notification;
import com.ecommerce.notification.domain.model.vo.PageResult;

public interface NotificationRepository {

    Uni<Notification> persist(Notification notification);

    Uni<List<Notification>> findByOrderId(UUID orderId);

    Uni<PageResult<Notification>> findPaged(int page, int limit);
}
