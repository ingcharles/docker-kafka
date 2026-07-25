package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.comprobantes.domain.model.entity.ItemLocal;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ItemLocalEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ItemLocalMapper {

    ItemLocalEntity toEntity(ItemLocal domain);

    ItemLocal toDomain(ItemLocalEntity entity);
}
