package com.facturacion.comprobantes.application.service.construccion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.application.dto.CampoAdicionalRequest;
import com.facturacion.comprobantes.application.dto.EmpresaRequest;
import com.facturacion.comprobantes.application.dto.PagoRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.AmbienteSri;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.FormaPagoSri;
import com.facturacion.comprobantes.domain.model.enums.RegimenRimpe;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.model.vo.Pago;
import com.facturacion.comprobantes.domain.service.ClaveAccesoGenerador;
import com.facturacion.comprobantes.domain.service.PagosValidador;
import com.facturacion.comprobantes.shared.constant.ZonaHoraria;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Piezas comunes a los constructores de los 6 tipos de comprobante: encabezado (clave de acceso, snapshot de la
 * empresa, estado inicial), formas de pago, campos adicionales y fechas dd/MM/aaaa.
 */
@ApplicationScoped
public class ConstructorComun {

    private static final DateTimeFormatter FORMATO_FECHA_ESTRICTO =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Encabezado común a los 6 tipos (clave de acceso, snapshot de la empresa inline, estado inicial). */
    public Comprobante encabezado(EmpresaRequest empresa, TipoComprobante tipo, long secuencial,
            Instant ahora) {
        AmbienteSri ambiente = convertirAmbiente(empresa.getAmbiente());
        String claveAcceso = ClaveAccesoGenerador.generar(ClaveAccesoGenerador.obtenerFechaActualEcuador(), tipo, empresa.getRuc(),
            ambiente, empresa.getEstablecimiento(), empresa.getPuntoEmision(), secuencial);

        Comprobante comprobante = new Comprobante();
        comprobante.setId(UUID.randomUUID());
        comprobante.setTipoComprobante(tipo);
        comprobante.setClaveAcceso(claveAcceso);
        comprobante.setAmbiente(ambiente);
        comprobante.setEstablecimiento(empresa.getEstablecimiento());
        comprobante.setPuntoEmision(empresa.getPuntoEmision());
        comprobante.setSecuencial(String.format("%09d", secuencial));
        comprobante.setEmpresaRuc(empresa.getRuc());
        comprobante.setEmpresaRazonSocial(empresa.getRazonSocial());
        comprobante.setEmpresaNombreComercial(empresa.getNombreComercial());
        comprobante.setEmpresaDireccionMatriz(empresa.getDireccionMatriz());
        comprobante.setDireccionEstablecimiento(
            empresa.getDireccionEstablecimiento() != null && !empresa.getDireccionEstablecimiento().isBlank()
                ? empresa.getDireccionEstablecimiento()
                : empresa.getDireccionMatriz());
        comprobante.setEmpresaObligadoContabilidad(empresa.isObligadoContabilidad());
        comprobante.setEmpresaContribuyenteEspecial(empresa.isContribuyenteEspecial());
        comprobante.setEmpresaNumeroContribuyenteEspecial(empresa.getNumeroContribuyenteEspecial());
        comprobante.setEmpresaEsGranContribuyente(empresa.isEsGranContribuyente());
        comprobante.setEmpresaNumeroResolucionGranContribuyente(empresa.getNumeroResolucionGranContribuyente());
        comprobante.setEmpresaAgenteRetencion(empresa.getAgenteRetencion());
        comprobante.setEmpresaRegimenRimpe(RegimenRimpe.desde(empresa.getRegimenRimpe()));
        comprobante.setFechaEmision(ahora);
        comprobante.setEstado(EstadoComprobante.BORRADOR);
        comprobante.setCreadoEn(ahora);
        comprobante.setActualizadoEn(ahora);
        return comprobante;
    }

    /**
     * Si el caller no manda pagos explícitos, se asume una sola forma de pago "01" (sin utilización del
     * sistema financiero) por el total. Si los manda, se validan contra la Tabla 24 y contra el importe
     * total (ver {@link PagosValidador}); las formas de pago válidas salen del catálogo de
     * formas de pago (tabla propia). {@code admitePlazo} es false solo para Retención v2.0.0.
     */
    public List<Pago> pagos(List<PagoRequest> pagosRequest, BigDecimal importeTotal, boolean admitePlazo,
            CatalogosSriAFecha catalogos) {
        if (pagosRequest == null || pagosRequest.isEmpty()) {
            return List.of(new Pago(FormaPagoSri.SIN_UTILIZACION_SISTEMA_FINANCIERO.codigo(), importeTotal, null, null));
        }
        List<Pago> pagos = pagosRequest.stream()
            .map(p -> new Pago(p.getFormaPago(), p.getTotal(), p.getPlazo(), p.getUnidadTiempo()))
            .collect(Collectors.toList());
        PagosValidador.validar(pagos, importeTotal, admitePlazo, catalogos.formasPagoVigentes());
        return pagos;
    }

    /**
     * {@code docSustentoFecha} se guarda como {@code date} en {@code detalle_impuesto}; una fecha que no
     * existe en el calendario (31/02/2026) pasaría el patrón del XSD pero rompería al persistir, así que se
     * rechaza aquí con un 400 claro.
     */
    public void validarFechaDocSustento(String ddMmAaaa) {
        try {
            LocalDate.parse(ddMmAaaa, FORMATO_FECHA_ESTRICTO);
        } catch (DateTimeParseException e) {
            throw new ErrorNegocioException(CodigoError.FECHA_DOC_SUSTENTO_INVALIDA, ddMmAaaa);
        }
    }

    public List<CampoAdicional> camposAdicionales(List<CampoAdicionalRequest> camposRequest) {
        if (camposRequest == null) {
            return List.of();
        }
        return camposRequest.stream()
            .map(c -> new CampoAdicional(c.getNombre(), c.getValor()))
            .collect(Collectors.toList());
    }

    /**
     * Fecha dd/MM/aaaa a instante (inicio del día en hora de Ecuador); {@code null} o vacío da {@code null}. Es
     * estricta: antes se aceptaba "31/02/2026" y se corregía en silencio a 28/02, y un texto que no era fecha
     * terminaba en un 500.
     */
    public Instant convertirFecha(String ddMmAaaa) {
        if (ddMmAaaa == null || ddMmAaaa.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(ddMmAaaa, FORMATO_FECHA_ESTRICTO).atStartOfDay(ZonaHoraria.ECUADOR).toInstant();
        } catch (DateTimeParseException e) {
            throw new ErrorNegocioException(CodigoError.FECHA_DOCUMENTO_INVALIDA, ddMmAaaa);
        }
    }

    /** Traduce el texto "PRUEBAS"/"PRODUCCION" del request a {@code AmbienteSri}, con un mensaje de error legible si viene mal. */
    private static AmbienteSri convertirAmbiente(String texto) {
        try {
            return AmbienteSri.valueOf(texto);
        } catch (IllegalArgumentException e) {
            throw new ErrorNegocioException(CodigoError.AMBIENTE_INVALIDO, texto);
        }
    }
}
