package com.facturacion.comprobantes.application.service.construccion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.dto.CrearFacturaRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.service.CatalogosValidador;
import com.facturacion.comprobantes.domain.service.IdentificacionValidador;
import com.facturacion.comprobantes.domain.service.PagosValidador;

/** Arma una factura (ver {@link ConstructorComprobante}). */
@ApplicationScoped
public class ConstructorFactura implements ConstructorComprobante<CrearFacturaRequest> {

    private final ConstructorComun comun;
    private final ConstructorDetalles constructorDetalles;
    private final BigDecimal montoMaximoConsumidorFinal;

    @Inject
    public ConstructorFactura(ConstructorComun comun, ConstructorDetalles constructorDetalles,
            @ConfigProperty(name = "comprobantes.consumidor-final.monto-maximo", defaultValue = "50.00")
            BigDecimal montoMaximoConsumidorFinal) {
        this.comun = comun;
        this.constructorDetalles = constructorDetalles;
        this.montoMaximoConsumidorFinal = montoMaximoConsumidorFinal;
    }

    @Override
    public Uni<Comprobante> construir(CrearFacturaRequest request, long secuencial, CatalogosSriAFecha catalogos) {
        Instant ahora = Instant.now();
        Comprobante comprobante = comun.encabezado(request.getEmpresa(), TipoComprobante.FACTURA, secuencial,
            ahora);

        return constructorDetalles.cargarItems(request.getDetalles(), request.getEmpresa().getRuc())
            .map(itemsPorId -> {
                List<DetalleComprobante> detalles = request.getDetalles().stream()
                    .map(det -> constructorDetalles.construir(det, itemsPorId, catalogos))
                    .collect(Collectors.toList());
                constructorDetalles.aplicarTotales(comprobante, detalles);

                CatalogosValidador.validarTipoIdentificacion(catalogos, request.getCompradorTipoIdentificacion(),
                    TipoComprobante.FACTURA, "compradorTipoIdentificacion");
                IdentificacionValidador.validar(request.getCompradorTipoIdentificacion(), request.getCompradorIdentificacion(),
                    "compradorTipoIdentificacion");
                IdentificacionValidador.validarMontoConsumidorFinal(request.getCompradorTipoIdentificacion(),
                    comprobante.getImporteTotal(), montoMaximoConsumidorFinal, "compradorTipoIdentificacion");
                comprobante.setCompradorTipoIdentificacion(request.getCompradorTipoIdentificacion());
                comprobante.setCompradorIdentificacion(request.getCompradorIdentificacion());
                comprobante.setCompradorRazonSocial(request.getCompradorRazonSocial());
                comprobante.setCompradorDireccion(request.getCompradorDireccion());
                comprobante.setCompradorCorreo(request.getCompradorCorreo());
                comprobante.setMoneda(request.getMoneda());
                comprobante.setDetalles(detalles);
                comprobante.setPagos(comun.pagos(request.getPagos(), comprobante.getImporteTotal(), true, catalogos));
                PagosValidador.validarFacturaNegociable(comprobante.getPagos(), comprobante.getCompradorDireccion());
                comprobante.setCamposAdicionales(comun.camposAdicionales(request.getCampoAdicional()));
                return comprobante;
            });
    }
}
