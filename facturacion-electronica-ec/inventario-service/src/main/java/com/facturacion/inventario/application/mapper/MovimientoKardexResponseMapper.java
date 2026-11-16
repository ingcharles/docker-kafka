package com.facturacion.inventario.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.application.dto.MovimientoKardexResponse;
import com.facturacion.inventario.domain.model.entity.MovimientoKardex;
import com.facturacion.inventario.domain.model.enums.TipoMovimientoKardex;

/** Dominio → DTO de respuesta de MovimientoKardex. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface MovimientoKardexResponseMapper {

    MovimientoKardexResponse toResponse(MovimientoKardex domain);

    default String map(TipoMovimientoKardex tipo) {
        return tipo == null ? null : tipo.name();
    }
}
