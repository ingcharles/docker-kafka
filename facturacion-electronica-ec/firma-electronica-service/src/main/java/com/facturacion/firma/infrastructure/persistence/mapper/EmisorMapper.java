package com.facturacion.firma.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.EmisorResponse;
import com.facturacion.firma.domain.model.entity.Emisor;
import com.facturacion.firma.domain.model.enums.AmbienteSri;
import com.facturacion.firma.infrastructure.persistence.entity.EmisorEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EmisorMapper {

    EmisorResponse toResponse(Emisor domain);

    EmisorEntity toEntity(Emisor domain);

    Emisor toDomain(EmisorEntity entity);

    default String map(AmbienteSri ambiente) {
        return ambiente == null ? null : ambiente.name();
    }

    default AmbienteSri map(String ambiente) {
        return ambiente == null ? null : AmbienteSri.valueOf(ambiente);
    }
}
