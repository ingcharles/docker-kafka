package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.facturacion.comprobantes.application.dto.DestinatarioGuiaRemisionResponse;
import com.facturacion.comprobantes.domain.model.entity.DestinatarioGuiaRemision;
import com.facturacion.comprobantes.infrastructure.persistence.entity.DestinatarioGuiaRemisionEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface DestinatarioGuiaRemisionMapper {

    @Mapping(target = "comprobante", ignore = true)
    DestinatarioGuiaRemisionEntity toEntity(DestinatarioGuiaRemision domain);

    DestinatarioGuiaRemision toDomain(DestinatarioGuiaRemisionEntity entity);

    DestinatarioGuiaRemisionResponse toResponse(DestinatarioGuiaRemision domain);
}
