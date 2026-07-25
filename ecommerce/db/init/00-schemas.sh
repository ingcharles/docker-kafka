#!/bin/sh
# Una sola base de datos ("ecommerce", creada automáticamente por la imagen
# oficial de postgres a partir de POSTGRES_DB), un esquema por microservicio,
# un rol de mínimo privilegio por esquema. El rol de un servicio no tiene
# ningún permiso fuera de su propio esquema — el aislamiento entre bounded
# contexts se aplica vía privilegios de Postgres, no teniendo seis bases de
# datos separadas.
set -e

: "${ORDER_DB_PASSWORD:?ORDER_DB_PASSWORD must be set}"
: "${INVENTORY_DB_PASSWORD:?INVENTORY_DB_PASSWORD must be set}"
: "${PAYMENT_DB_PASSWORD:?PAYMENT_DB_PASSWORD must be set}"
: "${SHIPPING_DB_PASSWORD:?SHIPPING_DB_PASSWORD must be set}"
: "${NOTIFICATION_DB_PASSWORD:?NOTIFICATION_DB_PASSWORD must be set}"
: "${SAGA_DB_PASSWORD:?SAGA_DB_PASSWORD must be set}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- "order" es una palabra reservada de SQL, por eso este esquema se
    -- llama "orders"; el esquema de cada uno de los demás servicios es
    -- simplemente su nombre de dominio.
    create schema if not exists orders;
    create schema if not exists inventory;
    create schema if not exists payment;
    create schema if not exists shipping;
    create schema if not exists notification;
    create schema if not exists saga;

    do \$\$
    begin
      if not exists (select from pg_roles where rolname = 'order_service') then
        create role order_service login password '${ORDER_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'inventory_service') then
        create role inventory_service login password '${INVENTORY_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'payment_service') then
        create role payment_service login password '${PAYMENT_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'shipping_service') then
        create role shipping_service login password '${SHIPPING_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'notification_service') then
        create role notification_service login password '${NOTIFICATION_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'saga_service') then
        create role saga_service login password '${SAGA_DB_PASSWORD}';
      end if;
    end
    \$\$;

    grant usage, create on schema orders to order_service;
    grant usage, create on schema inventory to inventory_service;
    grant usage, create on schema payment to payment_service;
    grant usage, create on schema shipping to shipping_service;
    grant usage, create on schema notification to notification_service;
    grant usage, create on schema saga to saga_service;

    -- search_path por defecto por cada rol: la URL JDBC/reactiva propia de
    -- cada servicio se conecta con este rol y nunca necesita calificar el
    -- nombre de una tabla con su esquema, y esto se aplica acá del lado del
    -- servidor en vez de confiar en que la configuración del cliente lo haga bien.
    alter role order_service set search_path = orders;
    alter role inventory_service set search_path = inventory;
    alter role payment_service set search_path = payment;
    alter role shipping_service set search_path = shipping;
    alter role notification_service set search_path = notification;
    alter role saga_service set search_path = saga;
EOSQL
