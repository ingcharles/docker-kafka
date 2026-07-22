#!/bin/bash
# Idempotent topic bootstrap. Runs once against the broker and exits;
# kept as a separate short-lived service instead of relying on
# auto.create.topics.enable, which we leave off (that flag silently
# creates mis-partitioned topics from typos in production).
set -euo pipefail

: "${KAFKA_BOOTSTRAP_SERVERS:?KAFKA_BOOTSTRAP_SERVERS must be set}"
: "${KAFKA_TOPICS:?KAFKA_TOPICS must be set, e.g. 'orders:3:1,payments:3:1'}"

KAFKA_BIN=/opt/kafka/bin

echo "Waiting for broker at ${KAFKA_BOOTSTRAP_SERVERS} ..."
until "$KAFKA_BIN/kafka-broker-api-versions.sh" --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" >/dev/null 2>&1; do
  sleep 2
done

IFS=',' read -ra TOPIC_DEFS <<< "$KAFKA_TOPICS"
for def in "${TOPIC_DEFS[@]}"; do
  name="$(cut -d: -f1 <<< "$def")"
  partitions="$(cut -d: -f2 <<< "$def")"
  replication="$(cut -d: -f3 <<< "$def")"

  echo "Ensuring topic '${name}' (partitions=${partitions}, replication=${replication}) ..."
  "$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
    --create --if-not-exists \
    --topic "$name" \
    --partitions "$partitions" \
    --replication-factor "$replication"
done

echo "Topic bootstrap complete."
"$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" --list
