package com.facturacion.firma.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.EmpresaResponse;
import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.model.enums.AmbienteSri;

/** Dominio → DTO de respuesta de Empresa. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EmpresaResponseMapper {

    EmpresaResponse toResponse(Empresa domain);

    default String map(AmbienteSri ambiente) {
        return ambiente == null ? null : ambiente.name();
    }
}
