package com.facturacion.orquestacion.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.infrastructure.persistence.entity.SagaInstanceEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface SagaInstanceMapper {

    SagaInstanceEntity toEntity(SagaInstance domain);

    SagaInstance toDomain(SagaInstanceEntity entity);

    default String map(SagaState state) {
        return state == null ? null : state.name();
    }

    default SagaState mapState(String state) {
        return state == null ? null : SagaState.valueOf(state);
    }
}
