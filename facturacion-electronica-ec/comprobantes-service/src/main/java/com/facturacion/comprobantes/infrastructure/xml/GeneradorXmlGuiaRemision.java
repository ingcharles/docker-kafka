package com.facturacion.comprobantes.infrastructure.xml;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DestinatarioGuiaRemision;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.service.GeneradorXmlComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Genera el XML de Guía de Remisión conforme a la ficha técnica del SRI
 * (versión 1.1.0, la última disponible — ver {@code xsd/GuiaRemision_V1.1.0.xsd}).
 * No es una venta (no lleva comprador ni valores
 * monetarios) — traslada bienes de un punto a otro, con uno o más
 * destinatarios, cada uno con su propia lista de ítems trasladados (ver
 * {@link DetalleComprobante#getDestinatarioId()}).
 */
@ApplicationScoped
public class GeneradorXmlGuiaRemision implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "1.1.0";

    @Override
    public boolean esTipoSoportado(TipoComprobante tipo) {
        return tipo == TipoComprobante.GUIA_REMISION;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.crearDocumento();

            Element raiz = doc.createElement("guiaRemision");
            raiz.setAttribute("id", "comprobante");
            raiz.setAttribute("version", VERSION_XSD);
            doc.appendChild(raiz);

            raiz.appendChild(construirInfoTributaria(doc, c));
            raiz.appendChild(construirInfoGuiaRemision(doc, c));
            raiz.appendChild(construirDestinatarios(doc, c));
            Element infoAdicional = XmlComprobanteUtil.construirInfoAdicional(doc, c);
            if (infoAdicional != null) {
                raiz.appendChild(infoAdicional);
            }

            return XmlComprobanteUtil.serializar(doc);
        } catch (Exception e) {
            throw new ErrorInternoException(e, CodigoError.XML_NO_GENERADO, c.getTipoComprobante().nombre(), c.getId());
        }
    }

    private Element construirInfoTributaria(Document doc, Comprobante c) {
        Element info = doc.createElement("infoTributaria");
        XmlComprobanteUtil.agregarTexto(doc, info, "ambiente", c.getAmbiente().codigo());
        XmlComprobanteUtil.agregarTexto(doc, info, "tipoEmision", "1");
        XmlComprobanteUtil.agregarTexto(doc, info, "razonSocial", c.getEmpresaRazonSocial());
        if (c.getEmpresaNombreComercial() != null && !c.getEmpresaNombreComercial().isBlank()) {
            XmlComprobanteUtil.agregarTexto(doc, info, "nombreComercial", c.getEmpresaNombreComercial());
        }
        XmlComprobanteUtil.agregarTexto(doc, info, "ruc", c.getEmpresaRuc());
        XmlComprobanteUtil.agregarTexto(doc, info, "claveAcceso", c.getClaveAcceso());
        XmlComprobanteUtil.agregarTexto(doc, info, "codDoc", TipoComprobante.GUIA_REMISION.codigo());
        XmlComprobanteUtil.agregarTexto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.agregarTexto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.agregarTexto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.agregarTexto(doc, info, "dirMatriz", c.getEmpresaDireccionMatriz());
        XmlComprobanteUtil.agregarAgenteYRegimen(doc, info, c);
        return info;
    }

    private Element construirInfoGuiaRemision(Document doc, Comprobante c) {
        Element info = doc.createElement("infoGuiaRemision");
        XmlComprobanteUtil.agregarTexto(doc, info, "dirEstablecimiento", XmlComprobanteUtil.obtenerDireccionEstablecimiento(c));
        XmlComprobanteUtil.agregarTexto(doc, info, "dirPartida", c.getDirPartida());
        XmlComprobanteUtil.agregarTexto(doc, info, "razonSocialTransportista", c.getTransportistaRazonSocial());
        XmlComprobanteUtil.agregarTexto(doc, info, "tipoIdentificacionTransportista", c.getTransportistaTipoIdentificacion());
        XmlComprobanteUtil.agregarTexto(doc, info, "rucTransportista", c.getTransportistaRuc());
        XmlComprobanteUtil.agregarTexto(doc, info, "obligadoContabilidad", c.isEmpresaObligadoContabilidad() ? "SI" : "NO");
        if (c.isEmpresaContribuyenteEspecial() && c.getEmpresaNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.agregarTexto(doc, info, "contribuyenteEspecial", c.getEmpresaNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.agregarTexto(doc, info, "fechaIniTransporte",
            FechasSri.formatearFecha(c.getFechaIniTransporte()));
        XmlComprobanteUtil.agregarTexto(doc, info, "fechaFinTransporte",
            FechasSri.formatearFecha(c.getFechaFinTransporte()));
        // Obligatorio siempre en Guía de Remisión (confirmado con un
        // rechazo real de Recepción) — a diferencia de Factura, donde solo
        // es obligatorio para empresas que venden combustibles.
        XmlComprobanteUtil.agregarTexto(doc, info, "placa", c.getPlaca());
        return info;
    }

    private Element construirDestinatarios(Document doc, Comprobante c) {
        Map<UUID, List<DetalleComprobante>> detallesPorDestinatario = c.getDetalles().stream()
            .collect(Collectors.groupingBy(DetalleComprobante::getDestinatarioId));

        Element destinatarios = doc.createElement("destinatarios");
        for (DestinatarioGuiaRemision destino : c.getDestinatarios()) {
            Element destinatario = doc.createElement("destinatario");
            XmlComprobanteUtil.agregarTexto(doc, destinatario, "identificacionDestinatario", destino.getIdentificacion());
            XmlComprobanteUtil.agregarTexto(doc, destinatario, "razonSocialDestinatario", destino.getRazonSocial());
            XmlComprobanteUtil.agregarTexto(doc, destinatario, "dirDestinatario", destino.getDireccion());
            XmlComprobanteUtil.agregarTexto(doc, destinatario, "motivoTraslado", destino.getMotivoTraslado());
            if (destino.getDocAduaneroUnico() != null && !destino.getDocAduaneroUnico().isBlank()) {
                XmlComprobanteUtil.agregarTexto(doc, destinatario, "docAduaneroUnico", destino.getDocAduaneroUnico());
            }
            if (destino.getCodEstabDestino() != null && !destino.getCodEstabDestino().isBlank()) {
                XmlComprobanteUtil.agregarTexto(doc, destinatario, "codEstabDestino", destino.getCodEstabDestino());
            }
            if (destino.getRuta() != null && !destino.getRuta().isBlank()) {
                XmlComprobanteUtil.agregarTexto(doc, destinatario, "ruta", destino.getRuta());
            }
            if (destino.getCodDocSustento() != null && !destino.getCodDocSustento().isBlank()) {
                XmlComprobanteUtil.agregarTexto(doc, destinatario, "codDocSustento", destino.getCodDocSustento());
            }
            if (destino.getNumDocSustento() != null && !destino.getNumDocSustento().isBlank()) {
                XmlComprobanteUtil.agregarTexto(doc, destinatario, "numDocSustento", destino.getNumDocSustento());
            }
            if (destino.getNumAutDocSustento() != null && !destino.getNumAutDocSustento().isBlank()) {
                XmlComprobanteUtil.agregarTexto(doc, destinatario, "numAutDocSustento", destino.getNumAutDocSustento());
            }
            if (destino.getFechaEmisionDocSustento() != null) {
                XmlComprobanteUtil.agregarTexto(doc, destinatario, "fechaEmisionDocSustento",
                    FechasSri.formatearFecha(destino.getFechaEmisionDocSustento()));
            }

            Element detalles = doc.createElement("detalles");
            for (DetalleComprobante d : detallesPorDestinatario.getOrDefault(destino.getId(), List.of())) {
                Element detalle = doc.createElement("detalle");
                XmlComprobanteUtil.agregarTexto(doc, detalle, "codigoInterno", d.getCodigoPrincipal());
                XmlComprobanteUtil.agregarTexto(doc, detalle, "descripcion", d.getDescripcion());
                XmlComprobanteUtil.agregarTexto(doc, detalle, "cantidad", d.getCantidad().toPlainString());
                detalles.appendChild(detalle);
            }
            destinatario.appendChild(detalles);

            destinatarios.appendChild(destinatario);
        }
        return destinatarios;
    }
}
