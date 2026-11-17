package com.facturacion.orquestacion.application.dto;

/** Resultado de pedir el reenvío masivo de correos: cuántas sagas en NOTIFICACION_FALLIDA se mandaron a reenviar. */
public record ReenvioMasivoResponse(int solicitados) {
}
