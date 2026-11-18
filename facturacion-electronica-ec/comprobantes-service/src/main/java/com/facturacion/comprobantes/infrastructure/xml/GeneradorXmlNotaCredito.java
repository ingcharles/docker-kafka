package com.facturacion.comprobantes.infrastructure.xml;

import java.math.BigDecimal;

import jakarta.enterprise.context.ApplicationScoped;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.service.GeneradorXmlComprobante;
import com.facturacion.comprobantes.shared.error.CodigoError;

/** Genera el XML de Nota de Crédito conforme al Anexo de la ficha técnica del SRI (versión 1.1.0, la última disponible — ver {@code xsd/NotaCredito_V1.1.0.xsd}). */
@ApplicationScoped
public class GeneradorXmlNotaCredito implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "1.1.0";

    @Override
    public boolean esTipoSoportado(TipoComprobante tipo) {
        return tipo == TipoComprobante.NOTA_CREDITO;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.crearDocumento();

            Element raiz = doc.createElement("notaCredito");
            raiz.setAttribute("id", "comprobante");
            raiz.setAttribute("version", VERSION_XSD);
            doc.appendChild(raiz);

            raiz.appendChild(construirInfoTributaria(doc, c));
            raiz.appendChild(construirInfoNotaCredito(doc, c));
            raiz.appendChild(construirDetalles(doc, c));
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
        XmlComprobanteUtil.agregarTexto(doc, info, "codDoc", TipoComprobante.NOTA_CREDITO.codigo());
        XmlComprobanteUtil.agregarTexto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.agregarTexto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.agregarTexto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.agregarTexto(doc, info, "dirMatriz", c.getEmpresaDireccionMatriz());
        XmlComprobanteUtil.agregarAgenteYRegimen(doc, info, c);
        return info;
    }

    private Element construirInfoNotaCredito(Document doc, Comprobante c) {
        Element info = doc.createElement("infoNotaCredito");
        XmlComprobanteUtil.agregarTexto(doc, info, "fechaEmision",
            FechasSri.formatearFecha(c.getFechaEmision()));
        XmlComprobanteUtil.agregarTexto(doc, info, "dirEstablecimiento",
            XmlComprobanteUtil.obtenerDireccionEstablecimiento(c));
        XmlComprobanteUtil.agregarTexto(doc, info, "tipoIdentificacionComprador", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.agregarTexto(doc, info, "razonSocialComprador", c.getCompradorRazonSocial());
        XmlComprobanteUtil.agregarTexto(doc, info, "identificacionComprador", c.getCompradorIdentificacion());
        if (c.isEmpresaContribuyenteEspecial() && c.getEmpresaNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.agregarTexto(doc, info, "contribuyenteEspecial", c.getEmpresaNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.agregarTexto(doc, info, "obligadoContabilidad", c.isEmpresaObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.agregarTexto(doc, info, "codDocModificado", c.getDocModificadoTipo());
        XmlComprobanteUtil.agregarTexto(doc, info, "numDocModificado", c.getDocModificadoNumero());
        XmlComprobanteUtil.agregarTexto(doc, info, "fechaEmisionDocSustento",
            FechasSri.formatearFecha(c.getDocModificadoFecha()));
        XmlComprobanteUtil.agregarTexto(doc, info, "totalSinImpuestos", XmlComprobanteUtil.formatearMonto(c.getSubtotalSinImpuestos()));
        XmlComprobanteUtil.agregarTexto(doc, info, "valorModificacion", XmlComprobanteUtil.formatearMonto(c.getImporteTotal()));
        XmlComprobanteUtil.agregarTexto(doc, info, "moneda", c.getMoneda());
        info.appendChild(construirTotalConImpuestos(doc, c));
        // Último hijo obligatorio de infoNotaCredito según el XSD real del
        // SRI (confirmado con un rechazo real de Recepción — no aparecía
        // claro como obligatorio en la ficha técnica en texto).
        XmlComprobanteUtil.agregarTexto(doc, info, "motivo", c.getMotivo());
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
            XmlComprobanteUtil.agregarTexto(doc, totalImpuesto, "codigo", primero.codigo());
            XmlComprobanteUtil.agregarTexto(doc, totalImpuesto, "codigoPorcentaje", primero.codigoPorcentaje());
            XmlComprobanteUtil.agregarTexto(doc, totalImpuesto, "baseImponible", XmlComprobanteUtil.formatearMonto(baseImponible));
            XmlComprobanteUtil.agregarTexto(doc, totalImpuesto, "valor", XmlComprobanteUtil.formatearMonto(valor));
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
            XmlComprobanteUtil.agregarTexto(doc, detalle, "codigoInterno", d.getCodigoPrincipal());
            if (d.getCodigoAuxiliar() != null && !d.getCodigoAuxiliar().isBlank()) {
                XmlComprobanteUtil.agregarTexto(doc, detalle, "codigoAdicional", d.getCodigoAuxiliar());
            }
            XmlComprobanteUtil.agregarTexto(doc, detalle, "descripcion", d.getDescripcion());
            XmlComprobanteUtil.agregarTexto(doc, detalle, "cantidad", d.getCantidad().toPlainString());
            XmlComprobanteUtil.agregarTexto(doc, detalle, "precioUnitario", XmlComprobanteUtil.formatearMonto(d.getPrecioUnitario()));
            XmlComprobanteUtil.agregarTexto(doc, detalle, "descuento",
                XmlComprobanteUtil.formatearMonto(d.getDescuento() == null ? BigDecimal.ZERO : d.getDescuento()));
            XmlComprobanteUtil.agregarTexto(doc, detalle, "precioTotalSinImpuesto",
                XmlComprobanteUtil.formatearMonto(d.calcularPrecioTotalSinImpuesto()));

            Element impuestos = doc.createElement("impuestos");
            for (Impuesto imp : d.getImpuestos()) {
                Element impuesto = doc.createElement("impuesto");
                XmlComprobanteUtil.agregarTexto(doc, impuesto, "codigo", imp.codigo());
                XmlComprobanteUtil.agregarTexto(doc, impuesto, "codigoPorcentaje", imp.codigoPorcentaje());
                XmlComprobanteUtil.agregarTexto(doc, impuesto, "tarifa", imp.tarifa().toPlainString());
                XmlComprobanteUtil.agregarTexto(doc, impuesto, "baseImponible", XmlComprobanteUtil.formatearMonto(imp.baseImponible()));
                XmlComprobanteUtil.agregarTexto(doc, impuesto, "valor", XmlComprobanteUtil.formatearMonto(imp.valor()));
                impuestos.appendChild(impuesto);
            }
            detalle.appendChild(impuestos);
            detalles.appendChild(detalle);
        }
        return detalles;
    }
}
