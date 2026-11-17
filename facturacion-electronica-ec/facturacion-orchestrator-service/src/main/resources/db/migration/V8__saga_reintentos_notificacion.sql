-- Contador de reenvios AUTOMATICOS del correo (job ReenvioNotificacionesJob): cuantas veces el job devolvio la saga de
-- NOTIFICACION_FALLIDA a AUTORIZADO para pedir otro envio. Es independiente de "reintentos" (el del watchdog de firma/SRI)
-- y los reenvios manuales por REST no lo cuentan. Al llegar al maximo configurado el job ya no la toca: queda en
-- NOTIFICACION_FALLIDA para que una persona la reenvie por REST. Corre contra el esquema "orquestacion".
alter table saga_instance add column reintentos_notificacion integer not null default 0;

-- El job busca solo estas dos clases de saga, las mas antiguas primero: el indice parcial evita recorrer todas las demas.
create index idx_saga_instance_notificacion_pendiente on saga_instance (updated_at)
    where state in ('AUTORIZADO', 'NOTIFICACION_FALLIDA');

comment on column saga_instance.reintentos_notificacion is 'Reenvios automaticos del correo hechos por el job (maximo saga.reenvio-notificaciones.max-intentos); los reenvios manuales por REST no cuentan.';
