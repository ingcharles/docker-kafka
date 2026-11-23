# Variables de entorno

Una variable de entorno es un valor que se le pasa a un programa desde afuera, sin escribirlo en el código. Aquí hay **dos clases**, y conviene no confundirlas:

| Clase | Quién la lee | Dónde se define | Ejemplo |
|---|---|---|---|
| **De Compose** | `docker compose`, al armar los contenedores (nombres de red, puertos, cuántos brokers, qué perfiles levantar) | `env/<ambiente>.env` | `KAFKA_NETWORK`, `FIRMA_SERVICE_PORT`, `COMPOSE_PROFILES` |
| **Del servicio** | la aplicación Quarkus dentro del contenedor, en `application.properties` como `${NOMBRE:valor-por-defecto}` | `env/<ambiente>.env` o `env/<ambiente>.secretos.env`; Compose se la pasa al contenedor | `DB_LOCK_TIMEOUT`, `SMTP_HOST`, `CORREO_TIMEOUT` |

`KAFKA_NETWORK` es de **Compose**: solo le da nombre a la red de Docker donde viven los brokers (`kafka_net` por defecto). La aplicación no la ve; para conectarse a Kafka usa `KAFKA_BOOTSTRAP_SERVERS`.

**Dónde se escribe cada cosa**

- `env/<ambiente>.env` — configuración no secreta (se versiona). Ambientes: `desarrollo`, `pruebas`, `produccion`.
- `env/<ambiente>.secretos.env` — contraseñas y claves (**no se versiona**; plantilla: `env/secretos.env.example`).
- Se levanta con `./scripts/compose.sh <ambiente> up -d`. El segundo archivo gana si una variable está en los dos.
- Una variable que no escribes usa el **valor por defecto** de esta lista. Si dice "obligatoria", sin ella el servicio (o Compose) no arranca.
- Compose solo le pasa a un contenedor las variables que aparecen en su `environment` de `docker-compose.yml`; todas las de esta lista ya están ahí.

## 1. Banderas (activar o apagar una función)

| Variable | Por defecto | Qué activa |
|---|---|---|
| `OBSERVABILIDAD_HABILITADA` | `false` (desarrollo), `true` (pruebas y producción) | Levanta Prometheus, Alertmanager, Grafana, Loki, Alloy y los exportadores de Postgres y Kafka. Apagada, esos contenedores no existen y los servicios funcionan igual. |
| `OBSERVABILIDAD_HOST_HABILITADA` | `false` (`true` en producción) | Solo servidor Linux: `node-exporter` y `cAdvisor` (CPU, memoria, disco y reloj del servidor). |
| `LOG_FORMATO_JSON` | `false` (`true` en pruebas y producción) | Los servicios escriben una línea JSON por log (para filtrar en Loki/Grafana). Apagado, texto legible. |
| `INVENTARIO_DESCUENTO_HABILITADO` | `true` | La saga reserva y descuenta stock de inventario en las facturas con ítems de inventario. |
| `SAGA_WATCHDOG_HABILITADO` | `true` | El orquestador reintenta o expira las sagas que esperan una respuesta. |
| `SAGA_REENVIO_NOTIFICACIONES_HABILITADO` | `false` | Reenvío automático de correos fallidos (envía correos solo). |
| `PGBOUNCER_HABILITADO` | `false` | PgBouncer entre los servicios y Postgres (`../postgres/compose.sh` lo levanta y `scripts/compose.sh` apunta los servicios a él). |
| `REPLICACION_HABILITADA` | `false` | Réplica de solo lectura de Postgres (necesita `REPLICACION_DB_PASSWORD`). |
| `RESPALDO_HABILITADO` | `false` | Respaldo continuo de Postgres con pgBackRest: WAL archivado, completo semanal y diferencial diario; restaura a un instante exacto. |
| `RESPALDO_ALMACEN_LOCAL_HABILITADO` | `true` (desarrollo y pruebas), `false` (producción) | Levanta un almacén S3 propio (SeaweedFS) para los respaldos; solo desarrollo y pruebas. |
| `ALERTAS_CORREO_HABILITADO` | `false` | Alertmanager envía las alertas por correo (necesita la observabilidad activa y las `ALERTAS_*` de la sección 8). |
| `COMPROBANTES_IDEMPOTENCIA_OBLIGATORIA` | `false` | Si es `true`, toda emisión debe traer la cabecera `Idempotency-Key` (sin ella, `400`). |
| `COMPOSE_PROFILES` | vacío | Perfiles de Compose a activar (p. ej. `tres-nodos` en el Kafka de producción). Con `scripts/compose.sh` no hace falta: las banderas de arriba los activan. |

