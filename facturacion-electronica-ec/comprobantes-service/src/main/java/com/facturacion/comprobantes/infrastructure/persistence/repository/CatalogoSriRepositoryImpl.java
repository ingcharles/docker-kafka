package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.enums.TipoTarifa;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.FormaPagoRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.ImpuestoRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.RetencionRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.SustentoRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.TarifaRow;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha.TipoIdentificacionRow;
import com.facturacion.comprobantes.domain.repository.CatalogoSriRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.FormaPagoEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ImpuestoEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ImpuestoRetencionEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.RetencionEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.SustentoTributarioEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.TarifaImpuestoEntity;
import com.facturacion.comprobantes.infrastructure.persistence.entity.TipoIdentificacionEntity;

/**
 * Lee los catálogos del SRI de la base con entidades (Hibernate Reactive), <b>sin caché</b>: cada emisión ve el
 * valor actual de la tabla, así que un cambio hecho con {@code PUT /catalogos/...} (p. ej. el IVA) aplica a la
 * siguiente factura, con una o con varias instancias del servicio.
 * <p>
 * Las siete lecturas se encadenan, no se lanzan en paralelo: dos consultas concurrentes sobre la misma sesión
 * reactiva la rompen (ver el comentario de {@code ComprobanteService.emitir}). Hay que llamar a {@link #cargar}
 * dentro de una sesión/transacción reactiva, como ya hacen los métodos de {@code ComprobanteService}.
 * <p>
 * La vigencia no se filtra aquí: {@link CatalogosSriAFecha} la evalúa en memoria a la fecha pedida.
 */
@ApplicationScoped
public class CatalogoSriRepositoryImpl implements CatalogoSriRepository {

    @Override
    public Uni<CatalogosSriAFecha> cargar(LocalDate fecha) {
        return FormaPagoEntity.<FormaPagoEntity>listAll()
            .flatMap(formasPago -> ImpuestoEntity.<ImpuestoEntity>listAll()
                .flatMap(impuestos -> TarifaImpuestoEntity.<TarifaImpuestoEntity>listAll()
                    .flatMap(tarifas -> TipoIdentificacionEntity.<TipoIdentificacionEntity>listAll()
                        .flatMap(tipos -> ImpuestoRetencionEntity.<ImpuestoRetencionEntity>listAll()
                            .flatMap(impuestosRetencion -> RetencionEntity.<RetencionEntity>listAll()
                                .flatMap(retenciones -> SustentoTributarioEntity.<SustentoTributarioEntity>listAll()
                                    .map(sustentos -> new CatalogosSriAFecha(fecha,
                                        mapear(formasPago, CatalogoSriRepositoryImpl::convertirFormaPago),
                                        mapear(impuestos, e -> new ImpuestoRow(e.codigo, e.descripcion, e.activo)),
                                        mapear(tarifas, CatalogoSriRepositoryImpl::convertirTarifa),
                                        mapear(tipos, e -> new TipoIdentificacionRow(e.codigo, e.descripcion,
                                            e.soloFactura, e.activo)),
                                        mapear(impuestosRetencion,
                                            e -> new ImpuestoRow(e.codigo, e.descripcion, e.activo)),
                                        mapear(retenciones, CatalogoSriRepositoryImpl::convertirRetencion),
                                        mapear(sustentos, e -> new SustentoRow(e.codigo, e.descripcion, e.activo))))))))));
    }

    private static <E, R> List<R> mapear(List<E> entidades, java.util.function.Function<E, R> mapeo) {
        return entidades.stream().map(mapeo).toList();
    }

    private static FormaPagoRow convertirFormaPago(FormaPagoEntity e) {
        return new FormaPagoRow(e.codigo, e.descripcion, e.vigenteDesde, e.vigenteHasta, e.activo);
    }

    private static TarifaRow convertirTarifa(TarifaImpuestoEntity e) {
        return new TarifaRow(e.impuestoCodigo, e.codigoPorcentaje, e.descripcion, e.tarifa,
            TipoTarifa.valueOf(e.tipoTarifa), e.vigenteDesde, e.vigenteHasta, e.activo);
    }

    private static RetencionRow convertirRetencion(RetencionEntity e) {
        return new RetencionRow(e.impuestoCodigo, e.codigo, e.vigenteDesde, e.descripcion, e.porcentaje,
            e.vigenteHasta, e.activo);
    }
}
