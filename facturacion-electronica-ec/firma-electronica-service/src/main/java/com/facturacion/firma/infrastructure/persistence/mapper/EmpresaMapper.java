package com.facturacion.firma.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.model.enums.AmbienteSri;
import com.facturacion.firma.infrastructure.persistence.entity.EmpresaEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EmpresaMapper {

    EmpresaEntity toEntity(Empresa domain);

    Empresa toDomain(EmpresaEntity entity);

    default String map(AmbienteSri ambiente) {
        return ambiente == null ? null : ambiente.name();
    }

    default AmbienteSri map(String ambiente) {
        return ambiente == null ? null : AmbienteSri.valueOf(ambiente);
    }
}
