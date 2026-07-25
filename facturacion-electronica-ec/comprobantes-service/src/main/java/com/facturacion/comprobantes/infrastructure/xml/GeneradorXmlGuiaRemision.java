package com.facturacion.comprobantes.infrastructure.xml;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DestinatarioGuiaRemision;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.service.GeneradorXmlComprobante;

/**
 * Genera el XML de Guía de Remisión conforme a la ficha técnica del SRI
 * (versión 1.0.0). No es una venta (no lleva comprador ni valores
 * monetarios) — traslada bienes de un punto a otro, con uno o más
 * destinatarios, cada uno con su propia lista de ítems trasladados (ver
 * {@link DetalleComprobante#getDestinatarioId()}).
 */
@ApplicationScoped
public class GeneradorXmlGuiaRemision implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "1.0.0";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean soporta(TipoComprobante tipo) {
        return tipo == TipoComprobante.GUIA_REMISION;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.nuevoDocumento();

            Element raiz = doc.createElement("guiaRemision");
            raiz.setAttribute("id", "comprobante");
            raiz.setAttribute("version", VERSION_XSD);
            doc.appendChild(raiz);

            raiz.appendChild(construirInfoTributaria(doc, c));
            raiz.appendChild(construirInfoGuiaRemision(doc, c));
            raiz.appendChild(construirDestinatarios(doc, c));
            if (c.isEmisorEsGranContribuyente()) {
                raiz.appendChild(XmlComprobanteUtil.construirInfoAdicionalGranContribuyente(doc, c));
            }

            return XmlComprobanteUtil.serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el XML de la guía de remisión " + c.getId(), e);
        }
    }

    private Element construirInfoTributaria(Document doc, Comprobante c) {
        Element info = doc.createElement("infoTributaria");
        XmlComprobanteUtil.texto(doc, info, "ambiente", c.getAmbiente().codigo());
        XmlComprobanteUtil.texto(doc, info, "tipoEmision", "1");
        XmlComprobanteUtil.texto(doc, info, "razonSocial", c.getEmisorRazonSocial());
        if (c.getEmisorNombreComercial() != null && !c.getEmisorNombreComercial().isBlank()) {
            XmlComprobanteUtil.texto(doc, info, "nombreComercial", c.getEmisorNombreComercial());
        }
        XmlComprobanteUtil.texto(doc, info, "ruc", c.getEmisorRuc());
        XmlComprobanteUtil.texto(doc, info, "claveAcceso", c.getClaveAcceso());
        XmlComprobanteUtil.texto(doc, info, "codDoc", TipoComprobante.GUIA_REMISION.codigo());
        XmlComprobanteUtil.texto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.texto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.texto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.texto(doc, info, "dirMatriz", c.getEmisorDireccionMatriz());
        return info;
    }

    private Element construirInfoGuiaRemision(Document doc, Comprobante c) {
        Element info = doc.createElement("infoGuiaRemision");
        XmlComprobanteUtil.texto(doc, info, "dirEstablecimiento", XmlComprobanteUtil.direccionEstablecimiento(c));
        XmlComprobanteUtil.texto(doc, info, "dirPartida", c.getDirPartida());
        XmlComprobanteUtil.texto(doc, info, "razonSocialTransportista", c.getTransportistaRazonSocial());
        XmlComprobanteUtil.texto(doc, info, "tipoIdentificacionTransportista", c.getTransportistaTipoIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "rucTransportista", c.getTransportistaRuc());
        XmlComprobanteUtil.texto(doc, info, "obligadoContabilidad", c.isEmisorObligadoContabilidad() ? "SI" : "NO");
        if (c.isEmisorContribuyenteEspecial() && c.getEmisorNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.texto(doc, info, "contribuyenteEspecial", c.getEmisorNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.texto(doc, info, "fechaIniTransporte",
            FORMATO_FECHA.format(c.getFechaIniTransporte().atZone(ZoneOffset.of("-05:00"))));
        XmlComprobanteUtil.texto(doc, info, "fechaFinTransporte",
            FORMATO_FECHA.format(c.getFechaFinTransporte().atZone(ZoneOffset.of("-05:00"))));
        // Obligatorio siempre en Guía de Remisión (confirmado con un
        // rechazo real de Recepción) — a diferencia de Factura, donde solo
        // es obligatorio para emisores que venden combustibles.
        XmlComprobanteUtil.texto(doc, info, "placa", c.getPlaca());
        return info;
    }

    private Element construirDestinatarios(Document doc, Comprobante c) {
        Map<java.util.UUID, List<DetalleComprobante>> detallesPorDestinatario = c.getDetalles().stream()
            .collect(Collectors.groupingBy(DetalleComprobante::getDestinatarioId));

        Element destinatarios = doc.createElement("destinatarios");
        for (DestinatarioGuiaRemision destino : c.getDestinatarios()) {
            Element destinatario = doc.createElement("destinatario");
            XmlComprobanteUtil.texto(doc, destinatario, "identificacionDestinatario", destino.getIdentificacion());
            XmlComprobanteUtil.texto(doc, destinatario, "razonSocialDestinatario", destino.getRazonSocial());
            XmlComprobanteUtil.texto(doc, destinatario, "dirDestinatario", destino.getDireccion());
            XmlComprobanteUtil.texto(doc, destinatario, "motivoTraslado", destino.getMotivoTraslado());
            if (destino.getDocAduaneroUnico() != null && !destino.getDocAduaneroUnico().isBlank()) {
                XmlComprobanteUtil.texto(doc, destinatario, "docAduaneroUnico", destino.getDocAduaneroUnico());
            }
            if (destino.getCodEstabDestino() != null && !destino.getCodEstabDestino().isBlank()) {
                XmlComprobanteUtil.texto(doc, destinatario, "codEstabDestino", destino.getCodEstabDestino());
            }
            if (destino.getRuta() != null && !destino.getRuta().isBlank()) {
                XmlComprobanteUtil.texto(doc, destinatario, "ruta", destino.getRuta());
            }
            if (destino.getCodDocSustento() != null && !destino.getCodDocSustento().isBlank()) {
                XmlComprobanteUtil.texto(doc, destinatario, "codDocSustento", destino.getCodDocSustento());
            }
            if (destino.getNumDocSustento() != null && !destino.getNumDocSustento().isBlank()) {
                XmlComprobanteUtil.texto(doc, destinatario, "numDocSustento", destino.getNumDocSustento());
            }
            if (destino.getNumAutDocSustento() != null && !destino.getNumAutDocSustento().isBlank()) {
                XmlComprobanteUtil.texto(doc, destinatario, "numAutDocSustento", destino.getNumAutDocSustento());
            }
            if (destino.getFechaEmisionDocSustento() != null) {
                XmlComprobanteUtil.texto(doc, destinatario, "fechaEmisionDocSustento",
                    FORMATO_FECHA.format(destino.getFechaEmisionDocSustento().atZone(ZoneOffset.of("-05:00"))));
            }

            Element detalles = doc.createElement("detalles");
            for (DetalleComprobante d : detallesPorDestinatario.getOrDefault(destino.getId(), List.of())) {
                Element detalle = doc.createElement("detalle");
                XmlComprobanteUtil.texto(doc, detalle, "codigoInterno", d.getCodigoPrincipal());
                XmlComprobanteUtil.texto(doc, detalle, "descripcion", d.getDescripcion());
                XmlComprobanteUtil.texto(doc, detalle, "cantidad", d.getCantidad().toPlainString());
                detalles.appendChild(detalle);
            }
            destinatario.appendChild(detalles);

            destinatarios.appendChild(destinatario);
        }
        return destinatarios;
    }
}