## 2. Secretos (obligatorios, sin valor por defecto; van en `env/<ambiente>.secretos.env`)

| Variable | Para qué |
|---|---|
| `POSTGRES_PASSWORD` | Administrador de Postgres (stack de la base de datos). |
| `INVENTARIO_DB_PASSWORD`, `FIRMA_DB_PASSWORD`, `COMPROBANTES_DB_PASSWORD`, `SRI_DB_PASSWORD`, `NOTIFICACION_DB_PASSWORD`, `ORQUESTACION_DB_PASSWORD`, `IDENTIDAD_DB_PASSWORD` | Contraseña del rol de cada servicio en su esquema. |
| `FIRMA_CIFRADO_CLAVE_MAESTRA` | Cifra los `.p12` y sus contraseñas. **Si se pierde, los certificados guardados no se recuperan**: respáldala aparte. |
| `IDENTIDAD_JWT_CLAVE_PRIVADA` | Llave Ed25519 con la que identidad firma las api-keys (solo identidad). |
| `IDENTIDAD_JWT_CLAVE_PUBLICA` | Su par público; todos los servicios verifican las api-keys con ella. |
| `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | Cuenta de correo saliente. |
| `CATALOGOS_ADMIN_TOKEN` | Token para modificar catálogos (`PUT /catalogos/...`). Vacío = escritura deshabilitada (403). |
| `GRAFANA_ADMIN_PASSWORD` | Clave del administrador de Grafana (con la observabilidad activa). |
| `OBSERVABILIDAD_DB_PASSWORD` | Clave del rol `observabilidad` de Postgres (solo lectura; `observabilidad/crear-rol-monitoreo.sql`). |
| `REPLICACION_DB_PASSWORD` | Clave del usuario `replicador` (con `REPLICACION_HABILITADA=true`). |
| `RESPALDO_CIFRADO_CLAVE` | Clave AES-256 con la que pgBackRest cifra los respaldos. **Sin ella no se restaura**: guárdala también fuera del servidor. |
| `RESPALDO_S3_ACCESS_KEY`, `RESPALDO_S3_SECRET_KEY` | Claves de acceso al S3 de los respaldos (en desarrollo y pruebas, las del almacén propio). |
| `ALERTAS_SMTP_PASSWORD` | Clave de la cuenta SMTP de las alertas (si el servidor pide autenticación). |

## 3. Generales (Compose)

| Variable | Por defecto | Qué hace |
|---|---|---|
| `POSTGRES_DB` | `facturacion` | Nombre de la base de datos. |
| `POSTGRES_USER` / `POSTGRES_PORT` | `postgres` / `5436` (desarrollo), `5435` (pruebas) | Solo documentan el acceso desde el host (psql, DBeaver). |
| `DB_HOST` | `facturacion-db` | Nombre del servidor Postgres dentro de la red de Docker. En producción, el host real. |
| `CORS_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Orígenes del frontend permitidos (sin comodín). En producción es obligatorio un dominio real con https. |
| `FIRMA_SERVICE_PORT` … `IDENTIDAD_SERVICE_PORT` | `8091` firma, `8092` inventario, `8093` comprobantes, `8094` SRI, `8095` notificación, `8096` orquestador, `8097` identidad | Puerto del host de cada API. |
| `SMTP_HOST` / `SMTP_PORT` | (obligatoria) / `587` | Servidor de correo saliente. |

## 4. Kafka

**Del compose de facturación** (`env/<ambiente>.env`; los usan los servicios y el job que crea los tópicos):

