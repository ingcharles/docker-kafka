package com.facturacion.comprobantes.application.service.construccion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.application.dto.CrearNotaCreditoRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.domain.service.CatalogosValidador;
import com.facturacion.comprobantes.domain.service.DocumentoModificadoValidador;
import com.facturacion.comprobantes.domain.service.DocumentoModificadoValidador.NumeroDocumento;
import com.facturacion.comprobantes.domain.service.IdentificacionValidador;
import com.facturacion.comprobantes.domain.service.NotaCreditoValidador;

/**
 * Arma una nota de crédito (ver {@link ConstructorComprobante}) y la comprueba contra el documento que modifica (ver
 * {@link DocumentoModificadoValidador} y {@link NotaCreditoValidador}).
 */
@ApplicationScoped
public class ConstructorNotaCredito implements ConstructorComprobante<CrearNotaCreditoRequest> {

    private final ConstructorComun comun;
    private final ConstructorDetalles constructorDetalles;
    private final BuscadorDocumentoModificado buscadorDocumento;
    private final ComprobanteRepository comprobanteRepository;

    @Inject
    public ConstructorNotaCredito(ConstructorComun comun, ConstructorDetalles constructorDetalles,
            BuscadorDocumentoModificado buscadorDocumento, ComprobanteRepository comprobanteRepository) {
        this.comun = comun;
        this.constructorDetalles = constructorDetalles;
        this.buscadorDocumento = buscadorDocumento;
        this.comprobanteRepository = comprobanteRepository;
    }

    @Override
    public Uni<Comprobante> construir(CrearNotaCreditoRequest request, long secuencial, CatalogosSriAFecha catalogos) {
        // Lo que se puede comprobar sin la base va primero: un número o una fecha mal puestos fallan sin consultar nada.
        NumeroDocumento numeroDocumento = DocumentoModificadoValidador.interpretarNumero(request.getDocModificadoNumero());
        LocalDate fechaDocumento = FechasSri.convertirAFecha(comun.convertirFecha(request.getDocModificadoFecha()));
        DocumentoModificadoValidador.validarFecha(fechaDocumento, FechasSri.obtenerHoy());

        Instant ahora = Instant.now();
        Comprobante comprobante = comun.armarEncabezado(request.getEmpresa(), TipoComprobante.NOTA_CREDITO, secuencial,
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
                comprobante.setCamposAdicionales(comun.armarCamposAdicionales(request.getCampoAdicional()));
                return comprobante;
            })
            .chain(armado -> comprobarContraElDocumento(request, numeroDocumento, fechaDocumento, armado)
                .replaceWith(armado));
    }

    /** Si el documento lo emitió este sistema, comprueba estado, fecha y valor; si no, no hay nada que comprobar. */
    private Uni<Void> comprobarContraElDocumento(CrearNotaCreditoRequest request, NumeroDocumento numero,
            LocalDate fechaIndicada, Comprobante nota) {
        return buscadorDocumento.buscar(request.getEmpresa().getRuc(), request.getDocModificadoTipo(), numero)
            .onItem().ifNotNull().transformToUni(original -> comprobanteRepository
                .sumarNotasCreditoVigentes(request.getEmpresa().getRuc(), request.getDocModificadoTipo(),
                    request.getDocModificadoNumero())
                .invoke(yaEmitido -> NotaCreditoValidador.validarContraDocumento(original,
                    request.getDocModificadoNumero(), fechaIndicada, nota.getImporteTotal(), yaEmitido)))
            .replaceWithVoid();
    }
}
