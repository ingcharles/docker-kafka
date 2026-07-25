package com.facturacion.inventario.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.application.dto.ItemResponse;
import com.facturacion.inventario.domain.model.entity.Item;
import com.facturacion.inventario.domain.model.enums.TipoItem;
import com.facturacion.inventario.infrastructure.persistence.entity.ItemEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ItemMapper {

    ItemResponse toResponse(Item domain);

    ItemEntity toEntity(Item domain);

    Item toDomain(ItemEntity entity);

    default String map(TipoItem tipo) {
        return tipo == null ? null : tipo.name();
    }

    default TipoItem map(String tipo) {
        return tipo == null ? null : TipoItem.valueOf(tipo);
    }
}
