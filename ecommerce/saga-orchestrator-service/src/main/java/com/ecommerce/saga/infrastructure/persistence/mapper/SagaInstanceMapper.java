package com.ecommerce.saga.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.ecommerce.saga.domain.model.entity.SagaInstance;
import com.ecommerce.saga.domain.model.vo.Address;
import com.ecommerce.saga.infrastructure.persistence.entity.SagaInstanceEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface SagaInstanceMapper {

    @Mapping(target = "shippingStreet", source = "shippingAddress.street")
    @Mapping(target = "shippingCity", source = "shippingAddress.city")
    @Mapping(target = "shippingState", source = "shippingAddress.state")
    @Mapping(target = "shippingZip", source = "shippingAddress.zip")
    @Mapping(target = "shippingCountry", source = "shippingAddress.country")
    SagaInstanceEntity toEntity(SagaInstance domain);

    @Mapping(target = "shippingAddress", expression = "java(new Address(entity.shippingStreet, "
        + "entity.shippingCity, entity.shippingState, entity.shippingZip, entity.shippingCountry))")
    SagaInstance toDomain(SagaInstanceEntity entity);
}
