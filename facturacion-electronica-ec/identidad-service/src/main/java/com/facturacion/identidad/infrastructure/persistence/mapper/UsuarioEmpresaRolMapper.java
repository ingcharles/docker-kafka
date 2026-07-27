package com.facturacion.identidad.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.identidad.application.dto.EmpresaRolResponse;
import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;
import com.facturacion.identidad.domain.model.enums.Rol;
import com.facturacion.identidad.infrastructure.persistence.entity.UsuarioEmpresaRolEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface UsuarioEmpresaRolMapper {

    UsuarioEmpresaRolEntity toEntity(UsuarioEmpresaRol domain);

    UsuarioEmpresaRol toDomain(UsuarioEmpresaRolEntity entity);

    EmpresaRolResponse toResponse(UsuarioEmpresaRol domain);

    default String map(Rol rol) {
        return rol == null ? null : rol.name();
    }

    default Rol mapRol(String rol) {
        return rol == null ? null : Rol.valueOf(rol);
    }
}
