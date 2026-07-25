package com.ecommerce.inventory.infrastructure.persistence.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ecommerce.inventory.application.dto.StockReservationResponse;
import com.ecommerce.inventory.domain.model.entity.StockReservation;
import com.ecommerce.inventory.domain.model.enums.ReservationStatus;
import com.ecommerce.inventory.infrastructure.persistence.entity.StockReservationEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface StockReservationMapper {

    StockReservationResponse toResponse(StockReservation domain);

    List<StockReservationResponse> toResponses(List<StockReservation> domains);

    StockReservation toDomain(StockReservationEntity entity);

    StockReservationEntity toEntity(StockReservation domain);

    default String map(ReservationStatus status) {
        return status == null ? null : status.name();
    }
}
