package com.facturacion.notificacion.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.notificacion.application.dto.PlantillaCorreoRequest;
import com.facturacion.notificacion.application.dto.PlantillaCorreoResponse;
import com.facturacion.notificacion.application.dto.PlantillaPorDefectoResponse;
import com.facturacion.notificacion.application.dto.VariablePlantillaResponse;
import com.facturacion.notificacion.application.dto.VersionPlantillaResponse;
import com.facturacion.notificacion.application.dto.VistaPreviaRequest;
import com.facturacion.notificacion.application.dto.VistaPreviaResponse;
import com.facturacion.notificacion.application.mapper.PlantillaCorreoResponseMapper;
import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.enums.VariableCorreo;
import com.facturacion.notificacion.domain.model.vo.ContenidoCorreo;
import com.facturacion.notificacion.domain.model.vo.LogoEmpresa;
import com.facturacion.notificacion.domain.port.ArmadorContenidoCorreo;
import com.facturacion.notificacion.domain.port.CorreoSalida;
import com.facturacion.notificacion.domain.port.SaneadorHtml;
import com.facturacion.notificacion.domain.repository.PlantillaCorreoRepository;
import com.facturacion.notificacion.domain.service.PlantillaCorreoValidador;
import com.facturacion.notificacion.shared.error.CodigoLog;
import com.facturacion.notificacion.shared.error.NotificacionError;

/**
 * Administración de las plantillas de correo de una empresa: guardar (con las reglas de {@link PlantillaCorreoValidador} y
 * el HTML limpiado), consultar, eliminar, ver el historial y probar una plantilla con datos de ejemplo. Todo opera sobre la
 * empresa de la api-key: no hay forma de tocar la plantilla de otra. Quien guarda o elimina renueva la caché de lectura
 * cuando su transacción ya terminó, para que el siguiente correo vea el cambio.
 */
@ApplicationScoped
public class PlantillaCorreoService {

    private static final Logger LOG = Logger.getLogger(PlantillaCorreoService.class);

    /** Qué dato de ejemplo ve la vista previa cuando la empresa no tiene nombre configurado. */
    private static final String NOMBRE_EMISOR_DE_EJEMPLO = "Su empresa";
    private static final String NOMBRE_RECEPTOR_DE_EJEMPLO = "Cliente de ejemplo";
    private static final BigDecimal TOTAL_DE_EJEMPLO = new BigDecimal("11.50");
    private static final String SIN_XML = "";
    /** Partes de una clave de acceso de ejemplo (49 dígitos): tipo, ambiente, serie, secuencial, código y emisión. */
    private static final String TIPO_DE_EJEMPLO = "01";
    private static final String AMBIENTE_DE_EJEMPLO = "1";
    private static final String SERIE_DE_EJEMPLO = "001001";
    private static final String SECUENCIAL_DE_EJEMPLO = "000000123";
    private static final String CODIGO_NUMERICO_DE_EJEMPLO = "12345678";
    private static final String EMISION_DE_EJEMPLO = "1";
    private static final String VERIFICADOR_DE_EJEMPLO = "0";
    private static final String REFERENCIA_AL_LOGO = "cid:" + LogoEmpresa.CID;
    private static final String PREFIJO_DATA_URI = "data:";
    private static final String BASE64_EN_DATA_URI = ";base64,";

    /** Lo que devuelve guardar: la plantilla y si se creó ahora (201) o se reemplazó (200). */
    public record ResultadoGuardado(PlantillaCorreoResponse plantilla, boolean creada) {
    }

    private final PlantillaCorreoRepository repository;
    private final SaneadorHtml saneador;
    private final ArmadorContenidoCorreo armador;
    private final LectorAjustesCorreo lectorAjustes;
    private final PlantillaCorreoResponseMapper mapper;

    @Inject
    public PlantillaCorreoService(PlantillaCorreoRepository repository, SaneadorHtml saneador,
            ArmadorContenidoCorreo armador, LectorAjustesCorreo lectorAjustes, PlantillaCorreoResponseMapper mapper) {
        this.repository = repository;
        this.saneador = saneador;
        this.armador = armador;
        this.lectorAjustes = lectorAjustes;
        this.mapper = mapper;
    }

