package com.facturacion.inventario.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.domain.model.entity.ImagenItem;
import com.facturacion.inventario.infrastructure.persistence.entity.ImagenItemEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ImagenItemMapper {

    ImagenItemEntity toEntity(ImagenItem domain);

    ImagenItem toDomain(ImagenItemEntity entity);
}