| Variable | Desarrollo | Pruebas | Producción | Qué hace |
|---|---|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `broker:9092` | `broker-1:9092,broker-2:9092` | `broker-1:9092,broker-2:9092,broker-3:9092` | Brokers a los que se conectan los servicios. |
| `KAFKA_REPLICATION_FACTOR` | `1` | `2` | `3` | Copias de cada tópico. |
| `KAFKA_MIN_ISR` | `1` | `2` | `2` | Copias sincronizadas mínimas para aceptar un mensaje. |
| `KAFKA_DLQ_RETENCION_MS` | `2592000000` | igual | igual | Cuánto se conservan las colas de mensajes fallidos (30 días). |
| `KAFKA_EXPORTER_BROKER` | `broker:9092` | `broker-1:9092` | `broker-1:9092` | Broker al que se conecta el exportador de métricas. |

**Del compose de Kafka** (`../kafka/env/pruebas.env` y `produccion.env`; el clúster de 2 o 3 nodos):

| Variable | Pruebas | Producción | Qué hace |
|---|---|---|---|
| `KAFKA_CLUSTER_ID` | (generar una vez) | (generar una vez) | Identificador del clúster; `docker run --rm apache/kafka:3.9.0 /opt/kafka/bin/kafka-storage.sh random-uuid`. |
| `KAFKA_CLUSTER_NOMBRE` | `pruebas` | `produccion` | Nombre que muestra Kafka UI. |
| `KAFKA_QUORUM_VOTERS` | nodos 1 y 2 | nodos 1, 2 y 3 | Controladores que votan (uno por nodo). |
| `KAFKA_REPLICATION_FACTOR`, `KAFKA_MIN_ISR`, `KAFKA_BOOTSTRAP_SERVERS` | `2`, `2`, 2 brokers | `3`, `2`, 3 brokers | Igual que arriba, del lado del clúster. |
| `COMPOSE_PROFILES` | vacío | `tres-nodos` | Activa el nodo 3. |
| `KAFKA_HOST_PORT_1..3` | `9094`, `9095` | `9094`, `9095`, `9096` | Puertos del host de cada nodo. |
| `KAFKA_UI_PORT` | `8080` | `8080` | Puerto de la consola web. |
| `KAFKA_NETWORK` | `kafka_net` | `kafka_net` | Nombre de la red de Docker del clúster (cámbialo solo si levantas dos clústeres en la misma máquina). |
| `KAFKA_HEAP_OPTS` | `-Xms512m -Xmx512m` | `-Xms1g -Xmx1g` | Memoria de cada JVM de Kafka. |
| `KAFKA_TOPICS` | (lo define facturación) | igual | Solo del job de tópicos: `nombre:particiones:replicación:configs`, separados por coma. |

El Kafka de **desarrollo** (un solo nodo, `../kafka/docker-compose.yml`) usa `KAFKA_CLUSTER_ID`, `KAFKA_HOST_PORT` (`9094`), `KAFKA_UI_PORT` (`8080`) y `KAFKA_TOPICS` de `../kafka/.env`.

## 5. Plazos y ajustes de rendimiento (los de los servicios; todos tienen valor por defecto)

Se pasan a **todos** los servicios.

| Variable | Por defecto | Qué hace |
|---|---|---|
| `DB_STATEMENT_TIMEOUT` | `30s` | Una sentencia SQL que tarda más se cancela. |
| `DB_LOCK_TIMEOUT` | `10s` | Esperar un bloqueo (p. ej. el secuencial) más que esto falla con `503`. |
| `DB_IDLE_EN_TRANSACCION_TIMEOUT` | `60s` | Una transacción abierta e inactiva se cierra. |
| `SHUTDOWN_TIMEOUT` | `PT30S` | Al apagar, se esperan las peticiones en curso hasta este plazo (Compose espera 45 s: `stop_grace_period`). |
| `JVM_MAX_RAM_PORCENTAJE` | `70` | Porcentaje de la memoria del contenedor que puede usar el heap de la JVM. |
| `JVM_OPCIONES_EXTRA` | vacío | Opciones adicionales de la JVM (p. ej. `-XX:+UseZGC`). |

Productores de Kafka (todos menos identidad):

