#!/usr/bin/env bash
# Ensayo de restauración a un instante exacto (PITR). Un respaldo que nunca se restauró no es un respaldo.
#
# Qué hace, sin tocar los datos del ambiente salvo una tabla de una fila (public.prueba_respaldo):
#   1. escribe la fila "antes" y toma un respaldo diferencial;
#   2. anota la hora, escribe la fila "despues" y fuerza el archivado del WAL;
#   3. restaura el repositorio en un contenedor desechable hasta la hora anotada;
#   4. comprueba que solo existe la fila "antes" y que las tablas de los servicios tienen las mismas filas que el original.
#
# Uso: ./scripts/probar-restauracion.sh <desarrollo|pruebas|produccion>
set -euo pipefail

cd "$(dirname "$0")/.."

ambiente="${1:-}"
case "$ambiente" in
  desarrollo|pruebas|produccion) ;;
  *) echo "Uso: $0 <desarrollo|pruebas|produccion>" >&2; exit 2 ;;
esac

config="../facturacion-electronica-ec/env/${ambiente}.env"
secretos="../facturacion-electronica-ec/env/${ambiente}.secretos.env"
valor() { grep -hE "^$1=" "$config" "$secretos" 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true; }
contenedor="$(valor DB_HOST)"; contenedor="${contenedor:-facturacion-db}"
stanza="$(valor RESPALDO_STANZA)"; stanza="${stanza:-facturacion}"
imagen="$(valor DB_IMAGEN)"; imagen="${imagen:-facturacion-postgres:16}"
base="$(valor POSTGRES_DB)"; base="${base:-facturacion}"
usuario="$(valor POSTGRES_USER)"; usuario="${usuario:-postgres}"
temporal="restauracion-prueba-$$"
volumen="restauracion_prueba_$$"
registro="$(mktemp)"

export MSYS_NO_PATHCONV=1
limpiar() {
  docker rm -f "$temporal" > /dev/null 2>&1 || true
  docker volume rm "$volumen" > /dev/null 2>&1 || true
  rm -f "$registro"
  docker exec "$contenedor" psql -U "$usuario" -d "$base" -qc "drop table if exists public.prueba_respaldo" > /dev/null 2>&1 || true
}
trap limpiar EXIT

psql_origen() { docker exec "$contenedor" psql -U "$usuario" -d "$base" -qtA "$@"; }

echo "== 1. fila 'antes' y respaldo diferencial"
psql_origen -c "drop table if exists public.prueba_respaldo; create table public.prueba_respaldo(nota text, ts timestamptz default clock_timestamp()); insert into public.prueba_respaldo(nota) values ('antes')" > /dev/null
docker exec -u postgres "$contenedor" pgbackrest --stanza="$stanza" --type=diff backup 2>&1 | grep -E "backup command end|new backup label|backup size" || true

echo "== 2. instante objetivo, fila 'despues' y archivado del WAL"
sleep 2
objetivo="$(psql_origen -c "select to_char(clock_timestamp() at time zone 'UTC', 'YYYY-MM-DD HH24:MI:SS') || '+00'")"
sleep 2
psql_origen -c "insert into public.prueba_respaldo(nota) values ('despues')" > /dev/null
psql_origen -c "select pg_switch_wal()" > /dev/null
docker exec -u postgres "$contenedor" pgbackrest --stanza="$stanza" check > /dev/null
echo "objetivo: $objetivo"

# Filas de las tablas de los servicios en el original (el ensayo compara contra esto).
consulta_filas="select string_agg(c, ',' order by c) from (select 'comprobantes=' || count(*) c from comprobantes.comprobante union all select 'sagas=' || count(*) from orquestacion.saga_instance union all select 'identidad_usuarios=' || count(*) from identidad.usuario) t"
filas_origen="$(psql_origen -c "$consulta_filas" 2>/dev/null || echo 'n/d')"
max_conexiones="$(psql_origen -c "show max_connections")"

echo "== 3. restaurando en un contenedor desechable hasta ese instante"
docker volume create "$volumen" > /dev/null
# Sin bind mounts (en Windows el origen sería una ruta del anfitrión): la configuración pasa de un contenedor a otro por tar.
docker create --name "$temporal" --network facturacion_net --user root --entrypoint sh \
  -v "$volumen:/var/lib/postgresql/data" "$imagen" -c "
    set -e
    mkdir -p /var/log/pgbackrest && chown postgres /var/log/pgbackrest
    chown postgres:postgres /var/lib/postgresql/data && chmod 700 /var/lib/postgresql/data
    su-exec postgres pgbackrest --stanza=$stanza --type=time --target='$objetivo' --target-action=promote --delta restore
    su-exec postgres pg_ctl -D /var/lib/postgresql/data -w -t 120 start -o '-c listen_addresses= -c archive_mode=off -c hot_standby=on -c max_connections=$max_conexiones'
    # La promoción termina cuando ya no está en recuperación.
    until [ \"\$(su-exec postgres psql -U $usuario -d $base -qtAc 'select pg_is_in_recovery()')\" = 'f' ]; do sleep 1; done
    echo NOTAS=\$(su-exec postgres psql -U $usuario -d $base -qtAc \"select string_agg(nota, ',') from public.prueba_respaldo\")
    echo FILAS=\$(su-exec postgres psql -U $usuario -d $base -qtAc \"$consulta_filas\")
    su-exec postgres pg_ctl -D /var/lib/postgresql/data -m fast stop > /dev/null
  " > /dev/null
docker cp "$contenedor:/etc/pgbackrest/pgbackrest.conf" - | docker cp - "$temporal:/etc/pgbackrest/"
if ! docker start -a "$temporal" > "$registro" 2>&1; then
  echo "La restauración falló; salida del contenedor:" >&2
  tail -40 "$registro" >&2
  exit 1
fi
grep -E "^(NOTAS|FILAS)=|restore command end" "$registro" || true

notas="$(grep "^NOTAS=" "$registro" | cut -d= -f2-)"
filas="$(grep "^FILAS=" "$registro" | cut -d= -f2-)"

echo "== 4. resultado"
echo "  filas en el original : $filas_origen"
echo "  filas restauradas    : $filas"
echo "  notas restauradas    : $notas (esperado: antes)"
if [ "$notas" = "antes" ] && [ "$filas" = "$filas_origen" ]; then
  echo "RESTAURACION OK: la base volvió exactamente al instante $objetivo"
else
  echo "RESTAURACION FALLIDA" >&2
  exit 1
fi
