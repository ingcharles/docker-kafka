package com.facturacion.inventario.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.domain.model.entity.MovimientoKardex;
import com.facturacion.inventario.domain.model.enums.TipoMovimientoKardex;
import com.facturacion.inventario.infrastructure.persistence.entity.MovimientoKardexEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface MovimientoKardexMapper {

    MovimientoKardexEntity toEntity(MovimientoKardex domain);

    MovimientoKardex toDomain(MovimientoKardexEntity entity);

    default String map(TipoMovimientoKardex tipo) {
        return tipo == null ? null : tipo.name();
    }

    default TipoMovimientoKardex map(String tipo) {
        return tipo == null ? null : TipoMovimientoKardex.valueOf(tipo);
    }
}
