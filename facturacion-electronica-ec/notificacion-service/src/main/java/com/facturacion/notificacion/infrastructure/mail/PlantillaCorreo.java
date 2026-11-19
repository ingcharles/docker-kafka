package com.facturacion.notificacion.infrastructure.mail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.enums.VariableCorreo;
import com.facturacion.notificacion.domain.model.vo.ContenidoCorreo;
import com.facturacion.notificacion.domain.model.vo.PlantillaPorDefecto;
import com.facturacion.notificacion.domain.port.ArmadorContenidoCorreo;
import com.facturacion.notificacion.domain.port.CorreoSalida;
import com.facturacion.notificacion.domain.service.ClaveAcceso;
import com.facturacion.notificacion.domain.service.HtmlATexto;
import com.facturacion.notificacion.domain.service.SustitutoVariables;
import com.facturacion.notificacion.shared.error.CodigoLog;

/**
 * Arma el asunto y el cuerpo (HTML y texto plano) del correo de un comprobante. Hay dos orígenes, en este orden:
 * <ol>
 *   <li>La plantilla propia de la empresa (tabla {@code plantilla_correo}), con variables {@code {{nombre}}} que se
 *       reemplazan por texto, sin lenguaje de plantillas. El HTML ya se limpió al guardarlo; aquí cada valor se escapa. Si
 *       falla algo, el correo no se pierde: se registra y sale con la plantilla por defecto.</li>
 *   <li>La plantilla por defecto: archivos Qute de {@code src/main/resources/templates/correo} ({@code asunto.txt},
 *       {@code base.html}/{@code base.txt} y una pareja {@code <tipo>.html}/{@code .txt} por tipo). Qute escapa los valores en
 *       las plantillas {@code .html}. Si la empresa tiene el ajuste {@code notificacion.asunto}, ese reemplaza al asunto.</li>
 * </ol>
 * El cuerpo HTML siempre se entrega dentro de {@code documento.html} (doctype, cabecera y cuerpo). El logo se muestra con la
 * variable {@code logo}: la imagen incrustada si la empresa tiene logo y, si no, el nombre de la empresa.
 */
@ApplicationScoped
public class PlantillaCorreo implements ArmadorContenidoCorreo {

    private static final Logger LOG = Logger.getLogger(PlantillaCorreo.class);

    private static final String DIRECTORIO = "correo/";
    private static final String EXTENSION_HTML = ".html";
    private static final String EXTENSION_TEXTO = ".txt";
    private static final String PLANTILLA_ASUNTO = DIRECTORIO + "asunto" + EXTENSION_TEXTO;
    private static final String PLANTILLA_DOCUMENTO = DIRECTORIO + "documento" + EXTENSION_HTML;
    private static final String PLANTILLA_LOGO = DIRECTORIO + "logo" + EXTENSION_HTML;
    private static final String VACIO = "";
    private static final String SALTOS_DE_LINEA = "[\\r\\n]+";
    private static final String ESPACIO = " ";
    private static final String RECEPTOR_SIN_NOMBRE = "cliente";
    private static final int DECIMALES_TOTAL = 2;

    private static final String DATO_ASUNTO = "asunto";
    private static final String DATO_CONTENIDO = "contenido";
    private static final String DATO_LOGO = "logo";
    private static final String DATO_TIENE_LOGO = "tieneLogo";
    private static final String DATO_NOMBRE_EMISOR = "nombreEmisor";

    private final Engine engine;

    @Inject
    public PlantillaCorreo(Engine engine) {
        this.engine = engine;
    }

