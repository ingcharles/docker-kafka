# Stack de Postgres (facturación electrónica)

Postgres 16 compartido por los microservicios de `../facturacion-electronica-ec`: una base (`facturacion`), un esquema y un rol de mínimo privilegio por servicio. Vive en el repositorio para que el proyecto se pueda levantar completo en otra máquina.

## Archivos

- `Dockerfile` — `postgres:16-alpine` con `init/` precargado y healthcheck.
- `docker-compose.yml` — servicio `facturacion-db`, volumen `facturacion_pg_data`, red `facturacion_net` (nombre fijo).
- `init/00-schemas.sh` — crea esquemas y roles (solo en el primer arranque con volumen vacío).
- `.env.example` — plantilla; copiar a `.env`.

## Correrlo

Con los mismos archivos de ambiente que los servicios (las contraseñas no se duplican):

```sh
# desarrollo (container `facturacion-db`, puerto 5434)
docker compose --env-file ../facturacion-electronica-ec/env/desarrollo.env \
               --env-file ../facturacion-electronica-ec/env/desarrollo.secretos.env up -d --build
# pruebas (container `facturacion-test-db`, puerto 5435, volumen propio)
docker compose -p facturacion-pruebas --env-file ../facturacion-electronica-ec/env/pruebas.env \
               --env-file ../facturacion-electronica-ec/env/pruebas.secretos.env up -d --build
```

El nombre del contenedor es `DB_HOST` (el mismo que usan los servicios). Alternativa sin los archivos de ambiente: `cp .env.example .env`, cambia las contraseñas y `docker compose up -d --build`. En producción se usa tu propio Postgres (`DB_HOST` en `env/produccion.env`), no este stack.

Desde la máquina: `localhost:5434` (`POSTGRES_PORT`). Desde otro contenedor en `facturacion_net`: `facturacion-db:5432`.

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

## Uso con los microservicios

Levanta este stack **antes** que `facturacion-electronica-ec/docker-compose.yml`, que se une a `facturacion_net` como red externa. Las contraseñas `*_DB_PASSWORD` deben ser las mismas que usan los servicios (con los archivos de ambiente de arriba lo son por construcción).

Los scripts de `init/` corren una sola vez; para re-ejecutarlos: `docker compose down -v` (borra los datos).

## Crear las tablas

Este contenedor solo crea esquemas y roles. Las tablas están definidas en las migraciones Flyway de cada servicio (`<servicio>/src/main/resources/db/migration/V*.sql`). Para crearlas sin levantar los microservicios:

```sh
docker compose --env-file ../facturacion-electronica-ec/env/<ambiente>.env --env-file ../facturacion-electronica-ec/env/<ambiente>.secretos.env --profile migrar run --rm migraciones
```

Aplica las migraciones de los 7 servicios con el rol de cada uno (`scripts/migrar.sh`) y deja el mismo `flyway_schema_history` que dejaría el servicio, así que después los servicios arrancan sin conflicto. Es idempotente: solo aplica lo pendiente, por lo que sirve también cuando se agregue una migración nueva.

Si el proyecto no está en la ruta por defecto (`../facturacion-electronica-ec`), define `FACTURACION_PROYECTO_DIR`.
