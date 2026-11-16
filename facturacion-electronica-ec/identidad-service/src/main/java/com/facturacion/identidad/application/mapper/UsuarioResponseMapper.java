package com.facturacion.identidad.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.identidad.application.dto.UsuarioResponse;
import com.facturacion.identidad.domain.model.entity.Usuario;

/** Dominio → DTO de respuesta de Usuario. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface UsuarioResponseMapper {

    UsuarioResponse toResponse(Usuario domain);
}
