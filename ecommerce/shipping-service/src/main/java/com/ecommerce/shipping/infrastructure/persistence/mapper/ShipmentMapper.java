package com.ecommerce.shipping.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.ecommerce.shipping.application.dto.AddressResponse;
import com.ecommerce.shipping.application.dto.ShipmentResponse;
import com.ecommerce.shipping.domain.model.entity.Shipment;
import com.ecommerce.shipping.domain.model.vo.Address;
import com.ecommerce.shipping.infrastructure.persistence.entity.ShipmentEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ShipmentMapper {

    ShipmentResponse toResponse(Shipment domain);

    AddressResponse toResponse(Address address);

    @Mapping(target = "addressStreet", source = "address.street")
    @Mapping(target = "addressCity", source = "address.city")
    @Mapping(target = "addressState", source = "address.state")
    @Mapping(target = "addressZip", source = "address.zip")
    @Mapping(target = "addressCountry", source = "address.country")
    ShipmentEntity toEntity(Shipment domain);

    @Mapping(target = "address", expression = "java(new Address(entity.addressStreet, "
        + "entity.addressCity, entity.addressState, entity.addressZip, entity.addressCountry))")
    Shipment toDomain(ShipmentEntity entity);
}
