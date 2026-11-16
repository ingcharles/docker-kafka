package com.facturacion.identidad.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.identidad.domain.model.entity.Usuario;
import com.facturacion.identidad.infrastructure.persistence.entity.UsuarioEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface UsuarioMapper {

    UsuarioEntity toEntity(Usuario domain);

    Usuario toDomain(UsuarioEntity entity);

}
