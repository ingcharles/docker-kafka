package com.facturacion.inventario.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.application.dto.ItemResponse;
import com.facturacion.inventario.domain.model.entity.Item;
import com.facturacion.inventario.domain.model.enums.TipoItem;

/** Dominio → DTO de respuesta de Item. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ItemResponseMapper {

    ItemResponse toResponse(Item domain);

    default String map(TipoItem tipo) {
        return tipo == null ? null : tipo.name();
    }
}