| Variable | Por defecto | Qué hace |
|---|---|---|
| `KAFKA_DELIVERY_TIMEOUT_MS` | `25000` | Plazo total para entregar un mensaje al broker. |
| `KAFKA_REQUEST_TIMEOUT_MS` | `10000` | Plazo de cada petición al broker. |
| `OUTBOX_TIMEOUT_ACK` | `PT30S` | Cuánto espera el relay del outbox la confirmación antes de soltar su transacción. Debe ser mayor que `KAFKA_DELIVERY_TIMEOUT_MS`. |
| `OUTBOX_INICIO_DIFERIDO` | `15s` | Espera inicial del relay para que Kafka esté conectado antes del primer ciclo. |

## 6. Por servicio

| Servicio | Variable | Por defecto | Qué hace |
|---|---|---|---|
| identidad | `IDENTIDAD_PASSWORD_HILOS` | `4` | Hilos de bcrypt (login y alta de usuarios). |
| identidad | `IDENTIDAD_PASSWORD_COLA` | `100` | Cola de bcrypt; llena, responde `429`. |
| identidad | `REVOCACIONES_TIMEOUT_ACK` | `PT10S` | Espera del ack de Kafka al revocar una api-key. |
| identidad | `KAFKA_REVOCACIONES_DELIVERY_TIMEOUT_MS` / `..._REQUEST_TIMEOUT_MS` | `8000` / `5000` | Plazos del productor de revocaciones. |
| comprobantes | `CONSUMIDOR_FINAL_MONTO_MAXIMO` | `50.00` | Tope (USD) de una factura a consumidor final. |
| comprobantes | `CORREO_MAXIMO_DESTINATARIOS` | `3` | Máximo de correos (separados por coma) en el campo del receptor. |
| sri-conector | `SRI_TIMEOUT_CONEXION` / `SRI_TIMEOUT_RESPUESTA` | `PT10S` / `PT30S` | Plazos de conexión y respuesta del SRI. |
| sri-conector | `SRI_URL_HOSTS_PERMITIDOS` | `*.sri.gob.ec` | Dominios a los que se puede llamar (un override por empresa fuera de la lista se ignora). |
| notificacion | `CORREO_TIMEOUT` | `PT30S` | Plazo del envío por SMTP. |
| notificacion | `PLANTILLA_CORREO_CACHE_TTL` / `PLANTILLA_CORREO_CACHE_MAXIMO` | `60S` / `2000` | Cuánto se guarda en memoria la plantilla de correo de cada empresa y cuántas se guardan. |
| orquestador | `SAGA_WATCHDOG_INTERVALO` | `30s` | Cada cuánto revisa las sagas. |
| orquestador | `SAGA_WATCHDOG_MAX_REINTENTOS` | `3` | Reintentos antes de pasar a `EXPIRADA`. |
| orquestador | `SAGA_WATCHDOG_PLAZO_FIRMA` / `SAGA_WATCHDOG_PLAZO_SRI` | `PT2M` / `PT15M` | Cuánto espera la firma y el SRI antes de reintentar. |
| orquestador | `SAGA_REENVIO_NOTIFICACIONES_INTERVALO` | `5m` | Cada cuánto corre el reenvío automático de correos. |
| orquestador | `SAGA_REENVIO_NOTIFICACIONES_LOTE` | `20` | Sagas por ciclo. |
| orquestador | `SAGA_REENVIO_NOTIFICACIONES_ESPERA_MINIMA` | `PT15M` | Espera entre dos reenvíos de la misma saga. |
| orquestador | `SAGA_REENVIO_NOTIFICACIONES_PLAZO_RESPUESTA` | `PT10M` | Cuánto esperar el resultado del envío antes de darlo por perdido. |
| orquestador | `SAGA_REENVIO_NOTIFICACIONES_MAX_INTENTOS` | `3` | Reenvíos automáticos por saga. |

Los plazos usan formato ISO-8601 (`PT30S` = 30 segundos, `PT2M` = 2 minutos) o `30s`/`5m`, según la propiedad; el valor por defecto de la tabla muestra cuál.

## 7. Conexión a la base de datos (por servicio)

Compose arma la URL a partir de `DB_HOST` y `POSTGRES_DB` (la reactiva usa `DB_POOL_HOST`/`DB_POOL_PORT` cuando `PGBOUNCER_HABILITADO=true`; la JDBC de Flyway siempre va directa); el usuario sale del servicio y no se pasa desde Compose (se llama como su rol).

