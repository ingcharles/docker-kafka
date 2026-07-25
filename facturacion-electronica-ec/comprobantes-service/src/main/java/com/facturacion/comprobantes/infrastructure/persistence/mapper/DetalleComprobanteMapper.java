package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.comprobantes.application.dto.DetalleComprobanteResponse;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.infrastructure.persistence.entity.DetalleComprobanteEntity;

/**
 * {@code impuestos} (dominio) <-> {@code impuestosJson} (entidad) no es algo
 * que MapStruct sepa convertir solo — se serializa/deserializa a mano con un
 * Jackson {@link ObjectMapper} local (sin inyección CDI: es un conversor sin
 * estado, no hace falta compartir la instancia con el resto del servicio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface DetalleComprobanteMapper {

    ObjectMapper JSON = new ObjectMapper();

    @Mapping(target = "precioTotalSinImpuesto", expression = "java(domain.precioTotalSinImpuesto())")
    DetalleComprobanteResponse toResponse(DetalleComprobante domain);

    @Mapping(target = "comprobante", ignore = true)
    @Mapping(target = "precioTotalSinImpuesto", expression = "java(domain.precioTotalSinImpuesto())")
    @Mapping(target = "impuestosJson", expression = "java(escribirImpuestos(domain.getImpuestos()))")
    DetalleComprobanteEntity toEntity(DetalleComprobante domain);

    @Mapping(target = "impuestos", expression = "java(leerImpuestos(entity.impuestosJson))")
    DetalleComprobante toDomain(DetalleComprobanteEntity entity);

    default String escribirImpuestos(List<Impuesto> impuestos) {
        try {
            return JSON.writeValueAsString(impuestos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar los impuestos de la línea de detalle", e);
        }
    }

    // destinatarioId se mapea 1-a-1 por nombre (mismo tipo UUID a ambos
    // lados) — MapStruct lo resuelve solo, no necesita @Mapping explícito.

    default List<Impuesto> leerImpuestos(String impuestosJson) {
        try {
            return JSON.readValue(impuestosJson, new TypeReference<List<Impuesto>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer los impuestos de la línea de detalle", e);
        }
    }
}
