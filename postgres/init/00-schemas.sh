#!/bin/sh
# Una sola base de datos ("facturacion", creada automáticamente por la imagen
# oficial de postgres a partir de POSTGRES_DB), un esquema por microservicio,
# un rol de mínimo privilegio por esquema. El rol de un servicio no tiene
# ningún permiso fuera de su propio esquema — aislamiento entre bounded
# contexts aplicado por privilegios de Postgres, no por tener seis bases
# de datos separadas. Mismo patrón que ecommerce/db/init/00-schemas.sh.
set -e

: "${INVENTARIO_DB_PASSWORD:?INVENTARIO_DB_PASSWORD must be set}"
: "${FIRMA_DB_PASSWORD:?FIRMA_DB_PASSWORD must be set}"
: "${COMPROBANTES_DB_PASSWORD:?COMPROBANTES_DB_PASSWORD must be set}"
: "${SRI_DB_PASSWORD:?SRI_DB_PASSWORD must be set}"
: "${NOTIFICACION_DB_PASSWORD:?NOTIFICACION_DB_PASSWORD must be set}"
: "${ORQUESTACION_DB_PASSWORD:?ORQUESTACION_DB_PASSWORD must be set}"
: "${IDENTIDAD_DB_PASSWORD:?IDENTIDAD_DB_PASSWORD must be set}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    create schema if not exists inventario;
    create schema if not exists firma_electronica;
    create schema if not exists comprobantes;
    create schema if not exists sri;
    create schema if not exists notificacion;
    create schema if not exists orquestacion;
    create schema if not exists identidad;

    do \$\$
    begin
      if not exists (select from pg_roles where rolname = 'inventario_service') then
        create role inventario_service login password '${INVENTARIO_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'firma_service') then
        create role firma_service login password '${FIRMA_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'comprobantes_service') then
        create role comprobantes_service login password '${COMPROBANTES_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'sri_service') then
        create role sri_service login password '${SRI_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'notificacion_service') then
        create role notificacion_service login password '${NOTIFICACION_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'orquestacion_service') then
        create role orquestacion_service login password '${ORQUESTACION_DB_PASSWORD}';
      end if;
      if not exists (select from pg_roles where rolname = 'identidad_service') then
        create role identidad_service login password '${IDENTIDAD_DB_PASSWORD}';
      end if;
    end
    \$\$;

    grant usage, create on schema inventario to inventario_service;
    grant usage, create on schema firma_electronica to firma_service;
    grant usage, create on schema comprobantes to comprobantes_service;
    grant usage, create on schema sri to sri_service;
    grant usage, create on schema notificacion to notificacion_service;
    grant usage, create on schema orquestacion to orquestacion_service;
    grant usage, create on schema identidad to identidad_service;

    -- search_path por defecto por cada rol: la URL JDBC/reactiva propia de
    -- cada servicio se conecta con este rol y nunca necesita calificar el
    -- nombre de una tabla con su esquema.
    alter role inventario_service set search_path = inventario;
    alter role firma_service set search_path = firma_electronica;
    alter role comprobantes_service set search_path = comprobantes;
    alter role sri_service set search_path = sri;
    alter role notificacion_service set search_path = notificacion;
    alter role orquestacion_service set search_path = orquestacion;
    alter role identidad_service set search_path = identidad;
EOSQL
