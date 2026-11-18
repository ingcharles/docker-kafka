package com.facturacion.notificacion.infrastructure.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

/** Un SMTP que no contesta no cuelga el consumidor: el envío falla al vencer el plazo configurado. */
class CorreoServiceTest {

    private static final Duration PLAZO = Duration.ofMillis(200);

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
        CorreoService servicio = new CorreoService(mailer, new MailRateLimiter(0));
        servicio.remitenteDefecto = "facturacion@ejemplo.test";
        servicio.timeoutEnvio = PLAZO;
        return servicio;
    }

    private Uni<Void> enviar(CorreoService servicio) {
        return servicio.enviarComprobante("receptor@ejemplo.test", "1811202601172203995300110010010000001016330585816",
            "FACTURA", new byte[] {1, 2, 3}, "<autorizacion/>", null, null);
    }

    @Test
    void siElSmtpContestaElCorreoSale() {
        enviar(servicio(true)).await().atMost(Duration.ofSeconds(5));

        assertEquals(1, enviados.size());
        assertEquals(2, enviados.get(0).getAttachments().size());
    }

    @Test
    void siElSmtpNoContestaElEnvioFallaAlVencerElPlazo() {
        CompletionException e = assertThrows(CompletionException.class, () -> enviar(servicio(false))
            .subscribeAsCompletionStage().join());

        assertTrue(e.getCause() instanceof TimeoutException, e.getCause().toString());
    }
}
