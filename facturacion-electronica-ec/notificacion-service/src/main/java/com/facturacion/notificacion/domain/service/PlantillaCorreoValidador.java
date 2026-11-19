package com.facturacion.notificacion.domain.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.notificacion.domain.model.enums.VariableCorreo;
import com.facturacion.notificacion.shared.error.NotificacionError;

/**
 * Controles de lo que una empresa escribe en su plantilla de correo, antes de guardarla. Lanza
 * {@link ErrorNegocioException} (400).
 * <ul>
 *   <li>El asunto va en una sola línea (un salto de línea permitiría agregar encabezados al correo).</li>
 *   <li>Solo se usan las variables de {@link VariableCorreo}; {@code {{logo}}} no va en el asunto.</li>
 *   <li>En el HTML, las variables van en el texto, nunca dentro de una etiqueta ({@code <a href="{{x}}">}): así un valor no
 *       puede cambiar la estructura del HTML.</li>
 * </ul>
 * El largo máximo lo comprueba la validación del DTO (ver {@code PlantillaCorreoEmpresa}).
 */
public final class PlantillaCorreoValidador {

    /** Una etiqueta HTML que contiene {@code {{...}}} dentro de ella. */
    private static final Pattern VARIABLE_EN_ETIQUETA = Pattern.compile("<[^<>]*\\{\\{[^<>]*>");

    private PlantillaCorreoValidador() {
    }

    public static void validar(String asunto, String cuerpoHtml, String cuerpoTexto) {
        if (asunto.indexOf('\n') >= 0 || asunto.indexOf('\r') >= 0) {
            throw new ErrorNegocioException(NotificacionError.PLANTILLA_ASUNTO_MULTILINEA);
        }
        validarVariables(asunto, false);
        validarVariables(cuerpoHtml, true);
        if (cuerpoTexto != null) {
            validarVariables(cuerpoTexto, true);
        }
        Matcher enEtiqueta = VARIABLE_EN_ETIQUETA.matcher(cuerpoHtml);
        if (enEtiqueta.find()) {
            throw new ErrorNegocioException(NotificacionError.PLANTILLA_VARIABLE_EN_ETIQUETA,
                SustitutoVariables.listarNombres(enEtiqueta.group()).get(0));
        }
    }

    private static void validarVariables(String texto, boolean admiteLogo) {
        List<String> nombres = SustitutoVariables.listarNombres(texto);
        for (String nombre : nombres) {
            VariableCorreo variable = VariableCorreo.obtenerPorNombre(nombre).orElseThrow(
                () -> new ErrorNegocioException(NotificacionError.PLANTILLA_VARIABLE_DESCONOCIDA, nombre,
                    VariableCorreo.listarMarcadores()));
            if (variable == VariableCorreo.LOGO && !admiteLogo) {
                throw new ErrorNegocioException(NotificacionError.PLANTILLA_LOGO_EN_ASUNTO, variable.marcador());
            }
        }
    }
}
