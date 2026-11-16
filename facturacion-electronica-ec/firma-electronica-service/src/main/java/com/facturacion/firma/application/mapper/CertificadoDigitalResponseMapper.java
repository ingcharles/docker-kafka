package com.facturacion.firma.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.firma.application.dto.CertificadoMetadataResponse;
import com.facturacion.firma.domain.model.entity.CertificadoDigital;
import com.facturacion.firma.domain.model.enums.EstadoCertificado;

/** Dominio → DTO de respuesta de CertificadoDigital. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface CertificadoDigitalResponseMapper {

    CertificadoMetadataResponse toResponse(CertificadoDigital domain);

    default String map(EstadoCertificado estado) {
        return estado == null ? null : estado.name();
    }
}
