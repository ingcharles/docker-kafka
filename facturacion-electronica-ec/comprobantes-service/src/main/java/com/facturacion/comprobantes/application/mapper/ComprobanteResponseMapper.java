package com.facturacion.comprobantes.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;

/** Dominio → DTO de respuesta de Comprobante. Vive en application: el mapeo a entidades JPA es de infraestructura. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ComprobanteResponseMapper {

    ComprobanteResponse toResponse(Comprobante domain);

    default String map(TipoComprobante tipo) {
        return tipo == null ? null : tipo.name();
    }
    default String map(AmbienteSri ambiente) {
        return ambiente == null ? null : ambiente.name();
    }
    default String map(EstadoComprobante estado) {
        return estado == null ? null : estado.name();
    }
}
