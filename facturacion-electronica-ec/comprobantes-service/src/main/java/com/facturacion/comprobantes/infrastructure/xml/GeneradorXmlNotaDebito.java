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
 * Genera el XML de Nota de Débito conforme a la ficha técnica del SRI
 * (versión 1.0.0). A diferencia de Factura/Nota de Crédito no tiene
 * {@code <detalles>} de ítems — cada línea de {@link Comprobante#getDetalles()}
 * se reutiliza para guardar un {@code <motivo>} (descripción=razón,
 * precioTotalSinImpuesto=valor); el bloque {@code <impuestos>} de
 * encabezado se arma agrupando los impuestos de esas mismas líneas (ver
 * ComprobanteService.crearNotaDebito: solo la primera línea carga
 * impuestos, así {@link XmlComprobanteUtil#agruparImpuestos} reutiliza la
 * misma lógica de Factura sin cambios).
 */
@ApplicationScoped
public class GeneradorXmlNotaDebito implements GeneradorXmlComprobante {

    private static final String VERSION_XSD = "1.0.0";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean soporta(TipoComprobante tipo) {
        return tipo == TipoComprobante.NOTA_DEBITO;
    }

    @Override
    public String generar(Comprobante c) {
        try {
            Document doc = XmlComprobanteUtil.nuevoDocumento();

            Element raiz = doc.createElement("notaDebito");
            raiz.setAttribute("id", "comprobante");
            raiz.setAttribute("version", VERSION_XSD);
            doc.appendChild(raiz);

            raiz.appendChild(construirInfoTributaria(doc, c));
            raiz.appendChild(construirInfoNotaDebito(doc, c));
            raiz.appendChild(construirMotivos(doc, c));
            Element infoAdicional = XmlComprobanteUtil.construirInfoAdicional(doc, c);
            if (infoAdicional != null) {
                raiz.appendChild(infoAdicional);
            }

            return XmlComprobanteUtil.serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el XML de la nota de débito " + c.getId(), e);
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
        XmlComprobanteUtil.texto(doc, info, "codDoc", TipoComprobante.NOTA_DEBITO.codigo());
        XmlComprobanteUtil.texto(doc, info, "estab", c.getEstablecimiento());
        XmlComprobanteUtil.texto(doc, info, "ptoEmi", c.getPuntoEmision());
        XmlComprobanteUtil.texto(doc, info, "secuencial", c.getSecuencial());
        XmlComprobanteUtil.texto(doc, info, "dirMatriz", c.getEmpresaDireccionMatriz());
        return info;
    }

    private Element construirInfoNotaDebito(Document doc, Comprobante c) {
        Element info = doc.createElement("infoNotaDebito");
        XmlComprobanteUtil.texto(doc, info, "fechaEmision",
            FORMATO_FECHA.format(c.getFechaEmision().atZone(ZonaHoraria.ECUADOR)));
        XmlComprobanteUtil.texto(doc, info, "dirEstablecimiento", XmlComprobanteUtil.direccionEstablecimiento(c));
        XmlComprobanteUtil.texto(doc, info, "tipoIdentificacionComprador", c.getCompradorTipoIdentificacion());
        XmlComprobanteUtil.texto(doc, info, "razonSocialComprador", c.getCompradorRazonSocial());
        XmlComprobanteUtil.texto(doc, info, "identificacionComprador", c.getCompradorIdentificacion());
        if (c.isEmpresaContribuyenteEspecial() && c.getEmpresaNumeroContribuyenteEspecial() != null) {
            XmlComprobanteUtil.texto(doc, info, "contribuyenteEspecial", c.getEmpresaNumeroContribuyenteEspecial());
        }
        XmlComprobanteUtil.texto(doc, info, "obligadoContabilidad", c.isEmpresaObligadoContabilidad() ? "SI" : "NO");
        XmlComprobanteUtil.texto(doc, info, "codDocModificado", c.getDocModificadoTipo());
        XmlComprobanteUtil.texto(doc, info, "numDocModificado", c.getDocModificadoNumero());
        XmlComprobanteUtil.texto(doc, info, "fechaEmisionDocSustento",
            FORMATO_FECHA.format(c.getDocModificadoFecha().atZone(ZonaHoraria.ECUADOR)));
        XmlComprobanteUtil.texto(doc, info, "totalSinImpuestos", XmlComprobanteUtil.montoTexto(c.getSubtotalSinImpuestos()));
        info.appendChild(construirImpuestos(doc, c));
        XmlComprobanteUtil.texto(doc, info, "valorTotal", XmlComprobanteUtil.montoTexto(c.getImporteTotal()));
        info.appendChild(XmlComprobanteUtil.construirPagos(doc, c.getPagos()));
        return info;
    }

    private Element construirImpuestos(Document doc, Comprobante c) {
        Element impuestos = doc.createElement("impuestos");
        for (var grupo : XmlComprobanteUtil.agruparImpuestos(c).values()) {
            Impuesto primero = grupo.get(0);
            BigDecimal baseImponible = grupo.stream().map(Impuesto::baseImponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal valor = grupo.stream().map(Impuesto::valor).reduce(BigDecimal.ZERO, BigDecimal::add);

            Element impuesto = doc.createElement("impuesto");
            XmlComprobanteUtil.texto(doc, impuesto, "codigo", primero.codigo());
            XmlComprobanteUtil.texto(doc, impuesto, "codigoPorcentaje", primero.codigoPorcentaje());
            XmlComprobanteUtil.texto(doc, impuesto, "tarifa", primero.tarifa().toPlainString());
            XmlComprobanteUtil.texto(doc, impuesto, "baseImponible", XmlComprobanteUtil.montoTexto(baseImponible));
            XmlComprobanteUtil.texto(doc, impuesto, "valor", XmlComprobanteUtil.montoTexto(valor));
            impuestos.appendChild(impuesto);
        }
        return impuestos;
    }

    private Element construirMotivos(Document doc, Comprobante c) {
        Element motivos = doc.createElement("motivos");
        for (DetalleComprobante d : c.getDetalles()) {
            Element motivo = doc.createElement("motivo");
            XmlComprobanteUtil.texto(doc, motivo, "razon", d.getDescripcion());
            XmlComprobanteUtil.texto(doc, motivo, "valor", XmlComprobanteUtil.montoTexto(d.precioTotalSinImpuesto()));
            motivos.appendChild(motivo);
        }
        return motivos;
    }
}
