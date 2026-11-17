package com.facturacion.comprobantes.infrastructure.xml;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import jakarta.enterprise.context.ApplicationScoped;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.service.GeneradorXmlComprobante;
import com.facturacion.comprobantes.shared.constant.ZonaHoraria;

/**
 * Genera el XML de Factura conforme al Anexo 1/3 de la ficha técnica del SRI
 * (declarado como versión 2.1.0, la última disponible — ver
 * {@code xsd/factura_V2.1.0.xsd} y {@code XsdValidador}). Los campos nuevos
 * de 1.1.0/2.x (reembolso, rubros de terceros, subsidios, comercio exterior)
 * son todos opcionales en el XSD; esta versión de referencia solo implementa
 * el subconjunto obligatorio + los ya wireados (ver ARQUITECTURA.md) —
 * omitirlos no invalida el XML contra el esquema 2.1.0.
 */
@ApplicationScoped
public class GeneradorXmlFactura implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "2.1.0";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean soporta(TipoComprobante tipo) {
        return tipo == TipoComprobante.FACTURA;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.crearDocumento();

            Element factura = doc.createElement("factura");
            factura.setAttribute("id", "comprobante");
            factura.setAttribute("version", VERSION_XSD);
            doc.appendChild(factura);

            factura.appendChild(construirInfoTributaria(doc, c));
            factura.appendChild(construirInfoFactura(doc, c));
            factura.appendChild(construirDetalles(doc, c));
            // infoAdicional: granContribuyente (si aplica) + campoAdicional del
            // request — el tag completo es opcional y se omite si no hay nada.
            Element infoAdicional = XmlComprobanteUtil.construirInfoAdicional(doc, c);
            if (infoAdicional != null) {
                factura.appendChild(infoAdicional);
            }

            return XmlComprobanteUtil.serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el XML de la factura " + c.getId(), e);
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
        XmlComprobanteUtil.agregarTexto(doc, info, "codDoc", TipoComprobante.FACTURA.codigo());
        XmlComprobanteUtil.agregarTexto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.agregarTexto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.agregarTexto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.agregarTexto(doc, info, "dirMatriz", c.getEmpresaDireccionMatriz());
        XmlComprobanteUtil.agregarAgenteYRegimen(doc, info, c);
        return info;
    }

    private Element construirInfoFactura(Document doc, Comprobante c) {
        Element info = doc.createElement("infoFactura");
        XmlComprobanteUtil.agregarTexto(doc, info, "fechaEmision",
            FORMATO_FECHA.format(c.getFechaEmision().atZone(ZonaHoraria.ECUADOR)));
        if (c.isEmpresaContribuyenteEspecial() && c.getEmpresaNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.agregarTexto(doc, info, "contribuyenteEspecial", c.getEmpresaNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.agregarTexto(doc, info, "obligadoContabilidad", c.isEmpresaObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.agregarTexto(doc, info, "tipoIdentificacionComprador", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.agregarTexto(doc, info, "razonSocialComprador", c.getCompradorRazonSocial());
        XmlComprobanteUtil.agregarTexto(doc, info, "identificacionComprador", c.getCompradorIdentificacion());
        if (c.getCompradorDireccion() != null && !c.getCompradorDireccion().isBlank()) {
            XmlComprobanteUtil.agregarTexto(doc, info, "direccionComprador", c.getCompradorDireccion());
        }
        XmlComprobanteUtil.agregarTexto(doc, info, "totalSinImpuestos", XmlComprobanteUtil.formatearMonto(c.getSubtotalSinImpuestos()));
        XmlComprobanteUtil.agregarTexto(doc, info, "totalDescuento", XmlComprobanteUtil.formatearMonto(c.getTotalDescuento()));
        info.appendChild(construirTotalConImpuestos(doc, c));
        XmlComprobanteUtil.agregarTexto(doc, info, "propina", "0.00");
        XmlComprobanteUtil.agregarTexto(doc, info, "importeTotal", XmlComprobanteUtil.formatearMonto(c.getImporteTotal()));
        XmlComprobanteUtil.agregarTexto(doc, info, "moneda", c.getMoneda());
        if (c.getPlaca() != null) {
            // Anexos 12 y 25: entre moneda y pagos.
            XmlComprobanteUtil.agregarTexto(doc, info, "placa", c.getPlaca());
        }
        info.appendChild(XmlComprobanteUtil.construirPagos(doc, c.getPagos()));
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
            XmlComprobanteUtil.agregarTexto(doc, totalImpuesto, "codigo", primero.codigo());
            XmlComprobanteUtil.agregarTexto(doc, totalImpuesto, "codigoPorcentaje", primero.codigoPorcentaje());
            XmlComprobanteUtil.agregarTexto(doc, totalImpuesto, "baseImponible", XmlComprobanteUtil.formatearMonto(baseImponible));
            XmlComprobanteUtil.agregarTexto(doc, totalImpuesto, "valor", XmlComprobanteUtil.formatearMonto(valor));
            totalConImpuestos.appendChild(totalImpuesto);
        }
        return totalConImpuestos;
    }

    private Element construirDetalles(Document doc, Comprobante c) {
        Element detalles = doc.createElement("detalles");
        for (DetalleComprobante d : c.getDetalles()) {
            detalles.appendChild(XmlComprobanteUtil.construirDetalle(doc, d));
        }
        return detalles;
    }
}
