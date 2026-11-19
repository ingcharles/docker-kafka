#!/bin/sh
# Genera pgbouncer.ini y la lista de usuarios (con las contraseñas de los 7 roles de servicio, que llegan por variables de
# entorno) en /tmp y arranca PgBouncer en primer plano.
set -eu

: "${DB_HOST:?DB_HOST es obligatorio (contenedor de Postgres)}"
: "${POSTGRES_DB:=facturacion}"

DIR=/tmp/pgbouncer
mkdir -p "$DIR"
LISTA="$DIR/userlist.txt"
: > "$LISTA"
chmod 600 "$LISTA"

# rol:variable con su contraseña
for par in \
  inventario_service:INVENTARIO_DB_PASSWORD firma_service:FIRMA_DB_PASSWORD comprobantes_service:COMPROBANTES_DB_PASSWORD \
  sri_service:SRI_DB_PASSWORD notificacion_service:NOTIFICACION_DB_PASSWORD orquestacion_service:ORQUESTACION_DB_PASSWORD \
  identidad_service:IDENTIDAD_DB_PASSWORD; do
  rol="${par%%:*}"
  variable="${par#*:}"
  eval "clave=\${$variable:?$variable es obligatorio}"
  printf '"%s" "%s"\n' "$rol" "$clave" >> "$LISTA"
done

cat > "$DIR/pgbouncer.ini" <<INI
[databases]
${POSTGRES_DB} = host=${DB_HOST} port=5432 dbname=${POSTGRES_DB}

[pgbouncer]
listen_addr = 0.0.0.0
listen_port = 6432
auth_type = scram-sha-256
auth_file = ${LISTA}
pool_mode = transaction
# Clientes: los 7 servicios con hasta 20 conexiones cada uno son 140; el margen cubre varias réplicas de cada servicio.
max_client_conn = ${PGBOUNCER_MAX_CLIENTES:-1000}
# Conexiones reales a Postgres: por cada par (base, rol) hasta default_pool_size, y en total hasta max_db_connections.
default_pool_size = ${PGBOUNCER_POOL_POR_ROL:-10}
reserve_pool_size = ${PGBOUNCER_POOL_RESERVA:-3}
reserve_pool_timeout = 3
max_db_connections = ${PGBOUNCER_MAX_CONEXIONES_BD:-100}
# Sentencias preparadas del cliente reactivo de Vert.x en modo transaction (PgBouncer 1.21 o superior).
max_prepared_statements = ${PGBOUNCER_SENTENCIAS_PREPARADAS:-200}
# Un cliente que espera conexión más de esto recibe error en vez de colgarse (mejor que el plazo de la petición).
query_wait_timeout = ${PGBOUNCER_ESPERA_MAXIMA:-30}
server_idle_timeout = 600
server_lifetime = 3600
# Los servicios mandan estos plazos al conectar; PgBouncer no los reenvía, por eso Postgres los fija por rol (post-arranque.sh).
ignore_startup_parameters = statement_timeout,lock_timeout,idle_in_transaction_session_timeout,extra_float_digits
admin_users =
log_connections = 0
log_disconnections = 0
log_pooler_errors = 1
INI

exec pgbouncer "$DIR/pgbouncer.ini"
