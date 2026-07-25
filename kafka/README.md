# Stack de Kafka

Apache Kafka de un solo nodo en modo KRaft (sin Zookeeper), más un job que crea los tópicos automáticamente y una interfaz web. Tres servicios:

- **broker** — `apache/kafka`, nodo combinado de broker+controller.
- **topic-init** — contenedor de un solo uso que espera a que el broker esté listo y crea los tópicos listados en `KAFKA_TOPICS`, y luego termina.
- **kafka-ui** — `provectuslabs/kafka-ui`, consola web para ver tópicos, mensajes y grupos de consumidores en `:8080`.

## Notas de diseño

- KRaft, no Zookeeper: menos piezas móviles, y además Zookeeper está en proceso de quedar obsoleto en el propio Kafka.
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` — los tópicos se crean explícitamente vía `topic-init`, no de forma implícita al primer intento de producir un mensaje. Así, un error de tipeo en el nombre de un tópico falla de forma ruidosa, en vez de crear silenciosamente un tópico de una sola partición del que te vas a arrepentir después.
- Dos listeners: `PLAINTEXT` (`broker:9092`) para otros contenedores dentro de la red `kafka_net`, y `PLAINTEXT_HOST` (`localhost:9094` por defecto) para cualquier cosa que se conecte desde la máquina anfitriona (scripts, plugins del IDE, etc.).
- `kafka_net` tiene un `name:` fijo (sin el prefijo del proyecto de Compose) específicamente para que otros proyectos de Compose puedan conectarse a ella como red externa — [`ecommerce/`](../ecommerce/README.md) reutiliza exactamente este mismo broker en vez de levantar uno segundo.
- Solo PLAINTEXT — aquí no hay TLS ni SASL. Está bien para desarrollo local; hay que agregar configuración de `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP`/SASL antes de que esto llegue a estar frente a una red real.

## Correrlo de forma independiente

```sh
cp .env.example .env      # luego edita KAFKA_CLUSTER_ID, ver más abajo
docker compose up -d --build
```

Genera un cluster ID una vez por stack (no lo reutilices entre distintos entornos):

```sh
docker run --rm apache/kafka:3.9.0 /opt/kafka/bin/kafka-storage.sh random-uuid
```

Kafka UI: http://localhost:8080
Broker (desde el host): `localhost:9094`
Broker (desde otro contenedor en `kafka_net`): `broker:9092`

## Agregar tópicos

Edita `KAFKA_TOPICS` en `.env`:

```
KAFKA_TOPICS=orders:6:1,payments:3:1,notifications:3:1
```

El formato es `nombre:particiones:factor-de-replicación`, separado por comas. Luego corre `docker compose up -d topic-init` para (re)ejecutar la creación — es idempotente (`--if-not-exists`), así que no hay problema en volver a correrlo.

## Prueba manual rápida

```sh
docker compose exec broker /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic example-topic

docker compose exec broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic example-topic --from-beginning
```
