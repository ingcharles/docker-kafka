package com.facturacion.inventario.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.application.dto.CategoriaResponse;
import com.facturacion.inventario.domain.model.entity.Categoria;
import com.facturacion.inventario.domain.model.enums.TipoCategoria;
import com.facturacion.inventario.infrastructure.persistence.entity.CategoriaEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface CategoriaMapper {

    CategoriaResponse toResponse(Categoria domain);

    CategoriaEntity toEntity(Categoria domain);

    Categoria toDomain(CategoriaEntity entity);

    default String map(TipoCategoria tipo) {
        return tipo == null ? null : tipo.name();
    }

    default TipoCategoria map(String tipo) {
        return tipo == null ? null : TipoCategoria.valueOf(tipo);
    }
}
