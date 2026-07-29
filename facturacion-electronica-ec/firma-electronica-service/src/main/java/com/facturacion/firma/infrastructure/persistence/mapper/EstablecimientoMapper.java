package com.facturacion.firma.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.EstablecimientoResponse;
import com.facturacion.firma.domain.model.entity.Establecimiento;
import com.facturacion.firma.infrastructure.persistence.entity.EstablecimientoEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EstablecimientoMapper {

    EstablecimientoResponse toResponse(Establecimiento domain);

    EstablecimientoEntity toEntity(Establecimiento domain);

    Establecimiento toDomain(EstablecimientoEntity entity);
}
