#!/bin/bash
# Bootstrap idempotente de tópicos. Corre una sola vez contra el broker y
# termina; se mantiene como un servicio de vida corta separado en vez de
# depender de auto.create.topics.enable, que dejamos apagado (ese flag
# crea silenciosamente tópicos mal particionados a partir de errores de
# tipeo en producción).
#
# Formato de KAFKA_TOPICS: nombre:particiones:replicación[:config1;config2...], separados por comas.
# Las configs (p. ej. min.insync.replicas=2;retention.ms=2592000000) también se aplican a los tópicos que ya existen:
# cambiar la retención o el min.insync.replicas y volver a correr este job basta. Lo que Kafka no deja cambiar así
# es el número de particiones por debajo del actual ni el factor de replicación de un tópico existente.
set -euo pipefail

: "${KAFKA_BOOTSTRAP_SERVERS:?KAFKA_BOOTSTRAP_SERVERS must be set}"
: "${KAFKA_TOPICS:?KAFKA_TOPICS must be set, e.g. 'orders:3:1,payments:3:1'}"

KAFKA_BIN=/opt/kafka/bin

echo "Esperando al broker en ${KAFKA_BOOTSTRAP_SERVERS} ..."
until "$KAFKA_BIN/kafka-broker-api-versions.sh" --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" >/dev/null 2>&1; do
  sleep 2
done

IFS=',' read -ra TOPIC_DEFS <<< "$KAFKA_TOPICS"
for raw_def in "${TOPIC_DEFS[@]}"; do
  # Tolera espacios en blanco alrededor de cada entrada (por ejemplo, un
  # "folded scalar" de YAML que une líneas con ", " en vez de ",") para que
  # el formato de KAFKA_TOPICS no sea frágil.
  def="$(echo "$raw_def" | xargs)"
  [ -z "$def" ] && continue
  name="$(cut -d: -f1 <<< "$def")"
  partitions="$(cut -d: -f2 <<< "$def")"
  replication="$(cut -d: -f3 <<< "$def")"
  # Cuarto campo opcional: configs del tópico separadas por ";" (p. ej. cleanup.policy=compact;retention.ms=-1).
  configs="$(cut -d: -f4 <<< "$def")"
  config_args=()
  config_lista=""
  if [ -n "$configs" ]; then
    IFS=";" read -ra CFGS <<< "$configs"
    for c in "${CFGS[@]}"; do
      config_args+=(--config "$c")
      config_lista="${config_lista:+$config_lista,}$c"
    done
  fi

  echo "Asegurando el tópico '${name}' (particiones=${partitions}, replicación=${replication}) ..."
  "$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
    --create --if-not-exists \
    --topic "$name" \
    --partitions "$partitions" \
    --replication-factor "$replication" \
    "${config_args[@]+"${config_args[@]}"}"

  # --create --if-not-exists no toca un tópico que ya existe: las configs se convergen aparte.
  if [ -n "$config_lista" ]; then
    "$KAFKA_BIN/kafka-configs.sh" --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
      --alter --entity-type topics --entity-name "$name" --add-config "$config_lista"
  fi
done

echo "Bootstrap de tópicos completo."
"$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" --list
