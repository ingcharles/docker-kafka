package com.facturacion.commons.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.UUID;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

class LogFallosMensajeriaInterceptorTest {

    record Mensaje(UUID eventId, UUID comprobanteId, String xml) {
    }

    @Incoming("canal-de-prueba")
    public Uni<Void> consumidor(Mensaje mensaje) {
        return Uni.createFrom().voidItem();
    }

    private static Method metodo() throws NoSuchMethodException {
        return LogFallosMensajeriaInterceptorTest.class.getMethod("consumidor", Mensaje.class);
    }

    private static InvocationContext contexto(Object[] parametros, Object resultado, Exception falla) throws Exception {
        Method metodo = metodo();
        return (InvocationContext) Proxy.newProxyInstance(LogFallosMensajeriaInterceptorTest.class.getClassLoader(),
            new Class<?>[] {InvocationContext.class}, (proxy, m, args) -> switch (m.getName()) {
                case "proceed" -> {
                    if (falla != null) {
                        throw falla;
                    }
                    yield resultado;
                }
                case "getMethod" -> metodo;
                case "getParameters" -> parametros;
                default -> null;
            });
    }

    @Test
    void elCanalSaleDeLaAnotacionIncoming() throws Exception {
        assertEquals("canal-de-prueba", LogFallosMensajeriaInterceptor.obtenerCanal(metodo()));
        assertEquals("desconocido", LogFallosMensajeriaInterceptor.obtenerCanal(null));
    }

    @Test
    void elContextoLlevaSoloLosIdsDelMensajeYNuncaSuContenido() {
        UUID evento = UUID.randomUUID();
        UUID comprobante = UUID.randomUUID();

        String ids = LogFallosMensajeriaInterceptor.extraerIdsDelMensaje(
            new Object[] {new Mensaje(evento, comprobante, "<xml-con-datos/>")});

        assertEquals("eventId=" + evento + ", comprobanteId=" + comprobante, ids);
    }

    @Test
    void unMensajeSinIdsOSinParametrosNoDaContexto() {
        assertNull(LogFallosMensajeriaInterceptor.extraerIdsDelMensaje(new Object[] {"texto"}));
        assertNull(LogFallosMensajeriaInterceptor.extraerIdsDelMensaje(new Object[0]));
        assertNull(LogFallosMensajeriaInterceptor.extraerIdsDelMensaje(null));
    }

    @Test
    void unaFallaAsincronaSeRegistraYSeVuelveALanzarIntacta() throws Exception {
        IllegalStateException falla = new IllegalStateException("boom");
        Uni<Void> original = Uni.createFrom().failure(falla);

        @SuppressWarnings("unchecked")
        Uni<Void> envuelto = (Uni<Void>) new LogFallosMensajeriaInterceptor().registrar(
            contexto(new Object[] {new Mensaje(UUID.randomUUID(), UUID.randomUUID(), "x")}, original, null));

        IllegalStateException recibida = assertThrows(IllegalStateException.class,
            () -> envuelto.await().atMost(Duration.ofSeconds(2)));
        assertSame(falla, recibida);
    }

    @Test
    void unaFallaSincronaSeVuelveALanzarIntacta() throws Exception {
        IllegalArgumentException falla = new IllegalArgumentException("sin ids");

        IllegalArgumentException recibida = assertThrows(IllegalArgumentException.class,
            () -> new LogFallosMensajeriaInterceptor().registrar(contexto(new Object[] {null}, null, falla)));

        assertSame(falla, recibida);
    }

    @Test
    void sinFallaDevuelveElResultadoSinTocarlo() throws Exception {
        Object resultado = new LogFallosMensajeriaInterceptor()
            .registrar(contexto(new Object[] {null}, "ok", null));

        assertTrue("ok".equals(resultado));
    }
}
