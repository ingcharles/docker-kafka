package com.facturacion.identidad.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.identidad.domain.model.entity.ApiKey;
import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.identidad.infrastructure.persistence.entity.ApiKeyEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ApiKeyMapper {

    ApiKeyEntity toEntity(ApiKey domain);

    ApiKey toDomain(ApiKeyEntity entity);

    default String map(Rol rol) {
        return rol == null ? null : rol.name();
    }

    default Rol mapRol(String rol) {
        return rol == null ? null : Rol.valueOf(rol);
    }
}
