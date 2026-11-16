package com.facturacion.identidad.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.identidad.application.dto.ApiKeyResponse;
import com.facturacion.identidad.domain.model.entity.ApiKey;
import com.facturacion.identidad.domain.model.enums.Rol;

/** Dominio → DTO de respuesta de ApiKey. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ApiKeyResponseMapper {

    ApiKeyResponse toResponse(ApiKey domain);

    default String map(Rol rol) {
        return rol == null ? null : rol.name();
    }
}
