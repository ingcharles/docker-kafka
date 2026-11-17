# Ambientes: cómo configurar y ejecutar

Hay tres ambientes. Cada uno tiene su configuración **sin secretos** (se sube a git) y su archivo de **secretos** (git lo ignora):

| Ambiente | Configuración (versionada) | Secretos (ignorado por git) | Base de datos | Job de reenvío de correos |
|---|---|---|---|---|
| `desarrollo` | `env/desarrollo.env` | `env/desarrollo.secretos.env` | contenedor `facturacion-db` (host `localhost:5434`) | apagado |
| `pruebas` | `env/pruebas.env` | `env/pruebas.secretos.env` | contenedor `facturacion-test-db` (host `localhost:5435`) | **encendido**, cada 2 min |
| `produccion` | `env/produccion.env` | `env/produccion.secretos.env` | la tuya (`DB_HOST` por definir) | apagado |

Todo se ejecuta con `./scripts/compose.sh <ambiente> <comando de docker compose>` desde `facturacion-electronica-ec/`. En Windows, desde Git Bash
(o desde PowerShell: `bash ./scripts/compose.sh <ambiente> ...`). El script carga `env/<ambiente>.env` y `env/<ambiente>.secretos.env`; si falta el
segundo, se detiene y te dice cómo crearlo.

## Antes de empezar (una vez por máquina o servidor)

Los servicios necesitan dos stacks ya levantados, fuera de este compose:
1. **Kafka** (`../kafka`): `cd ../kafka && cp .env.example .env && docker compose up -d --build` (red `kafka_net`).
2. **Postgres** del ambiente, en la red `facturacion_net`, con un esquema y un rol por servicio. Las contraseñas de esos roles deben ser **las mismas**
   que pondrás en `*_DB_PASSWORD` del archivo de secretos. Ver `documentacion/FLUJO_FACTURA_REAL.md`, sección 0.

## Primera vez en un ambiente

Reemplaza `<amb>` por `desarrollo`, `pruebas` o `produccion`.

```bash
# 1. Crea el archivo de secretos desde la plantilla
cp env/secretos.env.example env/<amb>.secretos.env

# 2. Completa cada valor (usa valores DISTINTOS en cada ambiente)
openssl rand -base64 24      # una vez por cada *_DB_PASSWORD (siete) y POSTGRES_PASSWORD si la necesitas
openssl rand -base64 32      # FIRMA_CIFRADO_CLAVE_MAESTRA y CATALOGOS_ADMIN_TOKEN
./scripts/generar-claves-jwt.sh >> env/<amb>.secretos.env    # agrega IDENTIDAD_JWT_CLAVE_PRIVADA y _PUBLICA
# SMTP_USERNAME, SMTP_PASSWORD y SMTP_FROM: tu cuenta de correo. Con Gmail necesitas una "contraseña de aplicación"
# (verificación en dos pasos); la contraseña normal no funciona.

# 3. Revisa que todo resuelve (no imprime nada si está bien; si falta un secreto, dice cuál)
./scripts/compose.sh <amb> config -q

# 4. Construye las imágenes de a una (pesan mucho en memoria) y levanta
for s in comprobantes-service firma-electronica-service facturacion-orchestrator-service inventario-service \
         sri-conector-service notificacion-service identidad-service; do ./scripts/compose.sh <amb> build $s; done
./scripts/compose.sh <amb> up -d
./scripts/compose.sh <amb> ps          # los 7 deben quedar en "healthy" (≈1 min)
```

Las migraciones de base de datos (Flyway) corren solas cuando cada servicio arranca.

## Operación diaria

```bash
./scripts/compose.sh <amb> up -d                          # levantar (o aplicar cambios de un .env)
./scripts/compose.sh <amb> up -d facturacion-orchestrator-service   # recrear solo un servicio tras cambiar su variable
./scripts/compose.sh <amb> logs -f notificacion-service    # ver logs
./scripts/compose.sh <amb> ps                              # estado
./scripts/compose.sh <amb> down                            # apagar (no borra datos: la base está fuera de este compose)
```