| Servicio | Usuario (por defecto) | URLs |
|---|---|---|
| identidad | `identidad_service` | `IDENTIDAD_DB_REACTIVE_URL`, `IDENTIDAD_DB_JDBC_URL` |
| firma-electronica | `firma_service` | `FIRMA_DB_REACTIVE_URL`, `FIRMA_DB_JDBC_URL` |
| inventario | `inventario_service` | `INVENTARIO_DB_REACTIVE_URL`, `INVENTARIO_DB_JDBC_URL` |
| comprobantes | `comprobantes_service` | `COMPROBANTES_DB_REACTIVE_URL`, `COMPROBANTES_DB_JDBC_URL` |
| sri-conector | `sri_service` | `SRI_DB_REACTIVE_URL`, `SRI_DB_JDBC_URL` |
| notificacion | `notificacion_service` | `NOTIFICACION_DB_REACTIVE_URL`, `NOTIFICACION_DB_JDBC_URL` |
| orquestador | `orquestacion_service` | `ORQUESTACION_DB_REACTIVE_URL`, `ORQUESTACION_DB_JDBC_URL` |

Con `mvn quarkus:dev` (sin Docker) las URLs por defecto apuntan a `localhost:5434` y Kafka a `localhost:9094`.

## 8. Observabilidad (con `OBSERVABILIDAD_HABILITADA=true`)

| Variable | Por defecto | Qué hace |
|---|---|---|
| `OBSERVABILIDAD_BIND` | `127.0.0.1` | Dirección donde se publican Prometheus, Alertmanager y Grafana. Solo la máquina local; en producción se sirven por el proxy con TLS. |
| `PROMETHEUS_PORT` / `ALERTMANAGER_PORT` / `GRAFANA_PORT` | `9090` / `9093` / `3001` | Puertos del host. |
| `PROMETHEUS_RETENCION` | `15d` | Cuánto conserva Prometheus las métricas. |
| `GRAFANA_ADMIN_USER` | `admin` | Usuario administrador de Grafana. |
| `ALERTAS_CORREO_DESTINATARIOS` | vacío | Correos que reciben las alertas, separados por coma (obligatorio con `ALERTAS_CORREO_HABILITADO=true`). |
| `ALERTAS_CORREO_REMITENTE` | vacío | Remitente de las alertas (obligatorio con la bandera). |
| `ALERTAS_SMTP_HOST` | `smtp.gmail.com:587` | Servidor SMTP `host:puerto`. |
| `ALERTAS_SMTP_USUARIO` | vacío | Usuario SMTP (vacío si el servidor no pide autenticación). |
| `ALERTAS_SMTP_REQUIERE_TLS` | `true` | `false` solo para un servidor de pruebas sin TLS. |

## 9. Contenedores de los servicios (límites y endurecimiento)

Se aplican a los 7 microservicios (`x-contenedor` y `x-jvm` en `docker-compose.yml`). Además cada contenedor corre con sistema de archivos de solo lectura (`/tmp` aparte), sin capacidades de Linux (`cap_drop: ALL`), sin poder escalar privilegios (`no-new-privileges`), con `init` (recoge procesos zombi) y rotación de logs.

| Variable | Por defecto | Qué hace |
|---|---|---|
| `CONTENEDOR_MEMORIA` | `1g` | Memoria máxima de cada servicio; al pasarla Docker lo reinicia en vez de dejar que se coma al servidor. |
| `CONTENEDOR_CPUS` | `2.0` | CPU máxima de cada servicio. |
| `CONTENEDOR_PIDS` | `512` | Procesos/hilos máximos (corta una fuga de hilos). |
| `LOG_ROTACION_TAMANO` / `LOG_ROTACION_ARCHIVOS` | `10m` / `5` | Rotación del log de Docker de cada contenedor (50 MB máximo por servicio). |
| `JVM_MAX_RAM_PORCENTAJE` | `70` | Ver sección 5. |

