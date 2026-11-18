package com.facturacion.commons.messaging;

import java.lang.reflect.Method;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.CodigoLogComun;
import com.facturacion.commons.error.LogCodificado;

/**
 * Registra con código ({@code GEN-402}) la falla de un consumidor de Kafka: el canal, la clase de la excepción y los ids
 * del mensaje ({@code eventId}, {@code comprobanteId}, si los tiene). No incluye el contenido del mensaje ni el texto de
 * la excepción, que pueden llevar datos del comprobante. Deja pasar la falla intacta.
 */
@LogFallosMensajeria
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class LogFallosMensajeriaInterceptor {

    private static final Logger LOG = Logger.getLogger(LogFallosMensajeriaInterceptor.class);

    @AroundInvoke
    public Object registrar(InvocationContext contexto) throws Exception {
        Object resultado;
        try {
            resultado = contexto.proceed();
        } catch (Exception falla) {
            registrarFalla(contexto, falla);
            throw falla;
        }
        if (resultado instanceof Uni<?> uni) {
            return uni.onFailure().invoke(falla -> registrarFalla(contexto, falla));
        }
        return resultado;
    }

    private static void registrarFalla(InvocationContext contexto, Throwable falla) {
        LogCodificado.error(LOG, CodigoLogComun.MENSAJE_FALLIDO, extraerIdsDelMensaje(contexto.getParameters()),
            obtenerCanal(contexto.getMethod()), falla.getClass().getSimpleName());
    }

    static String obtenerCanal(Method metodo) {
        Incoming incoming = metodo == null ? null : metodo.getAnnotation(Incoming.class);
        return incoming == null ? "desconocido" : incoming.value();
    }

    /** {@code eventId=…, comprobanteId=…} del primer parámetro, si es un mensaje que los expone; vacío si no. */
    static String extraerIdsDelMensaje(Object[] parametros) {
        if (parametros == null || parametros.length == 0 || parametros[0] == null) {
            return null;
        }
        StringBuilder ids = new StringBuilder();
        for (String nombre : new String[] {"eventId", "comprobanteId"}) {
            try {
                Object valor = parametros[0].getClass().getMethod(nombre).invoke(parametros[0]);
                if (valor != null) {
                    ids.append(ids.length() == 0 ? "" : ", ").append(nombre).append('=').append(valor);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                // El mensaje no expone ese id: se omite.
            }
        }
        return ids.length() == 0 ? null : ids.toString();
    }
}