Tras cambiar el **código** de un servicio: `./scripts/compose.sh <amb> build <servicio> && ./scripts/compose.sh <amb> up -d --no-deps <servicio>`.
Tras cambiar solo una **variable** de `env/*.env`: `./scripts/compose.sh <amb> up -d <servicio>` (recrea el contenedor).

## Comprobar que está arriba

```bash
for p in 8091/empresas 8092/inventario 8093/comprobantes 8094/sri 8095/notificaciones 8096/sagas 8097/identidad; do
  printf "%s " $p; curl -s -o /dev/null -w '%{http_code}\n' http://localhost:${p%%/*}/${p#*/}/api/v1/q/health/ready
done      # los siete deben responder 200
```
En producción esos puertos no se exponen a internet: ponlos detrás de un proxy inverso con TLS.

## Lo que cambia en cada ambiente

- **desarrollo**: tu máquina. CORS para `localhost:3000` y `:5173`. El reenvío automático de correos está apagado; el manual
  (`POST /sagas/reenviar-notificaciones`) funciona igual.
- **pruebas**: usa la base `facturacion-test-db`. Emite con el ambiente `PRUEBAS` del SRI: los comprobantes no tienen validez tributaria. El job de
  reenvío está encendido con tiempos cortos (cada 2 min, 20 sagas por ciclo, 5 min entre reenvíos de una misma saga, máximo 3 por saga).
- **produccion**: antes del primer arranque reemplaza en `env/produccion.env` los valores `CAMBIAR-...` (`DB_HOST` y `CORS_ORIGINS`: solo el dominio
  real del frontend, con `https`, sin `*`). Usa secretos nuevos y distintos a los de otros ambientes. Guarda `FIRMA_CIFRADO_CLAVE_MAESTRA`: sin ella
  no se pueden descifrar los certificados guardados. Haz respaldo de la base antes de actualizar, porque las migraciones corren solas. Una empresa
  con ambiente `PRODUCCION` emite documentos con validez legal. El reenvío automático de correos viene apagado: para activarlo pon
  `SAGA_REENVIO_NOTIFICACIONES_HABILITADO=true` en `env/produccion.env` y recrea el orquestador.

## Variables

| Dónde | Qué contiene |
|---|---|
| `env/<amb>.env` | `POSTGRES_DB`, `DB_HOST`, `CORS_ORIGINS`, puertos, `SMTP_HOST`/`SMTP_PORT`, `INVENTARIO_DESCUENTO_HABILITADO` y las `SAGA_REENVIO_NOTIFICACIONES_*` (ver `ARQUITECTURA.md` 4.2) |
| `env/<amb>.secretos.env` | contraseñas de Postgres y de cada rol de esquema, `FIRMA_CIFRADO_CLAVE_MAESTRA`, el par de claves JWT, `CATALOGOS_ADMIN_TOKEN` y la cuenta SMTP |

Compose solo lee `POSTGRES_DB` y `DB_HOST` de las variables de Postgres (dentro de Docker el puerto es siempre 5432); `POSTGRES_USER` y
`POSTGRES_PORT` documentan el acceso desde el host (psql, DBeaver).

## Reglas

- **Nunca** pongas una contraseña, clave o token en un archivo versionado. Los `*.secretos.env` están en `.gitignore`; verifica con
  `git check-ignore -v env/<amb>.secretos.env`.
- Si ya usas un `.env` en esta carpeta, sigue funcionando con `docker compose up` a secas; los ambientes de `env/` son la forma para manejar varios.
- Si `docker compose build` falla con `PKIX path building failed` (una red que intercepta el TLS hacia Maven Central), cambia de red o construye
  con otra conexión: el error ocurre al descargar dependencias dentro de Docker.
