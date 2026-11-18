# Observabilidad

Métricas, alertas y logs de los 7 servicios, Postgres y Kafka. Es **opcional**: con la bandera apagada estos contenedores no existen y los servicios funcionan igual.

## Cómo se activa

En `env/<ambiente>.env`:

```
OBSERVABILIDAD_HABILITADA=true     # Prometheus, Alertmanager, Grafana, Loki, Alloy, exportadores de Postgres y Kafka
OBSERVABILIDAD_HOST_HABILITADA=true # solo servidor Linux: node-exporter y cAdvisor (CPU, memoria, disco, reloj)
LOG_FORMATO_JSON=true              # los servicios escriben JSON (recomendado con Loki)
```

y `./scripts/compose.sh <ambiente> up -d` (el script traduce la bandera al perfil de Compose). Con `docker compose` directo: `COMPOSE_PROFILES=observabilidad`. Apagar: pon la bandera en `false` y `./scripts/compose.sh <ambiente> up -d --remove-orphans`.

## Primera vez

1. En `env/<ambiente>.secretos.env`: `GRAFANA_ADMIN_PASSWORD` y `OBSERVABILIDAD_DB_PASSWORD`.
2. Crea el rol de solo lectura de Postgres (una vez, como administrador): `psql -U postgres -d facturacion -v clave="'LA-CLAVE'" -f observabilidad/crear-rol-monitoreo.sql`. Solo puede leer las estadísticas de Postgres y contar filas de unas pocas columnas; no ve datos personales.
3. Levanta con la bandera.

## Dónde se mira

| Qué | Dónde (en la máquina local) |
|---|---|
| Tableros y logs | Grafana, `http://localhost:3001` — carpeta "Facturación electrónica": **Servicios** (peticiones, errores 5xx, latencia, memoria, CPU) y **Negocio** (sagas por estado, outbox, DLQ, retraso de consumidores, certificados por vencer, conexiones) |
| Alertas | Alertmanager, `http://localhost:9093`, y Prometheus → Alerts, `http://localhost:9090/alerts` |
| Logs | Grafana → Explore → Loki: `{servicio="comprobantes-service"} \|= "[COMP-"` |

Los puertos solo se publican en `127.0.0.1` (`OBSERVABILIDAD_BIND`). En producción se sirven por el proxy con TLS y autenticación; no los abras a internet.

## Qué hay

| Pieza | Para qué |
|---|---|
| Prometheus | Lee las métricas de los servicios (`<root-path>/q/metrics`) y exportadores cada 15 s; evalúa `prometheus/reglas/alertas.yml` |
| Alertmanager | Agrupa y envía las alertas. Por defecto solo las muestra; para correo o Teams descomenta un receptor en `alertmanager/alertmanager.yml` (los secretos van en archivos, no en el YAML) |
| Grafana | Tableros provisionados (`grafana/dashboards/*.json`), de solo lectura; para cambiarlos edita el JSON |
| Loki + Alloy | Alloy lee los logs de los contenedores por el socket de Docker y los manda a Loki (14 días). Con `LOG_FORMATO_JSON=true` el nivel queda como etiqueta |
| postgres-exporter | Estadísticas de Postgres y las métricas de negocio de `postgres-exporter/queries.yaml` (sagas por estado, outbox pendiente, certificados por vencer) |
| kafka-exporter | Retraso de consumidores y mensajes por tópico (incluidas las DLQ) |
| node-exporter, cAdvisor | Solo perfil `host`, en Linux. Sin ese perfil, sus dos objetivos aparecen DOWN en Prometheus: es normal |

## Alertas

`critica` = actuar ya; `aviso` = revisar en horario.

| Alerta | Severidad | Cuándo |
|---|---|---|
| ServicioCaido | crítica | un servicio no responde 2 min |
| ErroresHttp5xx | crítica | más de 3 errores 5xx por minuto durante 5 min |
| LatenciaAlta | aviso | latencia media > 2 s durante 10 min |
| MemoriaJvmAlta | aviso | heap > 90 % durante 10 min |
| MensajesEnDlq | crítica | llegó un mensaje a una `.dlq` |
| RetrasoDeConsumidor | aviso | más de 100 mensajes de retraso durante 10 min |
| KafkaCaido / PostgresCaido | crítica | no responden |
| ConexionesPostgresAltas | aviso | más del 80 % de `max_connections` |
| InterbloqueosPostgres | aviso | hubo deadlocks en 10 min |
| SagasExpiradas | crítica | hay sagas `EXPIRADA` más de 5 min |
| NotificacionesFallidasSostenidas | aviso | más de 10 sagas con correo fallido durante 30 min |
| OutboxAtascado | crítica | más de 100 eventos sin publicar durante 5 min |
| CertificadoPorVencer | aviso | un certificado activo vence en menos de 30 días |
| RelojDesfasado / DiscoLleno | crítica | reloj del servidor desfasado > 1 s / menos del 10 % de disco (perfil `host`) |

## Seguridad

- Los endpoints `/q/metrics` de los servicios no piden api-key: solo se alcanzan dentro de la red de Docker y no se publican fuera. No los expongas por el gateway.
- Los logs llevan códigos e ids, nunca cédulas, correos ni claves (regla de `CLAUDE.md`), por eso se pueden guardar en Loki.
- Alloy monta el socket de Docker en solo lectura; solo lee los logs de los contenedores de este proyecto.