    @Override
    public ContenidoCorreo armar(CorreoSalida.Comprobante comprobante) {
        TipoComprobanteCorreo tipo = TipoComprobanteCorreo.obtenerPorNombre(comprobante.tipoComprobante());
        CorreoSalida.Ajustes ajustes = comprobante.ajustes();
        Map<VariableCorreo, String> valores = armarValores(tipo, comprobante);
        String fragmentoLogo = armarFragmentoLogo(ajustes.logo() != null, valores.get(VariableCorreo.NOMBRE_EMISOR));
        PlantillaCorreoEmpresa propia = ajustes.plantilla();
        if (propia != null) {
            try {
                return armarConPlantillaDeLaEmpresa(propia, valores, fragmentoLogo);
            } catch (RuntimeException e) {
                LogCodificado.warn(LOG, CodigoLog.PLANTILLA_EMPRESA_FALLIDA, "claveAcceso=" + comprobante.claveAcceso(),
                    e.getClass().getSimpleName());
            }
        }
        return armarConPlantillaPorDefecto(tipo, valores, fragmentoLogo, ajustes.asunto());
    }

    @Override
    public PlantillaPorDefecto exportarPorDefecto(TipoComprobanteCorreo tipo) {
        Map<VariableCorreo, String> marcadores = new EnumMap<>(VariableCorreo.class);
        for (VariableCorreo variable : VariableCorreo.values()) {
            marcadores.put(variable, variable.marcador());
        }
        String logo = VariableCorreo.LOGO.marcador();
        return new PlantillaPorDefecto(
            renderizar(PLANTILLA_ASUNTO, datosDePlantilla(marcadores, logo)).strip(),
            renderizar(DIRECTORIO + tipo.plantilla() + EXTENSION_HTML, datosDePlantilla(marcadores, logo)).strip(),
            renderizar(DIRECTORIO + tipo.plantilla() + EXTENSION_TEXTO, datosDePlantilla(marcadores, logo)).strip());
    }

    private ContenidoCorreo armarConPlantillaDeLaEmpresa(PlantillaCorreoEmpresa propia,
            Map<VariableCorreo, String> valores, String fragmentoLogo) {
        Function<VariableCorreo, String> paraTexto = variable -> variable == VariableCorreo.LOGO
            ? valores.get(VariableCorreo.NOMBRE_EMISOR) : valorConReceptor(valores, variable);
        Function<VariableCorreo, String> paraHtml = variable -> variable == VariableCorreo.LOGO ? fragmentoLogo
            : SustitutoVariables.escaparHtml(valorConReceptor(valores, variable));

        String asunto = limpiarSaltosDeLinea(SustitutoVariables.sustituir(propia.getAsunto(), paraTexto));
        String fragmentoHtml = SustitutoVariables.sustituir(propia.getCuerpoHtml(), paraHtml);
        String texto = propia.getCuerpoTexto() == null || propia.getCuerpoTexto().isBlank()
            ? HtmlATexto.convertir(fragmentoHtml)
            : SustitutoVariables.sustituir(propia.getCuerpoTexto(), paraTexto);
        return new ContenidoCorreo(asunto, envolverEnDocumento(asunto, fragmentoHtml), texto);
    }

    private ContenidoCorreo armarConPlantillaPorDefecto(TipoComprobanteCorreo tipo, Map<VariableCorreo, String> valores,
            String fragmentoLogo, String asuntoDeLaEmpresa) {
        Map<String, Object> datos = datosDePlantilla(valores, fragmentoLogo);
        String asunto = asuntoDeLaEmpresa != null && !asuntoDeLaEmpresa.isBlank()
            ? limpiarSaltosDeLinea(asuntoDeLaEmpresa)
            : limpiarSaltosDeLinea(renderizar(PLANTILLA_ASUNTO, datos));
        String fragmentoHtml = renderizar(DIRECTORIO + tipo.plantilla() + EXTENSION_HTML, datos);
        String texto = renderizar(DIRECTORIO + tipo.plantilla() + EXTENSION_TEXTO, datos);
        return new ContenidoCorreo(asunto, envolverEnDocumento(asunto, fragmentoHtml), texto);
    }

