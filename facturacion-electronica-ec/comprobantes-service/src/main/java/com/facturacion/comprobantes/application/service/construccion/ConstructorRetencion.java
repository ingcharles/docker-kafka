package com.facturacion.comprobantes.application.service.construccion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.dto.CrearRetencionRequest;
import com.facturacion.comprobantes.application.dto.DocumentoSustentoRequest;
import com.facturacion.comprobantes.application.dto.RetencionLineaRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.DocumentoSustento;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.model.vo.ImpuestoDocSustento;
import com.facturacion.comprobantes.domain.service.CatalogosValidador;
import com.facturacion.comprobantes.domain.service.IdentificacionValidador;
import com.facturacion.comprobantes.domain.service.RetencionValidador;

/** Arma un comprobante de retención (ver {@link ConstructorComprobante}). */
@ApplicationScoped
public class ConstructorRetencion implements ConstructorComprobante<CrearRetencionRequest> {

    private final ConstructorComun comun;

    @Inject
    public ConstructorRetencion(ConstructorComun comun) {
        this.comun = comun;
    }

    /**
     * XSD v2.0.0 (estilo ATS): cada {@link DocumentoSustentoRequest} se
     * traduce en un {@code docSustento} con su propio impuesto y forma de
     * pago (ver {@code GeneradorXmlRetencion}). {@code detalles} se sigue
     * llenando igual que en la v1.0.0 (una fila por línea de retención) solo
     * para que el RIDE y el listado de comprobantes no necesiten cambios —
     * {@code documentosSustento} es lo que realmente usa el generador XML.
     */
    @Override
    public Uni<Comprobante> construir(CrearRetencionRequest request, long secuencial, CatalogosSriAFecha catalogos) {
        Instant ahora = Instant.now();
        Comprobante comprobante = comun.armarEncabezado(request.getEmpresa(), TipoComprobante.RETENCION, secuencial,
            ahora);

        List<DetalleComprobante> detalles = new ArrayList<>();
        List<DocumentoSustento> documentosSustento = new ArrayList<>();
        BigDecimal totalBaseImponible = BigDecimal.ZERO;
        BigDecimal totalRetenido = BigDecimal.ZERO;
        int contadorLinea = 0;

        for (DocumentoSustentoRequest docRequest : request.getDocumentosSustento()) {
            comun.validarFechaDocSustento(docRequest.getDocSustentoFecha());
            CatalogosValidador.validarSustento(catalogos, docRequest.getCodSustento());
            List<Impuesto> retenciones = new ArrayList<>();
            for (RetencionLineaRequest lineaRequest : docRequest.getRetenciones()) {
                CatalogosValidador.validarRetencion(catalogos, lineaRequest.getCodigo(),
                    lineaRequest.getCodigoRetencion(), lineaRequest.getPorcentajeRetener());
                RetencionValidador.validarValorRetenido(lineaRequest.getBaseImponible(),
                    lineaRequest.getPorcentajeRetener(), lineaRequest.getValorRetenido(),
                    "retencion " + lineaRequest.getCodigoRetencion() + " del documento " + docRequest.getDocSustentoNumero());
                contadorLinea++;
                Impuesto impuesto = new Impuesto(
                    lineaRequest.getCodigo(),
                    null,
                    lineaRequest.getPorcentajeRetener(),
                    lineaRequest.getBaseImponible(),
                    lineaRequest.getValorRetenido(),
                    lineaRequest.getCodigoRetencion(),
                    docRequest.getDocSustentoCodigo(),
                    docRequest.getDocSustentoNumero(),
                    docRequest.getDocSustentoFecha());
                retenciones.add(impuesto);

                DetalleComprobante detalle = new DetalleComprobante();
                detalle.setId(UUID.randomUUID());
                detalle.setCodigoPrincipal("RET-" + contadorLinea);
                detalle.setDescripcion("Retención " + lineaRequest.getCodigoRetencion() + " sobre "
                    + docRequest.getDocSustentoNumero());
                detalle.setCantidad(BigDecimal.ONE);
                detalle.setPrecioUnitario(lineaRequest.getBaseImponible());
                detalle.setDescuento(BigDecimal.ZERO);
                detalle.setImpuestos(List.of(impuesto));
                detalles.add(detalle);

                totalBaseImponible = totalBaseImponible.add(lineaRequest.getBaseImponible());
                totalRetenido = totalRetenido.add(lineaRequest.getValorRetenido());
            }

            List<ImpuestoDocSustento> impuestosDocSustento = docRequest.getImpuestos().stream()
                .map(i -> new ImpuestoDocSustento(i.getCodigo(), i.getCodigoPorcentaje(), i.getBaseImponible(),
                    i.getTarifa(), i.getValor()))
                .collect(Collectors.toList());

            documentosSustento.add(new DocumentoSustento(
                docRequest.getCodSustento(),
                docRequest.getDocSustentoCodigo(),
                docRequest.getDocSustentoNumero(),
                docRequest.getDocSustentoFecha(),
                docRequest.getPagoLocExt(),
                docRequest.getTotalSinImpuestos(),
                docRequest.getImporteTotal(),
                impuestosDocSustento,
                retenciones,
                comun.armarPagos(docRequest.getPagos(), docRequest.getImporteTotal(), false, catalogos)));
        }

        CatalogosValidador.validarTipoIdentificacion(catalogos, request.getSujetoRetenidoTipoIdentificacion(),
            TipoComprobante.RETENCION, "sujetoRetenidoTipoIdentificacion");
        IdentificacionValidador.validar(request.getSujetoRetenidoTipoIdentificacion(), request.getSujetoRetenidoIdentificacion(),
            "sujetoRetenidoTipoIdentificacion");
        comprobante.setCompradorTipoIdentificacion(request.getSujetoRetenidoTipoIdentificacion());
        comprobante.setCompradorIdentificacion(request.getSujetoRetenidoIdentificacion());
        comprobante.setCompradorRazonSocial(request.getSujetoRetenidoRazonSocial());
        comprobante.setCompradorCorreo(request.getSujetoRetenidoCorreo());
        comprobante.setMoneda("DOLAR");
        comprobante.setDetalles(detalles);
        comprobante.setDocumentosSustento(documentosSustento);
        comprobante.setSubtotalSinImpuestos(totalBaseImponible);
        comprobante.setTotalDescuento(BigDecimal.ZERO);
        comprobante.setImporteTotal(totalRetenido);
        comprobante.setPeriodoFiscal(request.getPeriodoFiscal());
        comprobante.setParteRelacionada(
            request.getParteRelacionada() == null || request.getParteRelacionada().isBlank()
                ? "NO"
                : request.getParteRelacionada());
        comprobante.setCamposAdicionales(comun.armarCamposAdicionales(request.getCampoAdicional()));
        return Uni.createFrom().item(comprobante);
    }
}