    /** Crea la plantilla de la empresa para ese tipo, o la reemplaza (sube la versión y la anterior queda en el historial). */
    public Uni<ResultadoGuardado> guardar(String empresaRuc, String tipoTexto, PlantillaCorreoRequest request) {
        TipoComprobanteCorreo tipo = interpretarTipo(tipoTexto);
        String cuerpoHtml = validarYLimpiar(request.asunto(), request.cuerpoHtml(), request.cuerpoTexto());
        String cuerpoTexto = request.cuerpoTexto() == null || request.cuerpoTexto().isBlank() ? null : request.cuerpoTexto();
        boolean activa = request.activa() == null || request.activa();
        Instant ahora = Instant.now();
        return Panache.withTransaction(() -> repository.buscar(empresaRuc, tipo).chain(existente -> {
                if (existente == null) {
                    PlantillaCorreoEmpresa nueva = PlantillaCorreoEmpresa.crear(empresaRuc, tipo, request.asunto(),
                        cuerpoHtml, cuerpoTexto, activa, ahora);
                    return repository.guardar(nueva, null).map(guardada -> armarResultado(guardada, true));
                }
                PlantillaCorreoEmpresa anterior = existente.copiar();
                existente.reemplazar(request.asunto(), cuerpoHtml, cuerpoTexto, activa, ahora);
                return repository.guardar(existente, anterior).map(guardada -> armarResultado(guardada, false));
            }))
            .call(resultado -> repository.renovarCache())
            .invoke(resultado -> LogCodificado.info(LOG, CodigoLog.PLANTILLA_GUARDADA, "empresa=" + empresaRuc, tipo,
                resultado.plantilla().version()));
    }

    public Uni<PlantillaCorreoResponse> obtener(String empresaRuc, String tipoTexto) {
        TipoComprobanteCorreo tipo = interpretarTipo(tipoTexto);
        return Panache.withSession(() -> repository.buscar(empresaRuc, tipo))
            .map(plantilla -> mapper.toResponse(exigirExistente(plantilla, tipo)));
    }

    public Uni<List<PlantillaCorreoResponse>> listar(String empresaRuc) {
        return Panache.withSession(() -> repository.listar(empresaRuc))
            .map(mapper::toResponses);
    }

    /** Borra la plantilla de la empresa para ese tipo: desde entonces el correo sale con la plantilla por defecto. */
    public Uni<Void> eliminar(String empresaRuc, String tipoTexto) {
        TipoComprobanteCorreo tipo = interpretarTipo(tipoTexto);
        return Panache.withTransaction(() -> repository.buscar(empresaRuc, tipo)
                .chain(plantilla -> repository.eliminar(exigirExistente(plantilla, tipo))))
            .call(ignorado -> repository.renovarCache())
            .invoke(ignorado -> LogCodificado.info(LOG, CodigoLog.PLANTILLA_ELIMINADA, "empresa=" + empresaRuc, tipo));
    }

    /** Las versiones anteriores de la plantilla, la más nueva primero (vacío si nunca cambió). */
    public Uni<List<VersionPlantillaResponse>> listarHistorial(String empresaRuc, String tipoTexto) {
        TipoComprobanteCorreo tipo = interpretarTipo(tipoTexto);
        return Panache.withSession(() -> repository.listarHistorial(empresaRuc, tipo))
            .map(mapper::toVersionResponses);
    }

    /** La plantilla por defecto del tipo, con las variables escritas, para que la empresa parta de ella. */
    public PlantillaPorDefectoResponse obtenerPorDefecto(String tipoTexto) {
        TipoComprobanteCorreo tipo = interpretarTipo(tipoTexto);
        return mapper.toResponse(tipo.name(), armador.exportarPorDefecto(tipo));
    }

    /** Las variables que se pueden usar en una plantilla. */
    public List<VariablePlantillaResponse> listarVariables() {
        return Arrays.stream(VariableCorreo.values()).map(mapper::toResponse).toList();
    }

