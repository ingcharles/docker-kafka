package com.facturacion.inventario.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.application.dto.ImagenItemResponse;
import com.facturacion.inventario.domain.model.entity.ImagenItem;

/** Dominio → DTO de respuesta de ImagenItem. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ImagenItemResponseMapper {

    ImagenItemResponse toResponse(ImagenItem domain);
}
