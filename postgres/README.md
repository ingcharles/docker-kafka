# Stack de Postgres (facturación electrónica)

Postgres 16 compartido por los microservicios de `../facturacion-electronica-ec`: una base (`facturacion`), un esquema y un rol de mínimo privilegio por servicio. Vive en el repositorio para que el proyecto se pueda levantar completo en otra máquina. Trae, **opcionales y con bandera**, un pooler de conexiones (PgBouncer), una réplica de solo lectura y el respaldo continuo con restauración a un instante exacto (pgBackRest).

## Archivos

- `Dockerfile` — `postgres:16-alpine` con `init/` precargado, healthcheck, pgBackRest y un envoltorio del entrypoint (`contenedor/`).
- `docker-compose.yml` — `facturacion-db`, volúmenes, red `facturacion_net` (nombre fijo) y los perfiles opcionales.
- `compose.sh` — corre Compose con los archivos de ambiente de los servicios y traduce las banderas a perfiles.
- `init/00-schemas.sh` — crea esquemas y roles (solo en el primer arranque con volumen vacío).
- `contenedor/` — `entrypoint.sh` (respaldo, réplica) y `post-arranque.sh` (plazos por rol, usuario y slot de replicación, primer respaldo).
- `pgbouncer/` — imagen de PgBouncer.
- `scripts/` — `migrar.sh` (Flyway), `respaldo.sh` (operar el respaldo), `probar-restauracion.sh` (ensayo de restauración).

## Correrlo

```sh
./compose.sh desarrollo up -d --build      # container `facturacion-db`, puerto 5436 (POSTGRES_PORT)
./compose.sh pruebas up -d --build         # container `facturacion-test-db`, puerto 5435, volumen propio
./compose.sh produccion up -d --build      # solo si usas este stack en producción
```

Usa los mismos archivos de ambiente que los servicios (`../facturacion-electronica-ec/env/<ambiente>.env` y `.secretos.env`), así las contraseñas no se duplican. El nombre del contenedor es `DB_HOST` (el mismo que usan los servicios). Desde otro contenedor en `facturacion_net`: `<DB_HOST>:5432`. En producción se suele usar tu propio Postgres (`DB_HOST` en `env/produccion.env`); las secciones siguientes dicen qué instalar allí.

## Esquema → rol

| Esquema | Rol | Variable |
|---|---|---|
| `inventario` | `inventario_service` | `INVENTARIO_DB_PASSWORD` |
| `firma_electronica` | `firma_service` | `FIRMA_DB_PASSWORD` |
| `comprobantes` | `comprobantes_service` | `COMPROBANTES_DB_PASSWORD` |
| `sri` | `sri_service` | `SRI_DB_PASSWORD` |
| `notificacion` | `notificacion_service` | `NOTIFICACION_DB_PASSWORD` |
| `orquestacion` | `orquestacion_service` | `ORQUESTACION_DB_PASSWORD` |
| `identidad` | `identidad_service` | `IDENTIDAD_DB_PASSWORD` |

Cada arranque, `post-arranque.sh` fija en cada rol `statement_timeout`, `lock_timeout` e `idle_in_transaction_session_timeout` (`DB_*_TIMEOUT`): valen también cuando hay un pooler en medio, que descarta los parámetros de arranque de la conexión.

## Crear las tablas

Este contenedor solo crea esquemas y roles. Las tablas están en las migraciones Flyway de cada servicio (`<servicio>/src/main/resources/db/migration/V*.sql`). Para crearlas sin levantar los microservicios:

```sh
./compose.sh <ambiente> --profile migrar run --rm migraciones
```

Aplica las migraciones de los 7 servicios con el rol de cada uno (`scripts/migrar.sh`) y deja el mismo `flyway_schema_history` que dejaría el servicio. Es idempotente. Si el proyecto no está en la ruta por defecto, define `FACTURACION_PROYECTO_DIR`.

