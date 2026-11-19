#!/usr/bin/env bash
# Operaciones del respaldo continuo (pgBackRest) sobre el contenedor de Postgres de un ambiente.
#
# Uso: ./scripts/respaldo.sh <desarrollo|pruebas|produccion> <acción>
#   estado        respaldos existentes, WAL archivado y tamaño (pgbackrest info)
#   completo      respaldo completo ahora
#   diferencial   cambios desde el último completo
#   incremental   cambios desde el último respaldo de cualquier tipo
#   verificar     comprueba que el archivo de WAL y el repositorio responden (pgbackrest check)
#   comprobar     revisa la integridad de todos los respaldos y WAL del repositorio (pgbackrest verify; puede tardar)
#   expirar       aplica la retención ahora (la hace sola después de cada respaldo)
#   edad          segundos desde el último respaldo (para alertar si pasan de ~26 horas)
# Los respaldos programados corren solos (RESPALDO_CRON_COMPLETO / RESPALDO_CRON_DIFERENCIAL en env/<ambiente>.env).
set -euo pipefail

cd "$(dirname "$0")/.."

ambiente="${1:-}"
accion="${2:-estado}"
case "$ambiente" in
  desarrollo|pruebas|produccion) ;;
  *) echo "Uso: $0 <desarrollo|pruebas|produccion> <estado|completo|diferencial|incremental|verificar|comprobar|expirar|edad>" >&2; exit 2 ;;
esac

config="../facturacion-electronica-ec/env/${ambiente}.env"
secretos="../facturacion-electronica-ec/env/${ambiente}.secretos.env"
valor() { grep -hE "^$1=" "$config" "$secretos" 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true; }
contenedor="$(valor DB_HOST)"
contenedor="${contenedor:-facturacion-db}"
stanza="$(valor RESPALDO_STANZA)"
stanza="${stanza:-facturacion}"

pgbackrest() { MSYS_NO_PATHCONV=1 docker exec -u postgres "$contenedor" pgbackrest --stanza="$stanza" "$@"; }

case "$accion" in
  estado) pgbackrest info ;;
  completo) pgbackrest --type=full backup ;;
  diferencial) pgbackrest --type=diff backup ;;
  incremental) pgbackrest --type=incr backup ;;
  verificar) pgbackrest check && echo "OK: el archivo de WAL y el repositorio responden" ;;
  comprobar) pgbackrest verify ;;
  expirar) pgbackrest expire ;;
  edad)
    fin="$(pgbackrest info --output=json | grep -o '"stop":[0-9]*' | tail -1 | cut -d: -f2)"
    [ -n "$fin" ] || { echo "no hay respaldos"; exit 1; }
    echo $(( $(date +%s) - fin ))
    ;;
  *) echo "Acción desconocida: $accion" >&2; exit 2 ;;
esac
