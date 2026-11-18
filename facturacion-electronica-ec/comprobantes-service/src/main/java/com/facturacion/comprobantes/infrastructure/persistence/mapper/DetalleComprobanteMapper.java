package com.facturacion.comprobantes.infrastructure.persistence.mapper;

import java.time.LocalDate;
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
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.application.dto.DetalleComprobanteResponse;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.infrastructure.persistence.entity.DetalleComprobanteEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.DetalleImpuestoEntity;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * {@code impuestos} (dominio, lista de {@link Impuesto}) <-> {@code impuestos}
 * (entidad, filas de {@code detalle_impuesto}) no es algo que MapStruct sepa
 * convertir solo: cada fila lleva su {@code orden} (la posición en la lista) y
 * una referencia inversa a la línea, así que se construye a mano — la
 * referencia inversa se completa en {@link #vincularImpuestosADetalle}, mismo
 * patrón que {@code ComprobanteMapper.vincularDetallesAComprobante}.
 * <p>
 * El dominio guarda {@code docSustentoFecha} como texto dd/MM/aaaa (así va en
 * el XML del SRI); la columna es {@code date}, se convierte aquí.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface DetalleComprobanteMapper {

    ObjectMapper JSON = new ObjectMapper();

    @Mapping(target = "precioTotalSinImpuesto", expression = "java(domain.calcularPrecioTotalSinImpuesto())")
    DetalleComprobanteResponse toResponse(DetalleComprobante domain);

    @Mapping(target = "comprobante", ignore = true)
    @Mapping(target = "precioTotalSinImpuesto", expression = "java(domain.calcularPrecioTotalSinImpuesto())")
    @Mapping(target = "impuestos", expression = "java(escribirImpuestos(domain.getImpuestos()))")
    @Mapping(target = "detallesAdicionalesJson", expression = "java(escribirDetallesAdicionales(domain.getDetallesAdicionales()))")
    DetalleComprobanteEntity toEntity(DetalleComprobante domain);

    @Mapping(target = "impuestos", expression = "java(leerImpuestos(entity.impuestos))")
    @Mapping(target = "detallesAdicionales", expression = "java(leerDetallesAdicionales(entity.detallesAdicionalesJson))")
    DetalleComprobante toDomain(DetalleComprobanteEntity entity);

    default String escribirDetallesAdicionales(List<CampoAdicional> detalles) {
        try {
            return JSON.writeValueAsString(detalles == null ? List.of() : detalles);
        } catch (JsonProcessingException e) {
            throw new ErrorInternoException(e, CodigoError.DETALLES_ADICIONALES_NO_SERIALIZABLES);
        }
    }

    default List<CampoAdicional> leerDetallesAdicionales(String detallesJson) {
        if (detallesJson == null) {
            return List.of();
        }
        try {
            return JSON.readValue(detallesJson, new TypeReference<List<CampoAdicional>>() {
            });
        } catch (Exception e) {
            throw new ErrorInternoException(e, CodigoError.DETALLES_ADICIONALES_NO_LEIBLES);
        }
    }

    default List<DetalleImpuestoEntity> escribirImpuestos(List<Impuesto> impuestos) {
        List<DetalleImpuestoEntity> filas = new ArrayList<>();
        if (impuestos == null) {
            return filas;
        }
        for (int i = 0; i < impuestos.size(); i++) {
            Impuesto impuesto = impuestos.get(i);
            DetalleImpuestoEntity fila = new DetalleImpuestoEntity();
            fila.id = UUID.randomUUID();
            fila.orden = i;
            fila.codigo = impuesto.codigo();
            fila.codigoPorcentaje = impuesto.codigoPorcentaje();
            fila.tarifa = impuesto.tarifa();
            fila.baseImponible = impuesto.baseImponible();
            fila.valor = impuesto.valor();
            fila.codigoRetencion = impuesto.codigoRetencion();
            fila.docSustentoCodigo = impuesto.docSustentoCodigo();
            fila.docSustentoNumero = impuesto.docSustentoNumero();
            fila.docSustentoFecha = convertirFechaSri(impuesto.docSustentoFecha());
            filas.add(fila);
        }
        return filas;
    }

    default List<Impuesto> leerImpuestos(List<DetalleImpuestoEntity> filas) {
        List<Impuesto> impuestos = new ArrayList<>();
        if (filas == null) {
            return impuestos;
        }
        for (DetalleImpuestoEntity fila : filas) {
            impuestos.add(new Impuesto(
                fila.codigo,
                fila.codigoPorcentaje,
                fila.tarifa,
                fila.baseImponible,
                fila.valor,
                fila.codigoRetencion,
                fila.docSustentoCodigo,
                fila.docSustentoNumero,
                fila.docSustentoFecha == null ? null : FechasSri.formatearFecha(fila.docSustentoFecha)));
        }
        return impuestos;
    }

    private static LocalDate convertirFechaSri(String fecha) {
        return fecha == null || fecha.isBlank() ? null : FechasSri.leerFecha(fecha);
    }

    @AfterMapping
    default void vincularImpuestosADetalle(@MappingTarget DetalleComprobanteEntity entity) {
        if (entity.impuestos != null) {
            entity.impuestos.forEach(impuesto -> impuesto.detalle = entity);
        }
    }

    // destinatarioId se mapea 1-a-1 por nombre (mismo tipo UUID a ambos
    // lados) — MapStruct lo resuelve solo, no necesita @Mapping explícito.
}
