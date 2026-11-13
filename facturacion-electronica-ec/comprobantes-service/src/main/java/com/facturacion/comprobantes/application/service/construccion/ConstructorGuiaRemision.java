package com.facturacion.comprobantes.application.service.construccion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.dto.CrearGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.DestinatarioGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.DetalleGuiaRemisionRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DestinatarioGuiaRemision;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;

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
        Comprobante comprobante = comun.encabezado(request.getEmpresa(), TipoComprobante.GUIA_REMISION, secuencial,
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
            destinatario.setFechaEmisionDocSustento(comun.parseFecha(destinoRequest.getFechaEmisionDocSustento()));
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
        comprobante.setFechaIniTransporte(comun.parseFecha(request.getFechaIniTransporte()));
        comprobante.setFechaFinTransporte(comun.parseFecha(request.getFechaFinTransporte()));
        comprobante.setPlaca(request.getPlaca());
        comprobante.setCamposAdicionales(comun.camposAdicionales(request.getCampoAdicional()));
        return Uni.createFrom().item(comprobante);
    }
}
