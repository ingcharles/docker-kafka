# Kafka stack

Single-node Apache Kafka in KRaft mode (no Zookeeper) plus a topic
bootstrap job and a web UI. Three services:

- **broker** — `apache/kafka`, combined broker+controller node.
- **topic-init** — one-shot container that waits for the broker and
  creates the topics listed in `KAFKA_TOPICS`, then exits.
- **kafka-ui** — `provectuslabs/kafka-ui`, browser console for topics,
  messages and consumer groups at `:8080`.

## Design notes

- KRaft, not Zookeeper: fewer moving parts, and Zookeeper is being
  phased out upstream.
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` — topics are created
  explicitly by `topic-init`, not implicitly on first produce. A typo
  in a topic name then fails loudly instead of silently creating a
  1-partition topic you'll regret later.
- Two listeners: `PLAINTEXT` (`broker:9092`) for other containers on
  `kafka_net`, `PLAINTEXT_HOST` (`localhost:9094` by default) for
  anything connecting from the host machine (scripts, IDE plugins).
- PLAINTEXT only — there's no TLS/SASL here. Fine for local dev; add
  `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP`/SASL config before this ever
  faces a real network.

## Run standalone

```sh
cp .env.example .env      # then edit KAFKA_CLUSTER_ID, see below
docker compose up -d --build
```

Generate a cluster ID once per stack (don't reuse across environments):

```sh
docker run --rm apache/kafka:3.9.0 /opt/kafka/bin/kafka-storage.sh random-uuid
```

Kafka UI: http://localhost:8080
Broker (from host): `localhost:9094`
Broker (from another container on `kafka_net`): `broker:9092`

## Adding topics

Edit `KAFKA_TOPICS` in `.env`:

```
KAFKA_TOPICS=orders:6:1,payments:3:1,notifications:3:1
```

Format is `name:partitions:replication-factor`, comma-separated. Then
`docker compose up -d topic-init` to (re)run the bootstrap — it's
idempotent (`--if-not-exists`).

## Manual smoke test

```sh
docker compose exec broker /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic example-topic

docker compose exec broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic example-topic --from-beginning
```
