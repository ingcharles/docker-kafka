package com.facturacion.firma.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.CertificadoMetadataResponse;
import com.facturacion.firma.domain.model.entity.CertificadoDigital;
import com.facturacion.firma.domain.model.enums.EstadoCertificado;
import com.facturacion.firma.infrastructure.persistence.entity.CertificadoDigitalEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface CertificadoDigitalMapper {

    CertificadoMetadataResponse toResponse(CertificadoDigital domain);

    CertificadoDigitalEntity toEntity(CertificadoDigital domain);

    CertificadoDigital toDomain(CertificadoDigitalEntity entity);

    default String map(EstadoCertificado estado) {
        return estado == null ? null : estado.name();
    }

    default EstadoCertificado map(String estado) {
        return estado == null ? null : EstadoCertificado.valueOf(estado);
    }
}
