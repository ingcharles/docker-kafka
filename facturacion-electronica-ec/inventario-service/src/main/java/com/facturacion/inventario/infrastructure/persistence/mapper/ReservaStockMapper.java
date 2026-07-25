package com.facturacion.inventario.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.inventario.domain.model.entity.ReservaStock;
import com.facturacion.inventario.domain.model.enums.EstadoReserva;
import com.facturacion.inventario.infrastructure.persistence.entity.ReservaStockEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ReservaStockMapper {

    ReservaStockEntity toEntity(ReservaStock domain);

    ReservaStock toDomain(ReservaStockEntity entity);

    default String map(EstadoReserva estado) {
        return estado == null ? null : estado.name();
    }

    default EstadoReserva map(String estado) {
        return estado == null ? null : EstadoReserva.valueOf(estado);
    }
}
