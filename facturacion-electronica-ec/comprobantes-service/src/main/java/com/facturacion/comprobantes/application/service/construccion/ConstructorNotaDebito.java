package com.facturacion.comprobantes.application.service.construccion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.application.dto.CrearNotaDebitoRequest;
import com.facturacion.comprobantes.application.dto.MotivoNotaDebitoRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.service.CatalogosValidador;
import com.facturacion.comprobantes.domain.service.DocumentoModificadoValidador;
import com.facturacion.comprobantes.domain.service.DocumentoModificadoValidador.NumeroDocumento;
import com.facturacion.comprobantes.domain.service.IdentificacionValidador;

/**
 * Arma una nota de débito (ver {@link ConstructorComprobante}) y la comprueba contra el documento que modifica (ver
 * {@link DocumentoModificadoValidador}). A diferencia de la de crédito, no tiene tope de valor: agrega cargos.
 */
@ApplicationScoped
public class ConstructorNotaDebito implements ConstructorComprobante<CrearNotaDebitoRequest> {

    private final ConstructorComun comun;
    private final BuscadorDocumentoModificado buscadorDocumento;

    @Inject
    public ConstructorNotaDebito(ConstructorComun comun, BuscadorDocumentoModificado buscadorDocumento) {
        this.comun = comun;
        this.buscadorDocumento = buscadorDocumento;
    }

    @Override
    public Uni<Comprobante> construir(CrearNotaDebitoRequest request, long secuencial, CatalogosSriAFecha catalogos) {
        // Lo que se puede comprobar sin la base va primero: un número o una fecha mal puestos fallan sin consultar nada.
        NumeroDocumento numeroDocumento = DocumentoModificadoValidador.interpretarNumero(request.getDocModificadoNumero());
        LocalDate fechaDocumento = FechasSri.convertirAFecha(comun.convertirFecha(request.getDocModificadoFecha()));
        DocumentoModificadoValidador.validarFecha(fechaDocumento, FechasSri.obtenerHoy());

        Instant ahora = Instant.now();
        Comprobante comprobante = comun.armarEncabezado(request.getEmpresa(), TipoComprobante.NOTA_DEBITO, secuencial,
            ahora);

        BigDecimal totalMotivos = request.getMotivos().stream()
            .map(MotivoNotaDebitoRequest::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // El bloque <impuestos> de Nota de Débito es único a nivel de
        // encabezado (no por motivo) — se calcula sobre la suma de los
        // motivos y se guarda solo en la primera línea, así el generador de
        // XML reutiliza la misma lógica de agrupación que Factura sin
        // cambios (ver XmlComprobanteUtil.agruparImpuestos).
        List<Impuesto> impuestosEncabezado = request.getImpuestos().stream()
            .map(impRequest -> {
                BigDecimal tarifa = CatalogosValidador.resolverTarifa(catalogos, impRequest.getCodigo(),
                    impRequest.getCodigoPorcentaje(), impRequest.getTarifa());
                return new Impuesto(
                    impRequest.getCodigo(),
                    impRequest.getCodigoPorcentaje(),
                    tarifa,
                    totalMotivos,
                    totalMotivos.multiply(tarifa).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            })
            .collect(Collectors.toList());
        BigDecimal totalImpuestos = impuestosEncabezado.stream().map(Impuesto::valor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DetalleComprobante> detalles = new ArrayList<>();
        for (int i = 0; i < request.getMotivos().size(); i++) {
            MotivoNotaDebitoRequest motivoRequest = request.getMotivos().get(i);
            DetalleComprobante detalle = new DetalleComprobante();
            detalle.setId(UUID.randomUUID());
            detalle.setCodigoPrincipal("MOTIVO-" + (i + 1));
            detalle.setDescripcion(motivoRequest.getRazon());
            detalle.setCantidad(BigDecimal.ONE);
            detalle.setPrecioUnitario(motivoRequest.getValor());
            detalle.setDescuento(BigDecimal.ZERO);
            detalle.setImpuestos(i == 0 ? impuestosEncabezado : List.of());
            detalles.add(detalle);
        }

        CatalogosValidador.validarTipoIdentificacion(catalogos, request.getCompradorTipoIdentificacion(),
            TipoComprobante.NOTA_DEBITO, "compradorTipoIdentificacion");
        IdentificacionValidador.validar(request.getCompradorTipoIdentificacion(), request.getCompradorIdentificacion(),
            "compradorTipoIdentificacion");
        comprobante.setCompradorTipoIdentificacion(request.getCompradorTipoIdentificacion());
        comprobante.setCompradorIdentificacion(request.getCompradorIdentificacion());
        comprobante.setCompradorRazonSocial(request.getCompradorRazonSocial());
        comprobante.setCompradorDireccion(request.getCompradorDireccion());
        comprobante.setCompradorCorreo(request.getCompradorCorreo());
        comprobante.setMoneda(request.getMoneda());
        comprobante.setDetalles(detalles);
        comprobante.setSubtotalSinImpuestos(totalMotivos);
        comprobante.setTotalDescuento(BigDecimal.ZERO);
        comprobante.setImporteTotal(totalMotivos.add(totalImpuestos));
        comprobante.setDocModificadoTipo(request.getDocModificadoTipo());
        comprobante.setDocModificadoNumero(request.getDocModificadoNumero());
        comprobante.setDocModificadoFecha(comun.convertirFecha(request.getDocModificadoFecha()));
        comprobante.setPagos(comun.armarPagos(request.getPagos(), comprobante.getImporteTotal(), true, catalogos));
        comprobante.setCamposAdicionales(comun.armarCamposAdicionales(request.getCampoAdicional()));
        return comprobarContraElDocumento(request, numeroDocumento, fechaDocumento).replaceWith(comprobante);
    }

    /** Si el documento lo emitió este sistema, comprueba estado y fecha; si no, no hay nada que comprobar. */
    private Uni<Void> comprobarContraElDocumento(CrearNotaDebitoRequest request, NumeroDocumento numero,
            LocalDate fechaIndicada) {
        return buscadorDocumento.buscar(request.getEmpresa().getRuc(), request.getDocModificadoTipo(), numero)
            .onItem().ifNotNull().invoke(original -> DocumentoModificadoValidador.validarContraDocumento(original,
                request.getDocModificadoNumero(), fechaIndicada))
            .replaceWithVoid();
    }
}
