package com.facturacion.notificacion.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.notificacion.application.dto.NotificacionEnviadaResponse;
import com.facturacion.notificacion.domain.model.entity.NotificacionEnviada;
import com.facturacion.notificacion.domain.model.enums.EstadoNotificacion;
import com.facturacion.notificacion.infrastructure.persistence.entity.NotificacionEnviadaEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface NotificacionEnviadaMapper {

    NotificacionEnviadaEntity toEntity(NotificacionEnviada domain);

    NotificacionEnviada toDomain(NotificacionEnviadaEntity entity);

    NotificacionEnviadaResponse toResponse(NotificacionEnviada domain);

    default String map(EstadoNotificacion estado) {
        return estado == null ? null : estado.name();
    }

    default EstadoNotificacion mapEstado(String estado) {
        return estado == null ? null : EstadoNotificacion.valueOf(estado);
    }
}
