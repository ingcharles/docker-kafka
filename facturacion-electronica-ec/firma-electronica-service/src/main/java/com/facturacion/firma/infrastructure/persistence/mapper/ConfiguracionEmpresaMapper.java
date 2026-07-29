package com.facturacion.firma.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.ConfiguracionEmpresaResponse;
import com.facturacion.firma.domain.model.entity.ConfiguracionEmpresa;
import com.facturacion.firma.infrastructure.persistence.entity.ConfiguracionEmpresaEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ConfiguracionEmpresaMapper {

    ConfiguracionEmpresaResponse toResponse(ConfiguracionEmpresa domain);

    ConfiguracionEmpresaEntity toEntity(ConfiguracionEmpresa domain);

    ConfiguracionEmpresa toDomain(ConfiguracionEmpresaEntity entity);
}
