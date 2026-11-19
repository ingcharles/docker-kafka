package com.facturacion.notificacion.infrastructure.mail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import io.quarkus.qute.Engine;
import io.quarkus.qute.HtmlEscaper;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.ValueResolvers;
import io.quarkus.qute.Variant;

/**
 * Motor Qute para las pruebas unitarias: lee las plantillas reales de {@code src/main/resources/templates} y les da el
 * tipo de contenido por su extensión, igual que Quarkus (así el HTML se escapa y el texto no).
 */
final class MotorPlantillasDePrueba {

    private static final String RAIZ = "templates/";
    private static final String EXTENSION_HTML = ".html";

    private MotorPlantillasDePrueba() {
    }

    static Engine crear() {
        return Engine.builder()
            .addDefaults()
            .addValueResolver(ValueResolvers.rawResolver())
            .addResultMapper(new HtmlEscaper(List.of(Variant.TEXT_HTML)))
            .addLocator(MotorPlantillasDePrueba::localizar)
            .build();
    }

    private static Optional<TemplateLocator.TemplateLocation> localizar(String id) {
        InputStream recurso = MotorPlantillasDePrueba.class.getClassLoader().getResourceAsStream(RAIZ + id);
        if (recurso == null) {
            return Optional.empty();
        }
        Variant variante = Variant.forContentType(id.endsWith(EXTENSION_HTML) ? Variant.TEXT_HTML : Variant.TEXT_PLAIN);
        return Optional.of(new TemplateLocator.TemplateLocation() {
            @Override
            public Reader read() {
                return new BufferedReader(new InputStreamReader(recurso, StandardCharsets.UTF_8));
            }

            @Override
            public Optional<Variant> getVariant() {
                return Optional.of(variante);
            }
        });
    }
}
