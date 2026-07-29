package com.facturacion.firma.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.PuntoEmisionResponse;
import com.facturacion.firma.domain.model.entity.PuntoEmision;
import com.facturacion.firma.infrastructure.persistence.entity.PuntoEmisionEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface PuntoEmisionMapper {

    PuntoEmisionResponse toResponse(PuntoEmision domain);

    PuntoEmisionEntity toEntity(PuntoEmision domain);

    PuntoEmision toDomain(PuntoEmisionEntity entity);
}
