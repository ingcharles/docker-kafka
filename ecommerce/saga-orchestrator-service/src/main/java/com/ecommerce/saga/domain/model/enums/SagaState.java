package com.ecommerce.saga.domain.model.enums;

/**
 * Ciclo de vida de la saga. Desde el punto de vista del orquestador, la
 * compensación es "disparar y olvidar" (fire-and-forget): cuando un paso
 * falla, se emiten los comandos de compensación y la saga pasa directo a
 * FAILED; el orquestador no espera a que los servicios compensados
 * confirmen antes de registrar el resultado final. Ver
 * saga-orchestrator-service/README.md para la tabla completa de
 * transiciones.
 */
public enum SagaState {
    STARTED,
    STOCK_RESERVED,
    PAYMENT_APPROVED,
    SHIPMENT_CREATED,
    COMPLETED,
    FAILED
}