## PgBouncer (pooler de conexiones)

`PGBOUNCER_HABILITADO=true` en `env/<ambiente>.env`. Los 7 servicios abren hasta 20 conexiones cada uno (140) y Postgres tiene `max_connections=200`; con varias réplicas de cada servicio eso se agota. PgBouncer en modo `transaction` multiplexa: Postgres solo mantiene `PGBOUNCER_MAX_CONEXIONES_BD` (100) conexiones, repartidas en un pool de `PGBOUNCER_POOL_POR_ROL` (10) por servicio, y quedan libres las demás para Flyway, administración, el exportador y la replicación.

- `scripts/compose.sh` de los servicios apunta las URLs reactivas al pooler (`DB_POOL_HOST`/`DB_POOL_PORT`); las URLs JDBC de Flyway siguen directas a Postgres.
- Sentencias preparadas: PgBouncer ≥ 1.21 las soporta en modo `transaction` (`max_prepared_statements`).
- Un cliente que espera una conexión más de `PGBOUNCER_ESPERA_MAXIMA` (30 s) recibe error.
- Con tu propio Postgres: instala PgBouncer allí con `pgbouncer/entrypoint.sh` como guía (`pool_mode=transaction`, `auth_type=scram-sha-256`, `ignore_startup_parameters=statement_timeout,lock_timeout,idle_in_transaction_session_timeout,extra_float_digits`) y fija los plazos por rol como arriba.

## Réplica de solo lectura

`REPLICACION_HABILITADA=true` y `REPLICACION_DB_PASSWORD` en secretos. Levanta `<DB_HOST>-replica` (puerto `REPLICA_PORT`, 5437), un Postgres en modo *hot standby* que se clona solo del primario con `pg_basebackup` y recibe su WAL por streaming con un slot (`replica_1`). El primario limita el WAL que retiene para la réplica (`REPLICACION_MAX_WAL_RETENIDO`, 10 GB) para que una réplica caída no le llene el disco.

Los servicios no leen de la réplica. Sirve de copia caliente (por ejemplo, para consultas pesadas o reportes) y de punto de partida si el primario se pierde. **No hay conmutación automática**: para eso hace falta Patroni o un Postgres administrado (RDS, Cloud SQL...). Promoverla a mano:

```sh
docker exec -u postgres <DB_HOST>-replica pg_ctl promote -D /var/lib/postgresql/data
# luego apunta DB_HOST (o el DNS) a la réplica promovida y recrea los servicios
```

Ver el retraso: `select client_addr, state, pg_wal_lsn_diff(sent_lsn, replay_lsn) as retraso_bytes from pg_stat_replication;` (en el primario).

## Respaldos

`RESPALDO_HABILITADO=true`. Un respaldo de Postgres es **independiente de lo que hace la aplicación**: no cambia el código ni el comportamiento de los servicios, solo necesita archivar el WAL (`archive_mode=on`, la bandera lo activa y reinicia Postgres al recrear el contenedor).

**Cómo respalda** (pgBackRest, el estándar para Postgres):

- Archiva cada segmento de WAL a medida que se cierra (`archive_timeout` 60 s) → se puede restaurar **a cualquier instante**, no solo al del último respaldo.
- Respaldo **completo semanal** (domingo 02:00, hora de Ecuador) y **diferencial diario** (lunes a sábado 02:00); `RESPALDO_CRON_COMPLETO` / `RESPALDO_CRON_DIFERENCIAL`.
- Eficiente: copia solo los bloques que cambiaron (`repo1-block`), agrupa archivos pequeños (`repo1-bundle`), comprime con zstd y paraleliza (`RESPALDO_PROCESOS`). Un diferencial de una base casi sin cambios pesa ~2 MB.
- Cifrado AES-256 (`RESPALDO_CIFRADO_CLAVE`) y verificación de checksums. Los respaldos contienen datos personales (XML con identificación del comprador): **guarda la clave de cifrado también fuera del servidor**; sin ella no se restaura nada.
- Retención: `RESPALDO_RETENCION_COMPLETOS` (4 semanas) y `RESPALDO_RETENCION_DIFERENCIALES` (7); el WAL se conserva mientras algún respaldo lo necesite.
- Al arrancar crea el repositorio (`stanza`) y, si no hay ningún respaldo, hace uno completo de inmediato.