    /**
     * Arma el correo con la plantilla recibida y datos de ejemplo, sin guardarla. Aplica las mismas reglas que al guardar, así
     * que lo que se ve es lo que se enviaría.
     */
    public Uni<VistaPreviaResponse> armarVistaPrevia(String empresaRuc, VistaPreviaRequest request) {
        TipoComprobanteCorreo tipo = interpretarTipo(request.tipoComprobante());
        String cuerpoHtml = validarYLimpiar(request.asunto(), request.cuerpoHtml(), request.cuerpoTexto());
        String cuerpoTexto = request.cuerpoTexto() == null || request.cuerpoTexto().isBlank() ? null : request.cuerpoTexto();
        PlantillaCorreoEmpresa temporal = PlantillaCorreoEmpresa.crear(empresaRuc, tipo, request.asunto(), cuerpoHtml,
            cuerpoTexto, true, Instant.now());
        return Panache.withSession(() -> lectorAjustes.leer(empresaRuc, tipo, NOMBRE_EMISOR_DE_EJEMPLO))
            .map(ajustes -> {
                CorreoSalida.Ajustes conLaPlantilla = new CorreoSalida.Ajustes(null, ajustes.remitenteNombre(),
                    ajustes.nombreEmisor(), ajustes.logo(), temporal);
                ContenidoCorreo contenido = armador.armar(armarComprobanteDeEjemplo(empresaRuc, tipo, conLaPlantilla));
                return new VistaPreviaResponse(contenido.asunto(),
                    incrustarLogoEnLaPagina(contenido.html(), ajustes.logo()), contenido.texto(), ajustes.logo() != null);
            });
    }

    private String validarYLimpiar(String asunto, String cuerpoHtml, String cuerpoTexto) {
        PlantillaCorreoValidador.validar(asunto, cuerpoHtml, cuerpoTexto);
        String limpio = saneador.sanear(cuerpoHtml).trim();
        if (limpio.isEmpty()) {
            throw new ErrorNegocioException(NotificacionError.PLANTILLA_SIN_CONTENIDO);
        }
        return limpio;
    }

    private static TipoComprobanteCorreo interpretarTipo(String tipoTexto) {
        return TipoComprobanteCorreo.buscarPorNombre(tipoTexto).orElseThrow(() -> new ErrorNegocioException(
            NotificacionError.PLANTILLA_TIPO_INVALIDO, tipoTexto, TipoComprobanteCorreo.listarNombres()));
    }

    private static PlantillaCorreoEmpresa exigirExistente(PlantillaCorreoEmpresa plantilla, TipoComprobanteCorreo tipo) {
        if (plantilla == null) {
            throw new NotFoundException(NotificacionError.PLANTILLA_NO_ENCONTRADA, tipo);
        }
        return plantilla;
    }

    private ResultadoGuardado armarResultado(PlantillaCorreoEmpresa plantilla, boolean creada) {
        return new ResultadoGuardado(mapper.toResponse(plantilla), creada);
    }

    private static CorreoSalida.Comprobante armarComprobanteDeEjemplo(String empresaRuc, TipoComprobanteCorreo tipo,
            CorreoSalida.Ajustes ajustes) {
        String claveAcceso = FechasSri.formatearFechaClaveAcceso(FechasSri.obtenerHoy()) + TIPO_DE_EJEMPLO + empresaRuc
            + AMBIENTE_DE_EJEMPLO + SERIE_DE_EJEMPLO + SECUENCIAL_DE_EJEMPLO + CODIGO_NUMERICO_DE_EJEMPLO
            + EMISION_DE_EJEMPLO + VERIFICADOR_DE_EJEMPLO;
        return new CorreoSalida.Comprobante(null, claveAcceso, tipo.name(), NOMBRE_RECEPTOR_DE_EJEMPLO, TOTAL_DE_EJEMPLO,
            new byte[0], SIN_XML, ajustes);
    }

    /** Un navegador no entiende {@code cid:}: en la vista previa la imagen va dentro de la propia página. */
    private static String incrustarLogoEnLaPagina(String html, LogoEmpresa logo) {
        if (logo == null) {
            return html;
        }
        String base64 = Base64.getEncoder().encodeToString(logo.datos());
        return html.replace(REFERENCIA_AL_LOGO, PREFIJO_DATA_URI + logo.tipoContenido() + BASE64_EN_DATA_URI + base64);
    }
}
