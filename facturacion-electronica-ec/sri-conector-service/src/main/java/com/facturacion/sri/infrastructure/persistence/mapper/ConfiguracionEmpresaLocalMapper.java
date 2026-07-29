package com.facturacion.sri.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.sri.domain.model.entity.ConfiguracionEmpresaLocal;
import com.facturacion.sri.infrastructure.persistence.entity.ConfiguracionEmpresaLocalEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ConfiguracionEmpresaLocalMapper {

    ConfiguracionEmpresaLocalEntity toEntity(ConfiguracionEmpresaLocal domain);

    ConfiguracionEmpresaLocal toDomain(ConfiguracionEmpresaLocalEntity entity);
}