Dimensionar: con `CONTENEDOR_MEMORIA=1g` y `JVM_MAX_RAM_PORCENTAJE=70` el heap llega a ~700 MB; el resto es memoria fuera del heap (hilos, buffers, metaspace). En producción mide con Grafana (tablero Servicios, memoria) y ajusta por servicio con `docker-compose.override.yml`.

## 10. Postgres: pooler, réplica y respaldo (stack `../postgres`)

Todas viven en `env/<ambiente>.env` (o en el archivo de secretos las claves) y las lee `../postgres/compose.sh`. Guía completa: `../postgres/README.md`.

| Variable | Por defecto | Qué hace |
|---|---|---|
| `DB_MAX_CONEXIONES` | `200` | `max_connections` de Postgres (la réplica debe tener al menos el mismo). |
| `PGBOUNCER_POOL_POR_ROL` | `10` | Conexiones reales a Postgres por servicio. |
| `PGBOUNCER_MAX_CONEXIONES_BD` | `100` | Conexiones reales totales de PgBouncer a Postgres. |
| `PGBOUNCER_POOL_RESERVA` | `3` | Conexiones extra por servicio si hay espera. |
| `PGBOUNCER_MAX_CLIENTES` | `1000` | Conexiones de los servicios que PgBouncer acepta. |
| `PGBOUNCER_ESPERA_MAXIMA` | `30` | Segundos que espera un cliente una conexión antes de fallar. |
| `PGBOUNCER_SENTENCIAS_PREPARADAS` | `200` | Sentencias preparadas por conexión que PgBouncer rastrea. |
| `PGBOUNCER_MEMORIA` | `128m` | Límite de memoria del contenedor. |
| `REPLICA_PORT` | `5437` | Puerto del host de la réplica. |
| `REPLICACION_MAX_WAL_RETENIDO` | `10GB` | WAL máximo que el primario guarda para una réplica caída. |
| `RESPALDO_REPO` | `posix` (en los env de ambiente: `s3`) | Dónde guarda pgBackRest: `posix` (volumen) o `s3`. |
| `RESPALDO_S3_ENDPOINT` | `<DB_HOST>-almacen:9000` | Servidor S3 `host:puerto` (en producción, el de tu proveedor). |
| `RESPALDO_S3_BUCKET` / `RESPALDO_S3_REGION` / `RESPALDO_S3_RUTA` | `respaldos-facturacion` / `us-east-1` / `/pgbackrest` | Bucket, región y carpeta dentro del bucket. |
| `RESPALDO_S3_ESTILO_URI` | `path` | `path` (almacén propio, MinIO, Ceph) o `host` (AWS S3). |
| `RESPALDO_S3_VERIFICAR_TLS` | `y` (`n` en desarrollo) | Verifica el certificado del S3; `n` solo con el almacén propio autofirmado. |
| `RESPALDO_RETENCION_COMPLETOS` / `RESPALDO_RETENCION_DIFERENCIALES` | `4` / `7` | Respaldos completos y diferenciales que se conservan. |
| `RESPALDO_CRON_COMPLETO` / `RESPALDO_CRON_DIFERENCIAL` | `0 2 * * 0` / `0 2 * * 1-6` | Cuándo corren (formato cron, en `RESPALDO_ZONA_HORARIA`). |
| `RESPALDO_ZONA_HORARIA` | `America/Guayaquil` | Zona horaria de esas horas. |
| `RESPALDO_ARCHIVE_TIMEOUT` | `60` | Segundos máximos entre dos archivados de WAL (pérdida máxima de datos si el disco muere). |
| `RESPALDO_PROCESOS` | `2` | Procesos paralelos de pgBackRest. |

## 11. Idempotencia de la emisión (comprobantes)

| Variable | Por defecto | Qué hace |
|---|---|---|
| `COMPROBANTES_IDEMPOTENCIA_OBLIGATORIA` | `false` | Ver sección 1. |
| `COMPROBANTES_IDEMPOTENCIA_RETENCION` | `P7D` | Cuánto tiempo una clave `Idempotency-Key` devuelve el mismo comprobante; pasado ese plazo se purga y la misma clave emitiría uno nuevo. |
| `COMPROBANTES_IDEMPOTENCIA_PURGA_CADA` | `1h` | Cada cuánto se borran las claves vencidas. |
