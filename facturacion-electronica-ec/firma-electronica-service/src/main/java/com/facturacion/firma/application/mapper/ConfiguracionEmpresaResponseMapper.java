package com.facturacion.firma.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.ConfiguracionEmpresaResponse;
import com.facturacion.firma.domain.model.entity.ConfiguracionEmpresa;

/** Dominio → DTO de respuesta de ConfiguracionEmpresa. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ConfiguracionEmpresaResponseMapper {

    ConfiguracionEmpresaResponse toResponse(ConfiguracionEmpresa domain);
}
