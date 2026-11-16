package com.facturacion.firma.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.EstablecimientoResponse;
import com.facturacion.firma.domain.model.entity.Establecimiento;

/** Dominio → DTO de respuesta de Establecimiento. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EstablecimientoResponseMapper {

    EstablecimientoResponse toResponse(Establecimiento domain);
}
