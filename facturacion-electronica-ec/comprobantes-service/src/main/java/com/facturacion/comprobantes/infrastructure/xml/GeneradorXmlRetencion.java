package com.facturacion.comprobantes.infrastructure.xml;

import java.time.ZoneOffset;
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
            Document doc = XmlComprobanteUtil.nuevoDocumento();

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
        XmlComprobanteUtil.texto(doc, info, "ambiente", c.getAmbiente().codigo());
        XmlComprobanteUtil.texto(doc, info, "tipoEmision", "1");
        XmlComprobanteUtil.texto(doc, info, "razonSocial", c.getEmpresaRazonSocial());
        if (c.getEmpresaNombreComercial() != null && !c.getEmpresaNombreComercial().isBlank()) {
            XmlComprobanteUtil.texto(doc, info, "nombreComercial", c.getEmpresaNombreComercial());
        }
        XmlComprobanteUtil.texto(doc, info, "ruc", c.getEmpresaRuc());
        XmlComprobanteUtil.texto(doc, info, "claveAcceso", c.getClaveAcceso());
        XmlComprobanteUtil.texto(doc, info, "codDoc", TipoComprobante.RETENCION.codigo());
        XmlComprobanteUtil.texto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.texto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.texto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.texto(doc, info, "dirMatriz", c.getEmpresaDireccionMatriz());
        return info;
    }

    private Element construirInfoCompRetencion(Document doc, Comprobante c) {
        Element info = doc.createElement("infoCompRetencion");
        XmlComprobanteUtil.texto(doc, info, "fechaEmision",
            FORMATO_FECHA.format(c.getFechaEmision().atZone(ZoneOffset.of("-05:00"))));
        XmlComprobanteUtil.texto(doc, info, "dirEstablecimiento", XmlComprobanteUtil.direccionEstablecimiento(c));
        if (c.isEmpresaContribuyenteEspecial() && c.getEmpresaNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.texto(doc, info, "contribuyenteEspecial", c.getEmpresaNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.texto(doc, info, "obligadoContabilidad", c.isEmpresaObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.texto(doc, info, "tipoIdentificacionSujetoRetenido", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "parteRel",
            c.getParteRelacionada() == null ? "NO" : c.getParteRelacionada());
        XmlComprobanteUtil.texto(doc, info, "razonSocialSujetoRetenido", c.getCompradorRazonSocial());
        XmlComprobanteUtil.texto(doc, info, "identificacionSujetoRetenido", c.getCompradorIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "periodoFiscal", c.getPeriodoFiscal());
        return info;
    }

    private Element construirDocsSustento(Document doc, Comprobante c) {
        Element docsSustento = doc.createElement("docsSustento");
        for (DocumentoSustento ds : c.getDocumentosSustento()) {
            Element docSustento = doc.createElement("docSustento");
            XmlComprobanteUtil.texto(doc, docSustento, "codSustento", ds.codSustento());
            XmlComprobanteUtil.texto(doc, docSustento, "codDocSustento", ds.docSustentoCodigo());
            XmlComprobanteUtil.texto(doc, docSustento, "numDocSustento", ds.docSustentoNumero());
            XmlComprobanteUtil.texto(doc, docSustento, "fechaEmisionDocSustento", ds.docSustentoFecha());
            XmlComprobanteUtil.texto(doc, docSustento, "pagoLocExt", ds.pagoLocExt());
            XmlComprobanteUtil.texto(doc, docSustento, "totalSinImpuestos",
                XmlComprobanteUtil.montoTexto(ds.totalSinImpuestos()));
            XmlComprobanteUtil.texto(doc, docSustento, "importeTotal",
                XmlComprobanteUtil.montoTexto(ds.importeTotal()));
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
            XmlComprobanteUtil.texto(doc, impuesto, "codImpuestoDocSustento", imp.codigo());
            XmlComprobanteUtil.texto(doc, impuesto, "codigoPorcentaje", imp.codigoPorcentaje());
            XmlComprobanteUtil.texto(doc, impuesto, "baseImponible", XmlComprobanteUtil.montoTexto(imp.baseImponible()));
            XmlComprobanteUtil.texto(doc, impuesto, "tarifa", imp.tarifa().toPlainString());
            XmlComprobanteUtil.texto(doc, impuesto, "valorImpuesto", XmlComprobanteUtil.montoTexto(imp.valor()));
            impuestosDocSustento.appendChild(impuesto);
        }
        return impuestosDocSustento;
    }

    private Element construirRetenciones(Document doc, DocumentoSustento ds) {
        Element retenciones = doc.createElement("retenciones");
        for (Impuesto ret : ds.retenciones()) {
            Element retencion = doc.createElement("retencion");
            XmlComprobanteUtil.texto(doc, retencion, "codigo", ret.codigo());
            XmlComprobanteUtil.texto(doc, retencion, "codigoRetencion", ret.codigoRetencion());
            XmlComprobanteUtil.texto(doc, retencion, "baseImponible", XmlComprobanteUtil.montoTexto(ret.baseImponible()));
            XmlComprobanteUtil.texto(doc, retencion, "porcentajeRetener", ret.tarifa().toPlainString());
            XmlComprobanteUtil.texto(doc, retencion, "valorRetenido", XmlComprobanteUtil.montoTexto(ret.valor()));
            retenciones.appendChild(retencion);
        }
        return retenciones;
    }
}