    /** Los valores en texto plano (sin escapar) de cada variable, salvo el logo, que se arma aparte. */
    private static Map<VariableCorreo, String> armarValores(TipoComprobanteCorreo tipo, CorreoSalida.Comprobante comprobante) {
        String clave = comprobante.claveAcceso();
        CorreoSalida.Ajustes ajustes = comprobante.ajustes();
        Map<VariableCorreo, String> valores = new EnumMap<>(VariableCorreo.class);
        valores.put(VariableCorreo.TIPO_COMPROBANTE, tipo.etiqueta());
        valores.put(VariableCorreo.NUMERO, Objects.toString(ClaveAcceso.obtenerNumeroDelComprobante(clave), VACIO));
        valores.put(VariableCorreo.FECHA_EMISION, Objects.toString(ClaveAcceso.obtenerFechaDeEmision(clave), VACIO));
        valores.put(VariableCorreo.TOTAL, formatearTotal(comprobante.importeTotal()));
        valores.put(VariableCorreo.CLAVE_ACCESO, Objects.toString(clave, VACIO));
        valores.put(VariableCorreo.RUC_EMISOR, Objects.toString(ClaveAcceso.obtenerRucDelEmisor(clave), VACIO));
        valores.put(VariableCorreo.NOMBRE_EMISOR, Objects.toString(ajustes.nombreEmisor(), VACIO).trim());
        valores.put(VariableCorreo.NOMBRE_RECEPTOR, Objects.toString(comprobante.nombreReceptor(), VACIO).trim());
        valores.put(VariableCorreo.LOGO, VACIO);
        return valores;
    }

    private static String formatearTotal(BigDecimal total) {
        return total == null ? VACIO : total.setScale(DECIMALES_TOTAL, RoundingMode.HALF_UP).toPlainString();
    }

    /** En una plantilla de empresa el receptor sin nombre se llama "cliente" (no hay condicionales para saludar distinto). */
    private static String valorConReceptor(Map<VariableCorreo, String> valores, VariableCorreo variable) {
        String valor = valores.get(variable);
        return variable == VariableCorreo.NOMBRE_RECEPTOR && valor.isBlank() ? RECEPTOR_SIN_NOMBRE : valor;
    }

    /** Lo que ven los archivos Qute: cada variable por su nombre y el logo ya armado como HTML. */
    private static Map<String, Object> datosDePlantilla(Map<VariableCorreo, String> valores, String fragmentoLogo) {
        Map<String, Object> datos = new LinkedHashMap<>();
        valores.forEach((variable, valor) -> datos.put(variable.nombre(), valor));
        datos.put(DATO_LOGO, fragmentoLogo);
        return datos;
    }

    /** La imagen incrustada si hay logo; si no, el nombre de la empresa; si tampoco hay nombre, nada. */
    private String armarFragmentoLogo(boolean tieneLogo, String nombreEmisor) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put(DATO_TIENE_LOGO, tieneLogo);
        datos.put(DATO_NOMBRE_EMISOR, nombreEmisor);
        return renderizar(PLANTILLA_LOGO, datos).strip();
    }

    private String envolverEnDocumento(String asunto, String fragmentoHtml) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put(DATO_ASUNTO, asunto);
        datos.put(DATO_CONTENIDO, fragmentoHtml);
        return renderizar(PLANTILLA_DOCUMENTO, datos);
    }

    private static String limpiarSaltosDeLinea(String texto) {
        return texto.replaceAll(SALTOS_DE_LINEA, ESPACIO).trim();
    }

    private String renderizar(String nombrePlantilla, Map<String, Object> datos) {
        Template plantilla = engine.getTemplate(nombrePlantilla);
        if (plantilla == null) {
            throw new IllegalStateException("Falta la plantilla de correo " + nombrePlantilla);
        }
        TemplateInstance instancia = plantilla.instance();
        datos.forEach(instancia::data);
        return instancia.render();
    }
}
