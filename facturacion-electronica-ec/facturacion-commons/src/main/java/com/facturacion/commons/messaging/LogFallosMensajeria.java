package com.facturacion.commons.messaging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * Marca un consumidor de Kafka ({@code @Incoming}) para que, si falla, se registre un log con código
 * ({@code GEN-402}, canal y ids del mensaje) antes de que el mensaje pase a la DLQ o se descarte según la estrategia de
 * fallos del canal. La falla se vuelve a lanzar: este registro no cambia lo que pasa con el mensaje.
 * Todo {@code @Incoming} debe llevar esta anotación.
 */
@Documented
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LogFallosMensajeria {
}
