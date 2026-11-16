package com.facturacion.sri.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.sri.domain.model.entity.EnvioSri;
import com.facturacion.sri.domain.model.enums.AmbienteSri;
import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;
import com.facturacion.sri.infrastructure.persistence.entity.EnvioSriEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EnvioSriMapper {

    EnvioSriEntity toEntity(EnvioSri domain);

    EnvioSri toDomain(EnvioSriEntity entity);

    default String map(AmbienteSri ambiente) {
        return ambiente == null ? null : ambiente.name();
    }

    default AmbienteSri mapAmbiente(String ambiente) {
        return ambiente == null ? null : AmbienteSri.valueOf(ambiente);
    }

    default String map(EstadoEnvioSri estado) {
        return estado == null ? null : estado.name();
    }

    default EstadoEnvioSri mapEstado(String estado) {
        return estado == null ? null : EstadoEnvioSri.valueOf(estado);
    }
}
