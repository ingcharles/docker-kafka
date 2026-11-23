package com.facturacion.notificacion.infrastructure.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.quarkus.mailer.Attachment;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.vo.LogoEmpresa;
import com.facturacion.notificacion.domain.port.CorreoSalida;

/**
 * Lo que sale por SMTP: HTML con alternativa en texto, el RIDE y el XML adjuntos, el logo incrustado solo si el HTML lo usa, y
 * un SMTP que no contesta no cuelga el consumidor (el envío falla al vencer el plazo configurado).
 */
class CorreoServiceTest {

    private static final Duration PLAZO = Duration.ofMillis(200);
    private static final String CLAVE = "1811202601172203995300110010010000001016330585816";
    private static final LogoEmpresa LOGO = new LogoEmpresa(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 1}, "image/png");

    private final List<Mail> enviados = new ArrayList<>();

    /** Mailer de prueba: o contesta enseguida o no contesta nunca (un SMTP colgado). */
    private CorreoService servicio(boolean contesta) {
        ReactiveMailer mailer = (ReactiveMailer) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {ReactiveMailer.class}, (proxy, metodo, args) -> {
                if (!metodo.getName().equals("send")) {
                    throw new UnsupportedOperationException(metodo.getName());
                }
                for (Object mail : (Mail[]) args[0]) {
                    enviados.add((Mail) mail);
                }
                return contesta ? Uni.createFrom().voidItem() : Uni.createFrom().nothing();
            });
        CorreoService servicio = new CorreoService(mailer, new MailRateLimiter(0),
            new PlantillaCorreo(MotorPlantillasDePrueba.crear()));
        servicio.remitenteDefecto = "facturacion@ejemplo.test";
        servicio.timeoutEnvio = PLAZO;
        return servicio;
    }

    private Uni<Void> enviar(CorreoService servicio, CorreoSalida.Ajustes ajustes) {
        return servicio.enviarComprobante(new CorreoSalida.Comprobante("receptor@ejemplo.test", CLAVE, "FACTURA",
            "Ana Pérez", new BigDecimal("11.50"), new byte[] {1, 2, 3}, "<autorizacion/>", ajustes));
    }

    private Mail enviarYObtener(CorreoSalida.Ajustes ajustes) {
        enviar(servicio(true), ajustes).await().atMost(Duration.ofSeconds(5));
        return enviados.get(0);
    }

    private static List<Attachment> incrustados(Mail correo) {
        return correo.getAttachments().stream().filter(Attachment::isInlineAttachment).toList();
    }

    @Test
    void testDeberiaEnviarElCorreoSiElSmtpContesta() {
        Mail correo = enviarYObtener(CorreoSalida.Ajustes.ninguno());

        assertEquals(1, enviados.size());
        assertEquals(2, correo.getAttachments().size());
        assertTrue(incrustados(correo).isEmpty());
    }

    @Test
    void testDeberiaEnviarEnUnSoloMensajeAVariosDestinatarios() {
        servicio(true).enviarComprobante(new CorreoSalida.Comprobante("uno@ejemplo.test,dos@ejemplo.test", CLAVE,
            "FACTURA", "Ana Pérez", new BigDecimal("11.50"), new byte[] {1}, "<autorizacion/>",
            CorreoSalida.Ajustes.ninguno())).await().atMost(Duration.ofSeconds(5));

        assertEquals(1, enviados.size());
        assertEquals(List.of("uno@ejemplo.test", "dos@ejemplo.test"), enviados.get(0).getTo());
    }

    @Test
    void testDeberiaArmarElCorreoConHtmlAlternativaEnTextoYElAsuntoDeLaPlantilla() {
        Mail correo = enviarYObtener(CorreoSalida.Ajustes.ninguno());

        assertEquals("Factura 001-001-000000101", correo.getSubject());
        assertTrue(correo.getHtml().contains("<html"));
        assertTrue(correo.getText().contains("Clave de acceso: " + CLAVE));
        assertNull(correo.getFrom());
    }

    @Test
    void testDeberiaDarPrioridadAlAsuntoYAlRemitenteDeLaEmpresa() {
        Mail correo = enviarYObtener(new CorreoSalida.Ajustes("Su factura de Tienda", "Tienda Ejemplo", "Tienda Ejemplo",
            null, null));

        assertEquals("Su factura de Tienda", correo.getSubject());
        assertEquals("Tienda Ejemplo <facturacion@ejemplo.test>", correo.getFrom());
        assertTrue(correo.getHtml().contains("Enviado por Tienda Ejemplo."));
    }

    @Test
    void testDeberiaIncrustarLaImagenDelLogoEnElCorreo() {
        Mail correo = enviarYObtener(new CorreoSalida.Ajustes(null, null, "Tienda Ejemplo", LOGO, null));

        assertEquals(3, correo.getAttachments().size());
        List<Attachment> logos = incrustados(correo);
        assertEquals(1, logos.size());
        assertEquals("<" + LogoEmpresa.CID + ">", logos.get(0).getContentId());
        assertEquals("image/png", logos.get(0).getContentType());
        assertTrue(correo.getHtml().contains("cid:" + LogoEmpresa.CID));
    }

    @Test
    void testDeberiaNoIncrustarNadaYMostrarElNombreEnElEncabezadoSinLogo() {
        Mail correo = enviarYObtener(new CorreoSalida.Ajustes(null, null, "Tienda Ejemplo", null, null));

        assertTrue(incrustados(correo).isEmpty());
        assertTrue(!correo.getHtml().contains("cid:"));
    }

    @Test
    void testDeberiaNoIncrustarElLogoSiLaPlantillaDeLaEmpresaNoLoUsaAunqueLoTenga() {
        PlantillaCorreoEmpresa propia = PlantillaCorreoEmpresa.crear("1722039953001", TipoComprobanteCorreo.FACTURA,
            "Asunto", "<p>Hola {{nombreReceptor}}</p>", null, true, Instant.now());

        Mail correo = enviarYObtener(new CorreoSalida.Ajustes(null, null, "Tienda", LOGO, propia));

        assertTrue(incrustados(correo).isEmpty());
        assertEquals(2, correo.getAttachments().size());
        assertEquals("Asunto", correo.getSubject());
    }

    @Test
    void testDeberiaIncrustarElLogoSiLaPlantillaDeLaEmpresaLoUsa() {
        PlantillaCorreoEmpresa propia = PlantillaCorreoEmpresa.crear("1722039953001", TipoComprobanteCorreo.FACTURA,
            "Asunto", "<div>{{logo}}</div>", null, true, Instant.now());

        Mail correo = enviarYObtener(new CorreoSalida.Ajustes(null, null, "Tienda", LOGO, propia));

        assertEquals(1, incrustados(correo).size());
    }

    @Test
    void testDeberiaFallarElEnvioAlVencerElPlazoSiElSmtpNoContesta() {
        CompletionException e = assertThrows(CompletionException.class, () -> enviar(servicio(false),
            CorreoSalida.Ajustes.ninguno()).subscribeAsCompletionStage().join());

        assertTrue(e.getCause() instanceof TimeoutException, e.getCause().toString());
    }
}
