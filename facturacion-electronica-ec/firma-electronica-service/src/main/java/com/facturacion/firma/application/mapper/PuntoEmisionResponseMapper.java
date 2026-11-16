package com.facturacion.firma.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.PuntoEmisionResponse;
import com.facturacion.firma.domain.model.entity.PuntoEmision;

/** Dominio → DTO de respuesta de PuntoEmision. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface PuntoEmisionResponseMapper {

    PuntoEmisionResponse toResponse(PuntoEmision domain);
}
