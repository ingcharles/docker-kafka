#!/bin/sh
# Envoltorio del entrypoint oficial de Postgres. Según las banderas del ambiente:
#   RESPALDO_HABILITADO=true     archiva el WAL y hace respaldos programados con pgBackRest (repositorio posix o S3).
#   REPLICACION_HABILITADA=true  el primario acepta una réplica (usuario y slot de replicación).
#   REPLICA_DE=<host>            este contenedor es una réplica de solo lectura de <host> (se clona con pg_basebackup).
# Sin banderas se comporta igual que la imagen oficial.
set -eu

PGDATA="${PGDATA:-/var/lib/postgresql/data}"
STANZA="${RESPALDO_STANZA:-facturacion}"
CONF_PGBACKREST=/etc/pgbackrest/pgbackrest.conf

es_verdadero() { [ "${1:-false}" = "true" ]; }

# Escribe la configuración de pgBackRest (con las claves de S3 y de cifrado, por eso no sale de la imagen).
generar_configuracion_pgbackrest() {
  mkdir -p /etc/pgbackrest /var/log/pgbackrest /var/lib/pgbackrest
  chown postgres:postgres /var/log/pgbackrest /var/lib/pgbackrest
  {
    echo "[global]"
    echo "log-level-console=${RESPALDO_LOG_NIVEL:-info}"
    echo "log-path=/var/log/pgbackrest"
    echo "compress-type=zst"
    echo "compress-level=${RESPALDO_COMPRESION_NIVEL:-3}"
    echo "process-max=${RESPALDO_PROCESOS:-2}"
    echo "start-fast=y"
    # Agrupa los archivos pequeños y copia solo los bloques que cambiaron: menos objetos y menos bytes por respaldo.
    echo "repo1-bundle=y"
    echo "repo1-block=y"
    echo "repo1-retention-full=${RESPALDO_RETENCION_COMPLETOS:-4}"
    echo "repo1-retention-diff=${RESPALDO_RETENCION_DIFERENCIALES:-7}"
    if [ "${RESPALDO_REPO:-posix}" = "s3" ]; then
      echo "repo1-type=s3"
      echo "repo1-path=${RESPALDO_S3_RUTA:-/pgbackrest}"
      echo "repo1-s3-endpoint=${RESPALDO_S3_ENDPOINT:?RESPALDO_S3_ENDPOINT es obligatorio con RESPALDO_REPO=s3}"
      echo "repo1-s3-bucket=${RESPALDO_S3_BUCKET:?RESPALDO_S3_BUCKET es obligatorio con RESPALDO_REPO=s3}"
      echo "repo1-s3-region=${RESPALDO_S3_REGION:-us-east-1}"
      echo "repo1-s3-key=${RESPALDO_S3_ACCESS_KEY:?RESPALDO_S3_ACCESS_KEY es obligatorio con RESPALDO_REPO=s3}"
      echo "repo1-s3-key-secret=${RESPALDO_S3_SECRET_KEY:?RESPALDO_S3_SECRET_KEY es obligatorio con RESPALDO_REPO=s3}"
      echo "repo1-s3-uri-style=${RESPALDO_S3_ESTILO_URI:-path}"
      echo "repo1-storage-verify-tls=${RESPALDO_S3_VERIFICAR_TLS:-y}"
    else
      echo "repo1-type=posix"
      echo "repo1-path=/var/lib/pgbackrest"
    fi
    if [ -n "${RESPALDO_CIFRADO_CLAVE:-}" ]; then
      echo "repo1-cipher-type=aes-256-cbc"
      echo "repo1-cipher-pass=${RESPALDO_CIFRADO_CLAVE}"
    fi
    echo ""
    echo "[${STANZA}]"
    echo "pg1-path=${PGDATA}"
    echo "pg1-socket-path=/var/run/postgresql"
  } > "$CONF_PGBACKREST"
  chown root:postgres "$CONF_PGBACKREST"
  chmod 640 "$CONF_PGBACKREST"
}

# Programa los respaldos: completo semanal y diferencial diario (hora de Ecuador por defecto).
iniciar_programador_de_respaldos() {
  export TZ="${RESPALDO_ZONA_HORARIA:-America/Guayaquil}"
  {
    echo "${RESPALDO_CRON_COMPLETO:-0 2 * * 0} su-exec postgres pgbackrest --stanza=${STANZA} --type=full backup > /proc/1/fd/1 2>&1"
    echo "${RESPALDO_CRON_DIFERENCIAL:-0 2 * * 1-6} su-exec postgres pgbackrest --stanza=${STANZA} --type=diff backup > /proc/1/fd/1 2>&1"
  } > /etc/crontabs/root
  crond -b -L /proc/1/fd/1
}

if [ "${1:-}" = "postgres" ]; then
  if [ -n "${REPLICA_DE:-}" ]; then
    # Réplica: se clona del primario la primera vez y arranca en modo de solo lectura.
    if [ ! -s "$PGDATA/PG_VERSION" ]; then
      mkdir -p "$PGDATA"
      chown postgres:postgres "$PGDATA"
      chmod 700 "$PGDATA"
      until su-exec postgres env PGPASSWORD="${REPLICACION_DB_PASSWORD:?REPLICACION_DB_PASSWORD es obligatorio en la réplica}" \
          pg_basebackup -h "$REPLICA_DE" -p 5432 -U "${REPLICACION_DB_USUARIO:-replicador}" -D "$PGDATA" \
          -R -X stream -S "${REPLICACION_SLOT:-replica_1}" -c fast; do
        echo "No se pudo clonar ${REPLICA_DE} (¿primario sin REPLICACION_HABILITADA=true?); reintento en 5 s"
        find "$PGDATA" -mindepth 1 -delete
        sleep 5
      done
    fi
    set -- "$@" -c hot_standby=on
  else
    if es_verdadero "${REPLICACION_HABILITADA:-false}"; then
      # Un slot retiene WAL mientras la réplica esté caída: se limita para que no llene el disco.
      set -- "$@" -c "max_slot_wal_keep_size=${REPLICACION_MAX_WAL_RETENIDO:-10GB}"
    fi
    if es_verdadero "${RESPALDO_HABILITADO:-false}"; then
      generar_configuracion_pgbackrest
      set -- "$@" -c archive_mode=on -c "archive_command=pgbackrest --stanza=${STANZA} archive-push %p" \
        -c "archive_timeout=${RESPALDO_ARCHIVE_TIMEOUT:-60}"
      iniciar_programador_de_respaldos
    fi
    # Tareas que necesitan el servidor arriba (roles, slot, stanza y primer respaldo).
    /usr/local/share/facturacion-postgres/post-arranque.sh &
  fi
fi

exec docker-entrypoint.sh "$@"
