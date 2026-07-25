package com.facturacion.comprobantes.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.PageResult;

public interface ComprobanteRepository {

    Uni<Comprobante> persist(Comprobante comprobante);

    Uni<Comprobante> update(Comprobante comprobante);

    Uni<Comprobante> findById(UUID id);

    Uni<Comprobante> findByClaveAcceso(String claveAcceso);

    Uni<PageResult<Comprobante>> findPaged(int page, int limit, EstadoComprobante estado, UUID emisorId);

    /**
     * Devuelve el siguiente secuencial (empezando en 1) para esa combinación
     * emisor+tipo+establecimiento+puntoEmision, de forma atómica — el SRI
     * exige que sea consecutivo y sin huecos (Tabla 1, campo "N° del comprobante").
     */
    Uni<Long> siguienteSecuencial(UUID emisorId, TipoComprobante tipo, String establecimiento, String puntoEmision);
}
