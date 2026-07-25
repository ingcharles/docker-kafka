package com.facturacion.comprobantes.infrastructure.xml;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import jakarta.enterprise.context.ApplicationScoped;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.service.GeneradorXmlComprobante;

/** Genera el XML de Nota de Crédito conforme al Anexo de la ficha técnica del SRI (versión 1.0.0). */
@ApplicationScoped
public class GeneradorXmlNotaCredito implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "1.0.0";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean soporta(TipoComprobante tipo) {
        return tipo == TipoComprobante.NOTA_CREDITO;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.nuevoDocumento();

            Element raiz = doc.createElement("notaCredito");
            raiz.setAttribute("id", "comprobante");
            raiz.setAttribute("version", VERSION_XSD);
            doc.appendChild(raiz);

            raiz.appendChild(construirInfoTributaria(doc, c));
            raiz.appendChild(construirInfoNotaCredito(doc, c));
            raiz.appendChild(construirDetalles(doc, c));
            if (c.isEmisorEsGranContribuyente()) {
                raiz.appendChild(XmlComprobanteUtil.construirInfoAdicionalGranContribuyente(doc, c));
            }

            return XmlComprobanteUtil.serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el XML de la nota de crédito " + c.getId(), e);
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
        XmlComprobanteUtil.texto(doc, info, "codDoc", TipoComprobante.NOTA_CREDITO.codigo());
        XmlComprobanteUtil.texto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.texto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.texto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.texto(doc, info, "dirMatriz", c.getEmisorDireccionMatriz());
        return info;
    }

    private Element construirInfoNotaCredito(Document doc, Comprobante c) {
        Element info = doc.createElement("infoNotaCredito");
        XmlComprobanteUtil.texto(doc, info, "fechaEmision",
            FORMATO_FECHA.format(c.getFechaEmision().atZone(ZoneOffset.of("-05:00"))));
        XmlComprobanteUtil.texto(doc, info, "dirEstablecimiento",
            XmlComprobanteUtil.direccionEstablecimiento(c));
        XmlComprobanteUtil.texto(doc, info, "tipoIdentificacionComprador", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "razonSocialComprador", c.getCompradorRazonSocial());
        XmlComprobanteUtil.texto(doc, info, "identificacionComprador", c.getCompradorIdentificacion());
        if (c.isEmisorContribuyenteEspecial() && c.getEmisorNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.texto(doc, info, "contribuyenteEspecial", c.getEmisorNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.texto(doc, info, "obligadoContabilidad", c.isEmisorObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.texto(doc, info, "codDocModificado", c.getDocModificadoTipo());
        XmlComprobanteUtil.texto(doc, info, "numDocModificado", c.getDocModificadoNumero());
        XmlComprobanteUtil.texto(doc, info, "fechaEmisionDocSustento",
            FORMATO_FECHA.format(c.getDocModificadoFecha().atZone(ZoneOffset.of("-05:00"))));
        XmlComprobanteUtil.texto(doc, info, "totalSinImpuestos", XmlComprobanteUtil.montoTexto(c.getSubtotalSinImpuestos()));
        XmlComprobanteUtil.texto(doc, info, "valorModificacion", XmlComprobanteUtil.montoTexto(c.getImporteTotal()));
        XmlComprobanteUtil.texto(doc, info, "moneda", c.getMoneda());
        info.appendChild(construirTotalConImpuestos(doc, c));
        // Último hijo obligatorio de infoNotaCredito según el XSD real del
        // SRI (confirmado con un rechazo real de Recepción — no aparecía
        // claro como obligatorio en la ficha técnica en texto).
        XmlComprobanteUtil.texto(doc, info, "motivo", c.getMotivo());
        return info;
    }

    private Element construirTotalConImpuestos(Document doc, Comprobante c) {
        Element totalConImpuestos = doc.createElement("totalConImpuestos");
        for (var grupo : XmlComprobanteUtil.agruparImpuestos(c).values()) {
            Impuesto primero = grupo.get(0);
            BigDecimal baseImponible = grupo.stream().map(Impuesto::baseImponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal valor = grupo.stream().map(Impuesto::valor).reduce(BigDecimal.ZERO, BigDecimal::add);

            Element totalImpuesto = doc.createElement("totalImpuesto");
            XmlComprobanteUtil.texto(doc, totalImpuesto, "codigo", primero.codigo());
            XmlComprobanteUtil.texto(doc, totalImpuesto, "codigoPorcentaje", primero.codigoPorcentaje());
            XmlComprobanteUtil.texto(doc, totalImpuesto, "baseImponible", XmlComprobanteUtil.montoTexto(baseImponible));
            XmlComprobanteUtil.texto(doc, totalImpuesto, "valor", XmlComprobanteUtil.montoTexto(valor));
            totalConImpuestos.appendChild(totalImpuesto);
        }
        return totalConImpuestos;
    }

    /**
     * A diferencia de Factura/Liquidación de Compra, el XSD real de Nota de
     * Crédito nombra el código del ítem {@code codigoInterno}/{@code codigoAdicional}
     * (como Guía de Remisión), no {@code codigoPrincipal}/{@code codigoAuxiliar}
     * — confirmado con un rechazo real de Recepción
     * ("Invalid content was found starting with element 'codigoPrincipal'.
     * One of '{codigoInterno, codigoAdicional, descripcion}' is expected"),
     * así que no puede reusar {@code XmlComprobanteUtil.construirDetalle}.
     */
    private Element construirDetalles(Document doc, Comprobante c) {
        Element detalles = doc.createElement("detalles");
        for (DetalleComprobante d : c.getDetalles()) {
            Element detalle = doc.createElement("detalle");
            XmlComprobanteUtil.texto(doc, detalle, "codigoInterno", d.getCodigoPrincipal());
            if (d.getCodigoAuxiliar() != null && !d.getCodigoAuxiliar().isBlank()) {
                XmlComprobanteUtil.texto(doc, detalle, "codigoAdicional", d.getCodigoAuxiliar());
            }
            XmlComprobanteUtil.texto(doc, detalle, "descripcion", d.getDescripcion());
            XmlComprobanteUtil.texto(doc, detalle, "cantidad", d.getCantidad().toPlainString());
            XmlComprobanteUtil.texto(doc, detalle, "precioUnitario", XmlComprobanteUtil.montoTexto(d.getPrecioUnitario()));
            XmlComprobanteUtil.texto(doc, detalle, "descuento",
                XmlComprobanteUtil.montoTexto(d.getDescuento() == null ? BigDecimal.ZERO : d.getDescuento()));
            XmlComprobanteUtil.texto(doc, detalle, "precioTotalSinImpuesto",
                XmlComprobanteUtil.montoTexto(d.precioTotalSinImpuesto()));

            Element impuestos = doc.createElement("impuestos");
            for (Impuesto imp : d.getImpuestos()) {
                Element impuesto = doc.createElement("impuesto");
                XmlComprobanteUtil.texto(doc, impuesto, "codigo", imp.codigo());
                XmlComprobanteUtil.texto(doc, impuesto, "codigoPorcentaje", imp.codigoPorcentaje());
                XmlComprobanteUtil.texto(doc, impuesto, "tarifa", imp.tarifa().toPlainString());
                XmlComprobanteUtil.texto(doc, impuesto, "baseImponible", XmlComprobanteUtil.montoTexto(imp.baseImponible()));
                XmlComprobanteUtil.texto(doc, impuesto, "valor", XmlComprobanteUtil.montoTexto(imp.valor()));
                impuestos.appendChild(impuesto);
            }
            detalle.appendChild(impuestos);
            detalles.appendChild(detalle);
        }
        return detalles;
    }
}
