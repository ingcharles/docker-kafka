package com.facturacion.comprobantes.application.service.construccion;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.dto.CrearLiquidacionCompraRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.service.CatalogosValidador;
import com.facturacion.comprobantes.domain.service.IdentificacionValidador;

/** Arma una liquidación de compra (ver {@link ConstructorComprobante}). */
@ApplicationScoped
public class ConstructorLiquidacionCompra implements ConstructorComprobante<CrearLiquidacionCompraRequest> {

    private final ConstructorComun comun;
    private final ConstructorDetalles constructorDetalles;

    @Inject
    public ConstructorLiquidacionCompra(ConstructorComun comun, ConstructorDetalles constructorDetalles) {
        this.comun = comun;
        this.constructorDetalles = constructorDetalles;
    }

    @Override
    public Uni<Comprobante> construir(CrearLiquidacionCompraRequest request, long secuencial, CatalogosSriAFecha catalogos) {
        Instant ahora = Instant.now();
        Comprobante comprobante = comun.encabezado(request.getEmpresa(), TipoComprobante.LIQUIDACION_COMPRA,
            secuencial, ahora);

        return constructorDetalles.cargarItems(request.getDetalles(), request.getEmpresa().getRuc())
            .map(itemsPorId -> {
                List<DetalleComprobante> detalles = request.getDetalles().stream()
                    .map(det -> constructorDetalles.construir(det, itemsPorId, catalogos))
                    .collect(Collectors.toList());
                constructorDetalles.aplicarTotales(comprobante, detalles);

                // El proveedor se guarda en las mismas columnas comprador_* — aquí
                // representa a quien vende, no a quien compra, pero es el mismo dato
                // desde el punto de vista del agregado (ver clase de la request).
                CatalogosValidador.validarTipoIdentificacion(catalogos, request.getProveedorTipoIdentificacion(),
                    TipoComprobante.LIQUIDACION_COMPRA, "proveedorTipoIdentificacion");
                IdentificacionValidador.validar(request.getProveedorTipoIdentificacion(), request.getProveedorIdentificacion(),
                    "proveedorTipoIdentificacion");
                comprobante.setCompradorTipoIdentificacion(request.getProveedorTipoIdentificacion());
                comprobante.setCompradorIdentificacion(request.getProveedorIdentificacion());
                comprobante.setCompradorRazonSocial(request.getProveedorRazonSocial());
                comprobante.setCompradorDireccion(request.getProveedorDireccion());
                comprobante.setCompradorCorreo(request.getProveedorCorreo());
                comprobante.setMoneda(request.getMoneda());
                comprobante.setDetalles(detalles);
                comprobante.setPagos(comun.pagos(request.getPagos(), comprobante.getImporteTotal(), true, catalogos));
                comprobante.setCamposAdicionales(comun.camposAdicionales(request.getCampoAdicional()));
                return comprobante;
            });
    }
}
