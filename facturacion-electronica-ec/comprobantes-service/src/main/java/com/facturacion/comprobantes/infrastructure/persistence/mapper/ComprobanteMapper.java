package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.Pago;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ComprobanteEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ComprobantePagoEntity;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * {@code detalles} sí se mapea automáticamente (vía {@link DetalleComprobanteMapper}),
 * pero la referencia inversa {@code DetalleComprobanteEntity.comprobante} no
 * es algo que MapStruct pueda inferir a partir de los nombres de los campos
 * — {@link #vincularDetallesAComprobante} la completa manualmente justo
 * después del mapeo principal.
 * <p>
 * {@code pagos} (dominio, lista de {@link Pago}) <-> {@code pagos} (entidad, filas de
 * {@code comprobante_pago}) se construye a mano: cada fila lleva su {@code orden} y la
 * referencia inversa se completa en {@link #vincularDetallesAComprobante}. {@code camposAdicionales}
 * <-> {@code camposAdicionalesJson} sigue serializándose como JSON.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI,
    uses = { DetalleComprobanteMapper.class, DestinatarioGuiaRemisionMapper.class })
public interface ComprobanteMapper {

    ObjectMapper JSON = new ObjectMapper();

    @Mapping(target = "pagos", expression = "java(escribirPagos(domain.getPagos()))")
    @Mapping(target = "camposAdicionalesJson", expression = "java(escribirCamposAdicionales(domain.getCamposAdicionales()))")
    ComprobanteEntity toEntity(Comprobante domain);

    @Mapping(target = "pagos", expression = "java(leerPagos(entity.pagos))")
    @Mapping(target = "camposAdicionales", expression = "java(leerCamposAdicionales(entity.camposAdicionalesJson))")
    Comprobante toDomain(ComprobanteEntity entity);

    default List<ComprobantePagoEntity> escribirPagos(List<Pago> pagos) {
        List<ComprobantePagoEntity> filas = new ArrayList<>();
        if (pagos == null) {
            return filas;
        }
        for (int i = 0; i < pagos.size(); i++) {
            Pago pago = pagos.get(i);
            ComprobantePagoEntity fila = new ComprobantePagoEntity();
            fila.id = UUID.randomUUID();
            fila.orden = i;
            fila.formaPago = pago.formaPago();
            fila.total = pago.total();
            fila.plazo = pago.plazo() == null || pago.plazo().isBlank() ? null : new BigDecimal(pago.plazo().trim());
            fila.unidadTiempo = pago.unidadTiempo() == null || pago.unidadTiempo().isBlank() ? null : pago.unidadTiempo();
            filas.add(fila);
        }
        return filas;
    }

    default List<Pago> leerPagos(List<ComprobantePagoEntity> filas) {
        List<Pago> pagos = new ArrayList<>();
        if (filas == null) {
            return pagos;
        }
        for (ComprobantePagoEntity fila : filas) {
            // plazo es numeric(14,2) en la base; "30.00" se vuelve a "30" para que coincida con lo que mando el cliente.
            String plazo = fila.plazo == null ? null : fila.plazo.stripTrailingZeros().toPlainString();
            pagos.add(new Pago(fila.formaPago, fila.total, plazo, fila.unidadTiempo));
        }
        return pagos;
    }

    default String escribirCamposAdicionales(List<CampoAdicional> campos) {
        try {
            return JSON.writeValueAsString(campos == null ? List.of() : campos);
        } catch (JsonProcessingException e) {
            throw new ErrorInternoException(e, CodigoError.CAMPOS_ADICIONALES_NO_SERIALIZABLES);
        }
    }

    default List<CampoAdicional> leerCamposAdicionales(String camposAdicionalesJson) {
        if (camposAdicionalesJson == null) {
            return List.of();
        }
        try {
            return JSON.readValue(camposAdicionalesJson, new TypeReference<List<CampoAdicional>>() {
            });
        } catch (Exception e) {
            throw new ErrorInternoException(e, CodigoError.CAMPOS_ADICIONALES_NO_LEIBLES);
        }
    }

    @AfterMapping
    default void vincularDetallesAComprobante(@MappingTarget ComprobanteEntity entity) {
        if (entity.detalles != null) {
            entity.detalles.forEach(detalle -> detalle.comprobante = entity);
        }
        if (entity.pagos != null) {
            entity.pagos.forEach(pago -> pago.comprobante = entity);
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
