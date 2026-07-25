package com.ecommerce.saga.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ecommerce.saga.domain.model.entity.SagaStep;
import com.ecommerce.saga.infrastructure.persistence.entity.SagaStepEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface SagaStepMapper {

    SagaStep toDomain(SagaStepEntity entity);

    SagaStepEntity toEntity(SagaStep domain);
}
