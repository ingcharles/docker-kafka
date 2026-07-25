package com.facturacion.orquestacion.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.orquestacion.domain.model.entity.SagaStep;
import com.facturacion.orquestacion.infrastructure.persistence.entity.SagaStepEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface SagaStepMapper {

    SagaStepEntity toEntity(SagaStep domain);

    SagaStep toDomain(SagaStepEntity entity);
}
