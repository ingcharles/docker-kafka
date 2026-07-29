package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.comprobantes.domain.model.entity.PuntoEmisionLocal;
import com.facturacion.comprobantes.infrastructure.persistence.entity.PuntoEmisionLocalEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface PuntoEmisionLocalMapper {

    PuntoEmisionLocalEntity toEntity(PuntoEmisionLocal domain);

    PuntoEmisionLocal toDomain(PuntoEmisionLocalEntity entity);
}
