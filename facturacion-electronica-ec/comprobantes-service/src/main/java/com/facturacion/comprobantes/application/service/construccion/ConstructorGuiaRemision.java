package com.facturacion.comprobantes.application.service.construccion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.application.dto.CrearGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.DestinatarioGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.DetalleGuiaRemisionRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DestinatarioGuiaRemision;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.shared.error.CodigoError;

/** Arma una guía de remisión (ver {@link ConstructorComprobante}). */
@ApplicationScoped
public class ConstructorGuiaRemision implements ConstructorComprobante<CrearGuiaRemisionRequest> {

    private final ConstructorComun comun;

    @Inject
    public ConstructorGuiaRemision(ConstructorComun comun) {
        this.comun = comun;
    }

    @Override
    public Uni<Comprobante> construir(CrearGuiaRemisionRequest request, long secuencial, CatalogosSriAFecha catalogos) {
        Instant ahora = Instant.now();
        validarInicioDelTransporte(request, ahora);
        Comprobante comprobante = comun.armarEncabezado(request.getEmpresa(), TipoComprobante.GUIA_REMISION, secuencial,
            ahora);

        List<DestinatarioGuiaRemision> destinatarios = new ArrayList<>();
        List<DetalleComprobante> detalles = new ArrayList<>();
        for (DestinatarioGuiaRemisionRequest destinoRequest : request.getDestinatarios()) {
            DestinatarioGuiaRemision destinatario = new DestinatarioGuiaRemision();
            destinatario.setId(UUID.randomUUID());
            destinatario.setIdentificacion(destinoRequest.getIdentificacion());
            destinatario.setRazonSocial(destinoRequest.getRazonSocial());
            destinatario.setDireccion(destinoRequest.getDireccion());
            destinatario.setMotivoTraslado(destinoRequest.getMotivoTraslado());
            destinatario.setDocAduaneroUnico(destinoRequest.getDocAduaneroUnico());
            destinatario.setCodEstabDestino(destinoRequest.getCodEstabDestino());
            destinatario.setRuta(destinoRequest.getRuta());
            destinatario.setCodDocSustento(destinoRequest.getCodDocSustento());
            destinatario.setNumDocSustento(destinoRequest.getNumDocSustento());
            destinatario.setNumAutDocSustento(destinoRequest.getNumAutDocSustento());
            destinatario.setFechaEmisionDocSustento(comun.convertirFecha(destinoRequest.getFechaEmisionDocSustento()));
            destinatarios.add(destinatario);

            for (DetalleGuiaRemisionRequest detalleRequest : destinoRequest.getDetalles()) {
                DetalleComprobante detalle = new DetalleComprobante();
                detalle.setId(UUID.randomUUID());
                detalle.setItemId(detalleRequest.getItemId());
                detalle.setCodigoPrincipal(detalleRequest.getCodigoPrincipal());
                detalle.setDescripcion(detalleRequest.getDescripcion());
                detalle.setCantidad(detalleRequest.getCantidad());
                detalle.setPrecioUnitario(BigDecimal.ZERO);
                detalle.setDescuento(BigDecimal.ZERO);
                detalle.setImpuestos(List.of());
                detalle.setDestinatarioId(destinatario.getId());
                detalles.add(detalle);
            }
        }

        comprobante.setDestinatarios(destinatarios);
        comprobante.setDetalles(detalles);
        comprobante.setSubtotalSinImpuestos(BigDecimal.ZERO);
        comprobante.setTotalDescuento(BigDecimal.ZERO);
        comprobante.setImporteTotal(BigDecimal.ZERO);
        comprobante.setMoneda("DOLAR");
        comprobante.setDirPartida(request.getDirPartida());
        comprobante.setTransportistaRazonSocial(request.getTransportistaRazonSocial());
        comprobante.setTransportistaTipoIdentificacion(request.getTransportistaTipoIdentificacion());
        comprobante.setTransportistaRuc(request.getTransportistaRuc());
        comprobante.setFechaIniTransporte(comun.convertirFecha(request.getFechaIniTransporte()));
        comprobante.setFechaFinTransporte(comun.convertirFecha(request.getFechaFinTransporte()));
        comprobante.setPlaca(request.getPlaca());
        comprobante.setCamposAdicionales(comun.armarCamposAdicionales(request.getCampoAdicional()));
        return Uni.createFrom().item(comprobante);
    }

    /**
     * El SRI toma la fecha de inicio del transporte como la fecha de emisión de la guía y la compara con la de la clave
     * de acceso: si no es la misma, la rechaza en la recepción. Se avisa aquí, antes de gastar un secuencial.
     */
    private void validarInicioDelTransporte(CrearGuiaRemisionRequest request, Instant ahora) {
        LocalDate emision = FechasSri.convertirAFecha(ahora);
        LocalDate inicio = FechasSri.convertirAFecha(comun.convertirFecha(request.getFechaIniTransporte()));
        if (!emision.equals(inicio)) {
            throw new ErrorNegocioException(CodigoError.GUIA_INICIO_TRANSPORTE_NO_ES_LA_EMISION,
                FechasSri.formatearFecha(inicio), FechasSri.formatearFecha(emision));
        }
    }
}
