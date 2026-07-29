package com.facturacion.identidad.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.identidad.domain.model.entity.EmpresaLocal;
import com.facturacion.identidad.infrastructure.persistence.entity.EmpresaLocalEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EmpresaLocalMapper {

    EmpresaLocalEntity toEntity(EmpresaLocal domain);

    EmpresaLocal toDomain(EmpresaLocalEntity entity);
}
