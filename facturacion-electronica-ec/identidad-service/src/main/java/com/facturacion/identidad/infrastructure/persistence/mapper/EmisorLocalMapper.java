package com.facturacion.identidad.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.identidad.domain.model.entity.EmisorLocal;
import com.facturacion.identidad.infrastructure.persistence.entity.EmisorLocalEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EmisorLocalMapper {

    EmisorLocalEntity toEntity(EmisorLocal domain);

    EmisorLocal toDomain(EmisorLocalEntity entity);
}
