#!/bin/sh
# Corre en segundo plano en el primario, una vez que Postgres acepta conexiones por TCP (la fase temporal de initdb no
# escucha por TCP, así que no se confunde con ella). Es idempotente: se puede repetir en cada arranque.
set -u

STANZA="${RESPALDO_STANZA:-facturacion}"
PGDATA="${PGDATA:-/var/lib/postgresql/data}"
# rol:variable con su contraseña
ROLES_DE_SERVICIO="inventario_service:INVENTARIO_DB_PASSWORD firma_service:FIRMA_DB_PASSWORD comprobantes_service:COMPROBANTES_DB_PASSWORD sri_service:SRI_DB_PASSWORD notificacion_service:NOTIFICACION_DB_PASSWORD orquestacion_service:ORQUESTACION_DB_PASSWORD identidad_service:IDENTIDAD_DB_PASSWORD"

until pg_isready -h 127.0.0.1 -q; do sleep 2; done

sql() { su-exec postgres psql -U "${POSTGRES_USER:-postgres}" -d postgres -v ON_ERROR_STOP=1 -qtAc "$1"; }

# Por cada rol de servicio: su contraseña sale siempre del archivo de secretos (init/00-schemas.sh solo la fija en el primer
# arranque; así, cambiarla en el archivo y recrear este contenedor basta para rotarla) y sus plazos de sesión
# (valen aunque entre el servicio y Postgres haya un pooler, que descarta los parámetros de arranque de la conexión;
# son los mismos que los servicios mandan al conectarse).
for par in $ROLES_DE_SERVICIO; do
  rol="${par%%:*}"
  variable="${par#*:}"
  if [ "$(sql "select 1 from pg_roles where rolname = '$rol'")" = "1" ]; then
    eval "clave=\${$variable:-}"
    if [ -n "$clave" ]; then
      sql "alter role $rol password '$(printf '%s' "$clave" | sed "s/'/''/g")'"
    fi
    sql "alter role $rol set statement_timeout = '${DB_STATEMENT_TIMEOUT:-30s}'"
    sql "alter role $rol set lock_timeout = '${DB_LOCK_TIMEOUT:-10s}'"
    sql "alter role $rol set idle_in_transaction_session_timeout = '${DB_IDLE_EN_TRANSACCION_TIMEOUT:-60s}'"
  fi
done

if [ "${REPLICACION_HABILITADA:-false}" = "true" ]; then
  usuario="${REPLICACION_DB_USUARIO:-replicador}"
  slot="${REPLICACION_SLOT:-replica_1}"
  if [ -z "${REPLICACION_DB_PASSWORD:-}" ]; then
    echo "[post-arranque] REPLICACION_HABILITADA=true pero falta REPLICACION_DB_PASSWORD: no se crea el usuario de replicación"
  else
    if [ "$(sql "select 1 from pg_roles where rolname = '$usuario'")" != "1" ]; then
      sql "create role $usuario with replication login password '${REPLICACION_DB_PASSWORD}'"
    else
      sql "alter role $usuario with replication login password '${REPLICACION_DB_PASSWORD}'"
    fi
    sql "select pg_create_physical_replication_slot('$slot') where not exists (select 1 from pg_replication_slots where slot_name = '$slot')" > /dev/null
    linea="host replication $usuario all scram-sha-256"
    grep -qxF "$linea" "$PGDATA/pg_hba.conf" \
      || { echo "$linea" >> "$PGDATA/pg_hba.conf"; sql "select pg_reload_conf()" > /dev/null; }
  fi
fi

if [ "${RESPALDO_HABILITADO:-false}" = "true" ]; then
  pgbackrest_stanza() { su-exec postgres pgbackrest --stanza="$STANZA" "$@"; }
  # El repositorio puede tardar en estar listo (MinIO arrancando): se reintenta.
  intentos=0
  until pgbackrest_stanza stanza-create && pgbackrest_stanza check; do
    intentos=$((intentos + 1))
    if [ "$intentos" -ge 30 ]; then
      echo "[post-arranque] No se pudo preparar el repositorio de respaldo tras $intentos intentos"
      exit 1
    fi
    sleep 5
  done
  # Siempre debe existir un respaldo base desde el cual restaurar: si no hay ninguno, se hace uno completo ahora.
  if ! pgbackrest_stanza info --output=json | grep -q '"backup":\[{'; then
    pgbackrest_stanza --type=full backup
  fi
fi
