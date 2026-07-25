package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ComprobanteEntity;

/**
 * {@code detalles} sí se mapea automáticamente (vía {@link DetalleComprobanteMapper}),
 * pero la referencia inversa {@code DetalleComprobanteEntity.comprobante} no
 * es algo que MapStruct pueda inferir a partir de los nombres de los campos
 * — {@link #linkDetallesToComprobante} la completa manualmente justo
 * después del mapeo principal, mismo patrón que {@code OrderMapper} en
 * ecommerce/order-service para {@code OrderItemEntity.order}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI,
    uses = { DetalleComprobanteMapper.class, DestinatarioGuiaRemisionMapper.class })
public interface ComprobanteMapper {

    ComprobanteEntity toEntity(Comprobante domain);

    Comprobante toDomain(ComprobanteEntity entity);

    ComprobanteResponse toResponse(Comprobante domain);

    @AfterMapping
    default void linkDetallesToComprobante(@MappingTarget ComprobanteEntity entity) {
        if (entity.detalles != null) {
            entity.detalles.forEach(detalle -> detalle.comprobante = entity);
        }
        if (entity.destinatarios != null) {
            entity.destinatarios.forEach(destinatario -> destinatario.comprobante = entity);
        }
    }

    default String map(TipoComprobante tipo) {
        return tipo == null ? null : tipo.name();
    }

    default TipoComprobante mapTipoComprobante(String tipo) {
        return tipo == null ? null : TipoComprobante.valueOf(tipo);
    }

    default String map(AmbienteSri ambiente) {
        return ambiente == null ? null : ambiente.name();
    }

    default AmbienteSri mapAmbiente(String ambiente) {
        return ambiente == null ? null : AmbienteSri.valueOf(ambiente);
    }

    default String map(EstadoComprobante estado) {
        return estado == null ? null : estado.name();
    }

    default EstadoComprobante mapEstado(String estado) {
        return estado == null ? null : EstadoComprobante.valueOf(estado);
    }
}
