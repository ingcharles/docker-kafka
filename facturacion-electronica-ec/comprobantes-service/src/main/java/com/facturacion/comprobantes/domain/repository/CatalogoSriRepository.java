package com.facturacion.comprobantes.domain.repository;

import java.time.LocalDate;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;

/** Lectura de los catálogos del SRI (tablas propias de este servicio). */
public interface CatalogoSriRepository {

    /** Los catálogos evaluados a {@code fecha} (la fecha de emisión del comprobante), leídos de la base en el momento. */
    Uni<CatalogosSriAFecha> cargar(LocalDate fecha);
}
