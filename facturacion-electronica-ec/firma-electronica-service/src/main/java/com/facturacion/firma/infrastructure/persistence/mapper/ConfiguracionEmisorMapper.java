package com.facturacion.firma.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.ConfiguracionEmisorResponse;
import com.facturacion.firma.domain.model.entity.ConfiguracionEmisor;
import com.facturacion.firma.infrastructure.persistence.entity.ConfiguracionEmisorEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ConfiguracionEmisorMapper {

    ConfiguracionEmisorResponse toResponse(ConfiguracionEmisor domain);

    ConfiguracionEmisorEntity toEntity(ConfiguracionEmisor domain);

    ConfiguracionEmisor toDomain(ConfiguracionEmisorEntity entity);
}