**Dónde guarda** (`RESPALDO_REPO`):

| Valor | Uso |
|---|---|
| `s3` | Recomendado. Bucket S3 de tu proveedor (AWS S3, Backblaze B2, Wasabi...) con `RESPALDO_S3_ENDPOINT`, `RESPALDO_S3_BUCKET`, `RESPALDO_S3_ACCESS_KEY`/`RESPALDO_S3_SECRET_KEY` (un usuario con permiso solo sobre ese bucket). El respaldo debe vivir **fuera** del servidor y del disco de Postgres. |
| `posix` | Un volumen aparte (`facturacion_pg_respaldo`). Sirve para probar; en producción móntalo en otro disco o servidor. |

**Almacén S3 propio, solo para desarrollo y pruebas**: `RESPALDO_ALMACEN_LOCAL_HABILITADO=true` levanta SeaweedFS con un bucket y certificado autofirmado (pgBackRest solo habla S3 por HTTPS; por eso `RESPALDO_S3_VERIFICAR_TLS=n`). No es un respaldo real: está en la misma máquina. MinIO ya no publica imágenes de contenedor.

**Operar** (`./scripts/respaldo.sh <ambiente> <acción>`): `estado`, `completo`, `diferencial`, `incremental`, `verificar` (¿responde el archivo de WAL?), `comprobar` (integridad de todo el repositorio), `expirar`, `edad` (segundos desde el último respaldo).

**Ensayar la restauración**: `./scripts/probar-restauracion.sh <ambiente>` escribe una fila, toma un diferencial, anota la hora, escribe otra fila, restaura en un contenedor desechable hasta esa hora y comprueba que solo está la primera y que las tablas de los servicios tienen las mismas filas que el original. Hazlo después de configurar y cada trimestre: un respaldo que nunca se restauró no es un respaldo.

**Restaurar de verdad** (el primario perdido o dañado): detén los servicios y Postgres, vacía el volumen de datos y, con el contenedor de Postgres detenido, restaura sobre el volumen vacío con una imagen igual a la del stack:

```sh
# último estado disponible:
pgbackrest --stanza=facturacion --delta restore
# a un instante (hora UTC):
pgbackrest --stanza=facturacion --type=time --target="2026-11-19 13:51:04+00" --target-action=promote --delta restore
```

`probar-restauracion.sh` es la receta ejecutable. Con tu propio Postgres en producción: instala `pgbackrest` allí, copia el bloque `[global]` y `[facturacion]` que genera `contenedor/entrypoint.sh` a `/etc/pgbackrest/pgbackrest.conf`, y fija `archive_mode=on` y `archive_command='pgbackrest --stanza=facturacion archive-push %p'`.

**Alertas**: la métrica `facturacion_archivado_fallos` (observabilidad) dispara `ArchivadoWalFallando` si Postgres no logra archivar el WAL. La edad del último respaldo (`respaldo.sh <ambiente> edad`) conviene vigilarla desde tu monitoreo externo (debe ser menor a ~26 h).

**No cubre**: los archivos XML/RIDE ya viven en la base y por tanto en estos respaldos; un archivo de 7 años con bloqueo contra borrado (S3 Object Lock) es otra pieza, pendiente.

## Uso con los microservicios

Levanta este stack **antes** que `facturacion-electronica-ec/docker-compose.yml`, que se une a `facturacion_net` como red externa. Los scripts de `init/` corren una sola vez; para re-ejecutarlos: `./compose.sh <ambiente> down -v` (borra los datos).
