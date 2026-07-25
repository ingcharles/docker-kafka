package com.ecommerce.notification.infrastructure.persistence.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ecommerce.notification.application.dto.NotificationResponse;
import com.ecommerce.notification.domain.model.entity.Notification;
import com.ecommerce.notification.domain.model.enums.NotificationStatus;
import com.ecommerce.notification.infrastructure.persistence.entity.NotificationEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface NotificationMapper {

    NotificationResponse toResponse(Notification domain);

    List<NotificationResponse> toResponses(List<Notification> domains);

    Notification toDomain(NotificationEntity entity);

    NotificationEntity toEntity(Notification domain);

    default String map(NotificationStatus status) {
        return status == null ? null : status.name();
    }
}
