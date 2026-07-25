package com.facturacion.comprobantes.infrastructure.xml;

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
 * Genera el XML del Comprobante de Retención conforme a la ficha técnica
 * del SRI (versión 1.0.0). No tiene {@code <detalles>} de ítems: cada línea
 * de {@link Comprobante#getDetalles()} guarda exactamente un
 * {@code <impuesto>} (una línea de retención) en su lista de impuestos —
 * ver ComprobanteService.crearRetencion.
 */
@ApplicationScoped
public class GeneradorXmlRetencion implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "1.0.0";
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
            raiz.appendChild(construirImpuestos(doc, c));
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
        XmlComprobanteUtil.texto(doc, info, "razonSocial", c.getEmisorRazonSocial());
        if (c.getEmisorNombreComercial() != null && !c.getEmisorNombreComercial().isBlank()) {
            XmlComprobanteUtil.texto(doc, info, "nombreComercial", c.getEmisorNombreComercial());
        }
        XmlComprobanteUtil.texto(doc, info, "ruc", c.getEmisorRuc());
        XmlComprobanteUtil.texto(doc, info, "claveAcceso", c.getClaveAcceso());
        XmlComprobanteUtil.texto(doc, info, "codDoc", TipoComprobante.RETENCION.codigo());
        XmlComprobanteUtil.texto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.texto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.texto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.texto(doc, info, "dirMatriz", c.getEmisorDireccionMatriz());
        return info;
    }

    private Element construirInfoCompRetencion(Document doc, Comprobante c) {
        Element info = doc.createElement("infoCompRetencion");
        XmlComprobanteUtil.texto(doc, info, "fechaEmision",
            FORMATO_FECHA.format(c.getFechaEmision().atZone(ZoneOffset.of("-05:00"))));
        XmlComprobanteUtil.texto(doc, info, "dirEstablecimiento", XmlComprobanteUtil.direccionEstablecimiento(c));
        if (c.isEmisorContribuyenteEspecial() && c.getEmisorNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.texto(doc, info, "contribuyenteEspecial", c.getEmisorNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.texto(doc, info, "obligadoContabilidad", c.isEmisorObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.texto(doc, info, "tipoIdentificacionSujetoRetenido", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "razonSocialSujetoRetenido", c.getCompradorRazonSocial());
        XmlComprobanteUtil.texto(doc, info, "identificacionSujetoRetenido", c.getCompradorIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "periodoFiscal", c.getPeriodoFiscal());
        return info;
    }

    private Element construirImpuestos(Document doc, Comprobante c) {
        Element impuestos = doc.createElement("impuestos");
        for (DetalleComprobante d : c.getDetalles()) {
            for (Impuesto imp : d.getImpuestos()) {
                Element impuesto = doc.createElement("impuesto");
                XmlComprobanteUtil.texto(doc, impuesto, "codigo", imp.codigo());
                XmlComprobanteUtil.texto(doc, impuesto, "codigoRetencion", imp.codigoRetencion());
                XmlComprobanteUtil.texto(doc, impuesto, "baseImponible", XmlComprobanteUtil.montoTexto(imp.baseImponible()));
                XmlComprobanteUtil.texto(doc, impuesto, "porcentajeRetener", imp.tarifa().toPlainString());
                XmlComprobanteUtil.texto(doc, impuesto, "valorRetenido", XmlComprobanteUtil.montoTexto(imp.valor()));
                XmlComprobanteUtil.texto(doc, impuesto, "codDocSustento", imp.docSustentoCodigo());
                XmlComprobanteUtil.texto(doc, impuesto, "numDocSustento", imp.docSustentoNumero());
                XmlComprobanteUtil.texto(doc, impuesto, "fechaEmisionDocSustento", imp.docSustentoFecha());
                impuestos.appendChild(impuesto);
            }
        }
        return impuestos;
    }
}
