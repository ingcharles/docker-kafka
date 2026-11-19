package com.facturacion.comprobantes.domain.repository;

import java.math.BigDecimal;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.paging.PageResult;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;

public interface ComprobanteRepository {

    Uni<Comprobante> persist(Comprobante comprobante);

    Uni<Comprobante> update(Comprobante comprobante);

    Uni<Comprobante> findById(UUID id);

    Uni<Comprobante> findByClaveAcceso(String claveAcceso);

    /**
     * El comprobante de esa empresa con ese tipo y número, o {@code null}. Lo deja bloqueado hasta el final de la
     * transacción para que dos notas de crédito sobre el mismo documento no se comprueben a la vez.
     */
    Uni<Comprobante> buscarParaModificar(String ruc, TipoComprobante tipo, String establecimiento, String puntoEmision,
            String secuencial);

    /** Suma del importe de las notas de crédito vigentes (no falladas ni rechazadas) emitidas sobre un documento. */
    Uni<BigDecimal> sumarNotasCreditoVigentes(String ruc, String docModificadoTipo, String docModificadoNumero);

    Uni<PageResult<Comprobante>> findPaged(int page, int limit, EstadoComprobante estado, String empresaRuc);

    /**
     * Devuelve el siguiente secuencial (empezando en 1) para esa combinación
     * ruc+tipo+establecimiento+puntoEmision, de forma atómica — el SRI
     * exige que sea consecutivo y sin huecos (Tabla 1, campo "N° del comprobante").
     */
    Uni<Long> obtenerSiguienteSecuencial(String ruc, TipoComprobante tipo, String establecimiento, String puntoEmision);
}
