package com.facturacion.sri.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.sri.application.dto.EnvioSriResponse;
import com.facturacion.sri.domain.model.entity.EnvioSri;
import com.facturacion.sri.domain.model.enums.AmbienteSri;
import com.facturacion.sri.domain.model.enums.EstadoEnvioSri;

/** Dominio → DTO de respuesta de EnvioSri. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface EnvioSriResponseMapper {

    EnvioSriResponse toResponse(EnvioSri domain);

    default String map(AmbienteSri ambiente) {
        return ambiente == null ? null : ambiente.name();
    }
    default String map(EstadoEnvioSri estado) {
        return estado == null ? null : estado.name();
    }
}
