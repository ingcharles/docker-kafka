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

El formato es `nombre:particiones:factor-de-replicación[:config1;config2]`, separado por comas; las configs (p. ej. `min.insync.replicas=2;retention.ms=604800000`) se aplican también a los tópicos que ya existen. Lo que Kafka no deja cambiar así es el factor de replicación de un tópico existente. Luego corre `docker compose up -d topic-init` para (re)ejecutar la creación — es idempotente (`--if-not-exists`), así que no hay problema en volver a correrlo.

## Prueba manual rápida

```sh
docker compose exec broker /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic example-topic

docker compose exec broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic example-topic --from-beginning
```

## Con réplicas: pruebas (2 nodos) y producción (3 nodos)

`docker-compose.ha.yml` levanta nodos KRaft (cada uno broker y controlador), con elección de líder solo entre réplicas sincronizadas y la creación automática de tópicos apagada. El `docker-compose.yml` de un solo nodo es para desarrollo.

| | Pruebas | Producción |
|---|---|---|
| Nodos | 2 | 3 (perfil `tres-nodos`) |
| `replication.factor` / `min.insync.replicas` | 2 / 2 | 3 / 2 |
| Un nodo caído | las escrituras se detienen (con 2 controladores no hay mayoría) y vuelven solas a los ~12 s de volver el nodo; lo confirmado no se pierde | se sigue produciendo y consumiendo sin perder mensajes |
| Dos nodos caídos | — | las escrituras se rechazan (`NOT_ENOUGH_REPLICAS`) a propósito; al volver, las réplicas se sincronizan solas |

```sh
# pruebas
docker compose --env-file env/pruebas.env -f docker-compose.ha.yml up -d --build
# producción
docker compose --env-file env/produccion.env -f docker-compose.ha.yml up -d --build
```

Las variables de cada ambiente están en `env/pruebas.env` y `env/produccion.env` (el `KAFKA_CLUSTER_ID` se genera una vez por clúster); la lista completa, en `facturacion-electronica-ec/documentacion/VARIABLES_DE_ENTORNO.md`. Los servicios se conectan con `KAFKA_BOOTSTRAP_SERVERS` y los tópicos se crean con `KAFKA_REPLICATION_FACTOR` y `KAFKA_MIN_ISR` (`facturacion-electronica-ec/env/<ambiente>.env`).

Un clúster de un solo nodo existente no se convierte en uno de varios: los tópicos con replicación 1 hay que recrearlos (o reasignar sus réplicas con `kafka-reassign-partitions.sh`). Para probar otro clúster al lado del de desarrollo: otro `KAFKA_NETWORK`, otros puertos y `-p <nombre>`.
