-- Contador de reintentos automáticos del watchdog de sagas (SagaWatchdog): cuántas veces se volvió a publicar
-- el último comando de una saga atascada. Al llegar al máximo configurado y vencer el último plazo, la saga
-- pasa a EXPIRADA. Corre contra el esquema "orquestacion".
alter table saga_instance add column reintentos integer not null default 0;
