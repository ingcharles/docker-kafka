package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.comprobantes.domain.model.entity.EmisorLocal;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.infrastructure.persistence.entity.EmisorLocalEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EmisorLocalMapper {

    EmisorLocalEntity toEntity(EmisorLocal domain);

    EmisorLocal toDomain(EmisorLocalEntity entity);

    default String map(AmbienteSri ambiente) {
        return ambiente == null ? null : ambiente.name();
    }

    default AmbienteSri map(String ambiente) {
        return ambiente == null ? null : AmbienteSri.valueOf(ambiente);
    }
}
