package com.facturacion.comprobantes.infrastructure.xml;

import java.time.format.DateTimeFormatter;

import jakarta.enterprise.context.ApplicationScoped;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.DocumentoSustento;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.model.vo.ImpuestoDocSustento;
import com.facturacion.comprobantes.domain.service.GeneradorXmlComprobante;
import com.facturacion.comprobantes.shared.constant.ZonaHoraria;

/**
 * Genera el XML del Comprobante de Retención conforme a la ficha técnica
 * v2.0.0 del SRI — reestructura todo el documento bajo un wrapper
 * {@code docsSustento}/{@code docSustento} estilo ATS: cada documento
 * retenido trae su propio {@code impuestosDocSustento} (el impuesto del
 * documento sustento, no de la retención), sus {@code retenciones} y sus
 * {@code pagos} — ver {@link Comprobante#getDocumentosSustento()} /
 * {@code ComprobanteService.construirRetencion}. {@code infoCompRetencion}
 * suma el campo {@code parteRel} (obligatorio en esta versión, ver
 * {@link Comprobante#getParteRelacionada()}).
 */
@ApplicationScoped
public class GeneradorXmlRetencion implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "2.0.0";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean soporta(TipoComprobante tipo) {
        return tipo == TipoComprobante.RETENCION;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.crearDocumento();

            Element raiz = doc.createElement("comprobanteRetencion");
            raiz.setAttribute("id", "comprobante");
            raiz.setAttribute("version", VERSION_XSD);
            doc.appendChild(raiz);

            raiz.appendChild(construirInfoTributaria(doc, c));
            raiz.appendChild(construirInfoCompRetencion(doc, c));
            raiz.appendChild(construirDocsSustento(doc, c));
            Element infoAdicional = XmlComprobanteUtil.construirInfoAdicional(doc, c);
            if (infoAdicional != null) {
                raiz.appendChild(infoAdicional);
            }

            return XmlComprobanteUtil.serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el XML del comprobante de retención " + c.getId(), e);
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
        XmlComprobanteUtil.agregarTexto(doc, info, "codDoc", TipoComprobante.RETENCION.codigo());
        XmlComprobanteUtil.agregarTexto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.agregarTexto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.agregarTexto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.agregarTexto(doc, info, "dirMatriz", c.getEmpresaDireccionMatriz());
        XmlComprobanteUtil.agregarAgenteYRegimen(doc, info, c);
        return info;
    }

    private Element construirInfoCompRetencion(Document doc, Comprobante c) {
        Element info = doc.createElement("infoCompRetencion");
        XmlComprobanteUtil.agregarTexto(doc, info, "fechaEmision",
            FORMATO_FECHA.format(c.getFechaEmision().atZone(ZonaHoraria.ECUADOR)));
        XmlComprobanteUtil.agregarTexto(doc, info, "dirEstablecimiento", XmlComprobanteUtil.direccionEstablecimiento(c));
        if (c.isEmpresaContribuyenteEspecial() && c.getEmpresaNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.agregarTexto(doc, info, "contribuyenteEspecial", c.getEmpresaNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.agregarTexto(doc, info, "obligadoContabilidad", c.isEmpresaObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.agregarTexto(doc, info, "tipoIdentificacionSujetoRetenido", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.agregarTexto(doc, info, "parteRel",
            c.getParteRelacionada() == null ? "NO" : c.getParteRelacionada());
        XmlComprobanteUtil.agregarTexto(doc, info, "razonSocialSujetoRetenido", c.getCompradorRazonSocial());
        XmlComprobanteUtil.agregarTexto(doc, info, "identificacionSujetoRetenido", c.getCompradorIdentificacion());
        XmlComprobanteUtil.agregarTexto(doc, info, "periodoFiscal", c.getPeriodoFiscal());
        return info;
    }

    private Element construirDocsSustento(Document doc, Comprobante c) {
        Element docsSustento = doc.createElement("docsSustento");
        for (DocumentoSustento ds : c.getDocumentosSustento()) {
            Element docSustento = doc.createElement("docSustento");
            XmlComprobanteUtil.agregarTexto(doc, docSustento, "codSustento", ds.codSustento());
            XmlComprobanteUtil.agregarTexto(doc, docSustento, "codDocSustento", ds.docSustentoCodigo());
            XmlComprobanteUtil.agregarTexto(doc, docSustento, "numDocSustento", ds.docSustentoNumero());
            XmlComprobanteUtil.agregarTexto(doc, docSustento, "fechaEmisionDocSustento", ds.docSustentoFecha());
            XmlComprobanteUtil.agregarTexto(doc, docSustento, "pagoLocExt", ds.pagoLocExt());
            XmlComprobanteUtil.agregarTexto(doc, docSustento, "totalSinImpuestos",
                XmlComprobanteUtil.formatearMonto(ds.totalSinImpuestos()));
            XmlComprobanteUtil.agregarTexto(doc, docSustento, "importeTotal",
                XmlComprobanteUtil.formatearMonto(ds.importeTotal()));
            docSustento.appendChild(construirImpuestosDocSustento(doc, ds));
            docSustento.appendChild(construirRetenciones(doc, ds));
            docSustento.appendChild(XmlComprobanteUtil.construirPagos(doc, ds.pagos()));
            docsSustento.appendChild(docSustento);
        }
        return docsSustento;
    }

    private Element construirImpuestosDocSustento(Document doc, DocumentoSustento ds) {
        Element impuestosDocSustento = doc.createElement("impuestosDocSustento");
        for (ImpuestoDocSustento imp : ds.impuestos()) {
            Element impuesto = doc.createElement("impuestoDocSustento");
            XmlComprobanteUtil.agregarTexto(doc, impuesto, "codImpuestoDocSustento", imp.codigo());
            XmlComprobanteUtil.agregarTexto(doc, impuesto, "codigoPorcentaje", imp.codigoPorcentaje());
            XmlComprobanteUtil.agregarTexto(doc, impuesto, "baseImponible", XmlComprobanteUtil.formatearMonto(imp.baseImponible()));
            XmlComprobanteUtil.agregarTexto(doc, impuesto, "tarifa", imp.tarifa().toPlainString());
            XmlComprobanteUtil.agregarTexto(doc, impuesto, "valorImpuesto", XmlComprobanteUtil.formatearMonto(imp.valor()));
            impuestosDocSustento.appendChild(impuesto);
        }
        return impuestosDocSustento;
    }

    private Element construirRetenciones(Document doc, DocumentoSustento ds) {
        Element retenciones = doc.createElement("retenciones");
        for (Impuesto ret : ds.retenciones()) {
            Element retencion = doc.createElement("retencion");
            XmlComprobanteUtil.agregarTexto(doc, retencion, "codigo", ret.codigo());
            XmlComprobanteUtil.agregarTexto(doc, retencion, "codigoRetencion", ret.codigoRetencion());
            XmlComprobanteUtil.agregarTexto(doc, retencion, "baseImponible", XmlComprobanteUtil.formatearMonto(ret.baseImponible()));
            XmlComprobanteUtil.agregarTexto(doc, retencion, "porcentajeRetener", ret.tarifa().toPlainString());
            XmlComprobanteUtil.agregarTexto(doc, retencion, "valorRetenido", XmlComprobanteUtil.formatearMonto(ret.valor()));
            retenciones.appendChild(retencion);
        }
        return retenciones;
    }
}
