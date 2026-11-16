package com.facturacion.inventario.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.application.dto.CategoriaResponse;
import com.facturacion.inventario.domain.model.entity.Categoria;
import com.facturacion.inventario.domain.model.enums.TipoCategoria;

/** Dominio → DTO de respuesta de Categoria. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface CategoriaResponseMapper {

    CategoriaResponse toResponse(Categoria domain);

    default String map(TipoCategoria tipo) {
        return tipo == null ? null : tipo.name();
    }
}
