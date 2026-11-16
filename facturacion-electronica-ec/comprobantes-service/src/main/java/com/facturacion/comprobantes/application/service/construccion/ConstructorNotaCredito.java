package com.facturacion.comprobantes.application.service.construccion;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.dto.CrearNotaCreditoRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.service.CatalogosValidador;
import com.facturacion.comprobantes.domain.service.IdentificacionValidador;

/** Arma una nota de crédito (ver {@link ConstructorComprobante}). */
@ApplicationScoped
public class ConstructorNotaCredito implements ConstructorComprobante<CrearNotaCreditoRequest> {

    private final ConstructorComun comun;
    private final ConstructorDetalles constructorDetalles;

    @Inject
    public ConstructorNotaCredito(ConstructorComun comun, ConstructorDetalles constructorDetalles) {
        this.comun = comun;
        this.constructorDetalles = constructorDetalles;
    }

    @Override
    public Uni<Comprobante> construir(CrearNotaCreditoRequest request, long secuencial, CatalogosSriAFecha catalogos) {
        Instant ahora = Instant.now();
        Comprobante comprobante = comun.encabezado(request.getEmpresa(), TipoComprobante.NOTA_CREDITO, secuencial,
            ahora);

        return constructorDetalles.cargarItems(request.getDetalles(), request.getEmpresa().getRuc())
            .map(itemsPorId -> {
                List<DetalleComprobante> detalles = request.getDetalles().stream()
                    .map(det -> constructorDetalles.construir(det, itemsPorId, catalogos))
                    .collect(Collectors.toList());
                constructorDetalles.aplicarTotales(comprobante, detalles);

                CatalogosValidador.validarTipoIdentificacion(catalogos, request.getCompradorTipoIdentificacion(),
                    TipoComprobante.NOTA_CREDITO, "compradorTipoIdentificacion");
                IdentificacionValidador.validar(request.getCompradorTipoIdentificacion(), request.getCompradorIdentificacion(),
                    "compradorTipoIdentificacion");
                comprobante.setCompradorTipoIdentificacion(request.getCompradorTipoIdentificacion());
                comprobante.setCompradorIdentificacion(request.getCompradorIdentificacion());
                comprobante.setCompradorRazonSocial(request.getCompradorRazonSocial());
                comprobante.setCompradorDireccion(request.getCompradorDireccion());
                comprobante.setCompradorCorreo(request.getCompradorCorreo());
                comprobante.setMoneda(request.getMoneda());
                comprobante.setDetalles(detalles);
                comprobante.setDocModificadoTipo(request.getDocModificadoTipo());
                comprobante.setDocModificadoNumero(request.getDocModificadoNumero());
                comprobante.setDocModificadoFecha(comun.convertirFecha(request.getDocModificadoFecha()));
                comprobante.setMotivo(request.getMotivo());
                comprobante.setCamposAdicionales(comun.camposAdicionales(request.getCampoAdicional()));
                return comprobante;
            });
    }
}
