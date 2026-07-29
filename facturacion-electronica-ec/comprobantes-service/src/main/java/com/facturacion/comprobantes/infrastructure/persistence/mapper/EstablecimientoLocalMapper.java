package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.comprobantes.domain.model.entity.EstablecimientoLocal;
import com.facturacion.comprobantes.infrastructure.persistence.entity.EstablecimientoLocalEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EstablecimientoLocalMapper {

    EstablecimientoLocalEntity toEntity(EstablecimientoLocal domain);

    EstablecimientoLocal toDomain(EstablecimientoLocalEntity entity);
}
