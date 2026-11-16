package com.facturacion.notificacion.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.notificacion.application.dto.NotificacionEnviadaResponse;
import com.facturacion.notificacion.domain.model.entity.NotificacionEnviada;
import com.facturacion.notificacion.domain.model.enums.EstadoNotificacion;

/** Dominio → DTO de respuesta de NotificacionEnviada. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface NotificacionEnviadaResponseMapper {

    NotificacionEnviadaResponse toResponse(NotificacionEnviada domain);

    default String map(EstadoNotificacion estado) {
        return estado == null ? null : estado.name();
    }
}
