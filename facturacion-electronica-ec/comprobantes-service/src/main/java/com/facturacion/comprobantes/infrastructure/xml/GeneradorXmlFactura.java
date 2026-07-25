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

/**
 * Genera el XML de Factura conforme al Anexo 1 de la ficha técnica del SRI
 * (versión 1.0.0 — es la variante cuyo detalle campo por campo se verificó
 * directamente contra el manual; la 1.1.0/2.x añaden campos opcionales para
 * casos especiales — reembolso, rubros de terceros, subsidios — que quedan
 * fuera de esta versión de referencia, ver ARQUITECTURA.md).
 */
@ApplicationScoped
public class GeneradorXmlFactura implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "1.0.0";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean soporta(TipoComprobante tipo) {
        return tipo == TipoComprobante.FACTURA;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.nuevoDocumento();

            Element factura = doc.createElement("factura");
            factura.setAttribute("id", "comprobante");
            factura.setAttribute("version", VERSION_XSD);
            doc.appendChild(factura);

            factura.appendChild(construirInfoTributaria(doc, c));
            factura.appendChild(construirInfoFactura(doc, c));
            factura.appendChild(construirDetalles(doc, c));
            // infoAdicional: solo se agrega si el emisor es Gran Contribuyente
            // (Anexo 24) — de lo contrario el tag es opcional y se omite.
            if (c.isEmisorEsGranContribuyente()) {
                factura.appendChild(XmlComprobanteUtil.construirInfoAdicionalGranContribuyente(doc, c));
            }

            return XmlComprobanteUtil.serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el XML de la factura " + c.getId(), e);
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
        XmlComprobanteUtil.texto(doc, info, "codDoc", TipoComprobante.FACTURA.codigo());
        XmlComprobanteUtil.texto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.texto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.texto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.texto(doc, info, "dirMatriz", c.getEmisorDireccionMatriz());
        return info;
    }

    private Element construirInfoFactura(Document doc, Comprobante c) {
        Element info = doc.createElement("infoFactura");
        XmlComprobanteUtil.texto(doc, info, "fechaEmision",
            FORMATO_FECHA.format(c.getFechaEmision().atZone(ZoneOffset.of("-05:00"))));
        if (c.isEmisorContribuyenteEspecial() && c.getEmisorNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.texto(doc, info, "contribuyenteEspecial", c.getEmisorNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.texto(doc, info, "obligadoContabilidad", c.isEmisorObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.texto(doc, info, "tipoIdentificacionComprador", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "razonSocialComprador", c.getCompradorRazonSocial());
        XmlComprobanteUtil.texto(doc, info, "identificacionComprador", c.getCompradorIdentificacion());
        if (c.getCompradorDireccion() != null && !c.getCompradorDireccion().isBlank()) {
            XmlComprobanteUtil.texto(doc, info, "direccionComprador", c.getCompradorDireccion());
        }
        XmlComprobanteUtil.texto(doc, info, "totalSinImpuestos", XmlComprobanteUtil.montoTexto(c.getSubtotalSinImpuestos()));
        XmlComprobanteUtil.texto(doc, info, "totalDescuento", XmlComprobanteUtil.montoTexto(c.getTotalDescuento()));
        info.appendChild(construirTotalConImpuestos(doc, c));
        XmlComprobanteUtil.texto(doc, info, "propina", "0.00");
        XmlComprobanteUtil.texto(doc, info, "importeTotal", XmlComprobanteUtil.montoTexto(c.getImporteTotal()));
        XmlComprobanteUtil.texto(doc, info, "moneda", c.getMoneda());
        info.appendChild(construirPagos(doc, c));
        return info;
    }

    /** Consolida los impuestos de todas las líneas de detalle, agrupados por código+código de porcentaje. */
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

    /** Forma de pago genérica (sin plazo) — suficiente para el alcance de esta versión de referencia. */
    private Element construirPagos(Document doc, Comprobante c) {
        Element pagos = doc.createElement("pagos");
        Element pago = doc.createElement("pago");
        XmlComprobanteUtil.texto(doc, pago, "formaPago", "01");
        XmlComprobanteUtil.texto(doc, pago, "total", XmlComprobanteUtil.montoTexto(c.getImporteTotal()));
        pagos.appendChild(pago);
        return pagos;
    }

    private Element construirDetalles(Document doc, Comprobante c) {
        Element detalles = doc.createElement("detalles");
        for (DetalleComprobante d : c.getDetalles()) {
            detalles.appendChild(XmlComprobanteUtil.construirDetalle(doc, d));
        }
        return detalles;
    }
}
