package com.facturacion.comprobantes.infrastructure.xml;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import jakarta.enterprise.context.ApplicationScoped;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.comprobantes.shared.constant.ZonaHoraria;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.service.GeneradorXmlComprobante;

/**
 * Genera el XML de Liquidación de Compra conforme a la ficha técnica del
 * SRI (versión 1.1.0, la última disponible — ver {@code xsd/LiquidacionCompra_V1.1.0.xsd}).
 * Mismo shape que Factura (detalle/impuestos/totales)
 * — la única diferencia real es que la contraparte es un "proveedor", no un
 * "comprador" (quien emite es el comprador, ante la imposibilidad del
 * proveedor de emitir su propio comprobante de venta). No incluye el
 * bloque opcional de reembolso (codDocReembolso) — fuera del alcance de
 * esta versión de referencia, igual que Factura 1.1.0/2.x, ver ARQUITECTURA.md.
 */
@ApplicationScoped
public class GeneradorXmlLiquidacionCompra implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "1.1.0";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean soporta(TipoComprobante tipo) {
        return tipo == TipoComprobante.LIQUIDACION_COMPRA;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.nuevoDocumento();

            Element raiz = doc.createElement("liquidacionCompra");
            raiz.setAttribute("id", "comprobante");
            raiz.setAttribute("version", VERSION_XSD);
            doc.appendChild(raiz);

            raiz.appendChild(construirInfoTributaria(doc, c));
            raiz.appendChild(construirInfoLiquidacionCompra(doc, c));
            raiz.appendChild(construirDetalles(doc, c));
            Element infoAdicional = XmlComprobanteUtil.construirInfoAdicional(doc, c);
            if (infoAdicional != null) {
                raiz.appendChild(infoAdicional);
            }

            return XmlComprobanteUtil.serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el XML de la liquidación de compra " + c.getId(), e);
        }
    }

    private Element construirInfoTributaria(Document doc, Comprobante c) {
        Element info = doc.createElement("infoTributaria");
        XmlComprobanteUtil.texto(doc, info, "ambiente", c.getAmbiente().codigo());
        XmlComprobanteUtil.texto(doc, info, "tipoEmision", "1");
        XmlComprobanteUtil.texto(doc, info, "razonSocial", c.getEmpresaRazonSocial());
        if (c.getEmpresaNombreComercial() != null && !c.getEmpresaNombreComercial().isBlank()) {
            XmlComprobanteUtil.texto(doc, info, "nombreComercial", c.getEmpresaNombreComercial());
        }
        XmlComprobanteUtil.texto(doc, info, "ruc", c.getEmpresaRuc());
        XmlComprobanteUtil.texto(doc, info, "claveAcceso", c.getClaveAcceso());
        XmlComprobanteUtil.texto(doc, info, "codDoc", TipoComprobante.LIQUIDACION_COMPRA.codigo());
        XmlComprobanteUtil.texto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.texto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.texto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.texto(doc, info, "dirMatriz", c.getEmpresaDireccionMatriz());
        return info;
    }

    private Element construirInfoLiquidacionCompra(Document doc, Comprobante c) {
        Element info = doc.createElement("infoLiquidacionCompra");
        XmlComprobanteUtil.texto(doc, info, "fechaEmision",
            FORMATO_FECHA.format(c.getFechaEmision().atZone(ZonaHoraria.ECUADOR)));
        XmlComprobanteUtil.texto(doc, info, "dirEstablecimiento", XmlComprobanteUtil.direccionEstablecimiento(c));
        if (c.isEmpresaContribuyenteEspecial() && c.getEmpresaNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.texto(doc, info, "contribuyenteEspecial", c.getEmpresaNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.texto(doc, info, "obligadoContabilidad", c.isEmpresaObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.texto(doc, info, "tipoIdentificacionProveedor", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "razonSocialProveedor", c.getCompradorRazonSocial());
        XmlComprobanteUtil.texto(doc, info, "identificacionProveedor", c.getCompradorIdentificacion());
        if (c.getCompradorDireccion() != null && !c.getCompradorDireccion().isBlank()) {
            XmlComprobanteUtil.texto(doc, info, "direccionProveedor", c.getCompradorDireccion());
        }
        XmlComprobanteUtil.texto(doc, info, "totalSinImpuestos", XmlComprobanteUtil.montoTexto(c.getSubtotalSinImpuestos()));
        XmlComprobanteUtil.texto(doc, info, "totalDescuento", XmlComprobanteUtil.montoTexto(c.getTotalDescuento()));
        info.appendChild(construirTotalConImpuestos(doc, c));
        XmlComprobanteUtil.texto(doc, info, "importeTotal", XmlComprobanteUtil.montoTexto(c.getImporteTotal()));
        XmlComprobanteUtil.texto(doc, info, "moneda", c.getMoneda());
        info.appendChild(XmlComprobanteUtil.construirPagos(doc, c.getPagos()));
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

    private Element construirDetalles(Document doc, Comprobante c) {
        Element detalles = doc.createElement("detalles");
        for (DetalleComprobante d : c.getDetalles()) {
            detalles.appendChild(XmlComprobanteUtil.construirDetalle(doc, d));
        }
        return detalles;
    }
}
