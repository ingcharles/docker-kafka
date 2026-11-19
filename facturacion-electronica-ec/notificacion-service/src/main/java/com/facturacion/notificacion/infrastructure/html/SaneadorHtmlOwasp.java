package com.facturacion.notificacion.infrastructure.html;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import com.facturacion.notificacion.domain.port.SaneadorHtml;

/**
 * Limpia el HTML de las plantillas con OWASP Java HTML Sanitizer. La política es una lista de lo permitido (no de lo
 * prohibido), pensada para correo:
 * <ul>
 *   <li>Etiquetas de texto, listas, tablas, enlaces, imágenes y separadores. Fuera quedan {@code script}, {@code style},
 *       {@code iframe}, {@code form}, {@code input}, {@code object}, {@code svg}, {@code meta}, {@code link},
 *       {@code html}/{@code head}/{@code body} y toda etiqueta que no esté en la lista: se quita la etiqueta y se conserva
 *       su texto (salvo script y style, que se quitan enteros).</li>
 *   <li>Ningún atributo de evento ({@code onclick}, {@code onerror}...).</li>
 *   <li>{@code style} en línea con propiedades y valores de la lista del sanitizador; sin URL dentro del CSS.</li>
 *   <li>Direcciones solo {@code https}, {@code mailto} y {@code cid} (la imagen incrustada del logo): nunca
 *       {@code javascript:}, {@code data:} ni {@code http:}.</li>
 *   <li>Enlaces con {@code rel="nofollow noopener noreferrer"}.</li>
 * </ul>
 * Dos ajustes para que el resultado sirva de plantilla: las variables {@code {{nombre}}} se conservan tal cual (la librería
 * parte {@code {{} con un comentario para frenar plantillas del navegador, que aquí no existen) y {@code @}, {@code +} y
 * {@code =} no se dejan como entidades numéricas (son inofensivos y así {@code cid:logo@facturacion} se reconoce).
 */
@ApplicationScoped
public class SaneadorHtmlOwasp implements SaneadorHtml {

    private static final String[] ETIQUETAS = {
        "p", "br", "div", "span", "hr", "blockquote", "pre", "code", "center",
        "h1", "h2", "h3", "h4", "h5", "h6",
        "strong", "b", "em", "i", "u", "s", "small", "sub", "sup", "font",
        "ul", "ol", "li",
        "table", "thead", "tbody", "tfoot", "tr", "td", "th", "caption",
        "a", "img"
    };

    /** Atributos de presentación y accesibilidad que sirven en cualquier etiqueta permitida. */
    private static final String[] ATRIBUTOS_GENERALES = {
        "align", "valign", "bgcolor", "width", "height", "border", "cellpadding", "cellspacing", "colspan", "rowspan",
        "role", "dir", "lang", "title", "alt", "color", "face", "size"
    };

    private static final String[] PROTOCOLOS_PERMITIDOS = {"https", "mailto", "cid"};

    private static final PolicyFactory POLITICA = new HtmlPolicyBuilder()
        .allowElements(ETIQUETAS)
        .allowAttributes(ATRIBUTOS_GENERALES).globally()
        .allowAttributes("href").onElements("a")
        .allowAttributes("src").onElements("img")
        .allowUrlProtocols(PROTOCOLOS_PERMITIDOS)
        .allowWithoutAttributes("span", "font", "a")
        .allowStyling()
        .requireRelsOnLinks("nofollow", "noopener", "noreferrer")
        .toFactory();

    /** Una variable {@code {{...}}} (sin llaves ni signos de etiqueta adentro). */
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{[^{}<>]*}}");
    /** Lo que reemplaza a cada variable mientras se limpia: letras y dígitos, que la librería no toca. */
    private static final String PREFIJO_MARCA = "ZZVAR";
    private static final String SUFIJO_MARCA = "ZZ";
    private static final String NUMERO_DE_MARCA = "(\\d+)";
    private static final int BASE_DEL_AZAR = 36;

    @Override
    public String sanear(String html) {
        List<String> variables = new ArrayList<>();
        String prefijo = elegirPrefijoDeMarca(html);
        String limpio = POLITICA.sanitize(proteger(html, variables, prefijo));
        return restaurarCaracteres(restaurar(limpio, variables, prefijo));
    }

    /** Un prefijo que no aparece en el HTML, para que un texto escrito por la empresa nunca se confunda con una marca. */
    private static String elegirPrefijoDeMarca(String html) {
        String prefijo = PREFIJO_MARCA;
        while (html.contains(prefijo)) {
            prefijo = PREFIJO_MARCA + Long.toString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE), BASE_DEL_AZAR);
        }
        return prefijo;
    }

    private static String proteger(String html, List<String> variables, String prefijo) {
        Matcher coincidencia = VARIABLE.matcher(html);
        StringBuilder protegido = new StringBuilder();
        while (coincidencia.find()) {
            variables.add(coincidencia.group());
            coincidencia.appendReplacement(protegido, prefijo + (variables.size() - 1) + SUFIJO_MARCA);
        }
        coincidencia.appendTail(protegido);
        return protegido.toString();
    }

    private static String restaurar(String html, List<String> variables, String prefijo) {
        Matcher coincidencia = Pattern.compile(Pattern.quote(prefijo) + NUMERO_DE_MARCA + SUFIJO_MARCA).matcher(html);
        StringBuilder restaurado = new StringBuilder();
        while (coincidencia.find()) {
            int posicion = Integer.parseInt(coincidencia.group(1));
            String original = posicion < variables.size() ? variables.get(posicion) : coincidencia.group();
            coincidencia.appendReplacement(restaurado, Matcher.quoteReplacement(original));
        }
        coincidencia.appendTail(restaurado);
        return restaurado.toString();
    }

    private static String restaurarCaracteres(String html) {
        return html.replace("&#64;", "@").replace("&#43;", "+").replace("&#61;", "=");
    }
}
