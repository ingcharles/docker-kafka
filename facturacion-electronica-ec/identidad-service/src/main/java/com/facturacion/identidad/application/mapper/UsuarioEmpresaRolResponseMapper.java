package com.facturacion.identidad.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.identidad.application.dto.EmpresaRolResponse;
import com.facturacion.identidad.domain.model.entity.UsuarioEmpresaRol;
import com.facturacion.identidad.domain.model.enums.Rol;

/** Dominio → DTO de respuesta de UsuarioEmpresaRol. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface UsuarioEmpresaRolResponseMapper {

    EmpresaRolResponse toResponse(UsuarioEmpresaRol domain);

    default String map(Rol rol) {
        return rol == null ? null : rol.name();
    }
}
