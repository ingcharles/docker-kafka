# Reuses the broker image purely for its bundled kafka-topics.sh /
# kafka-broker-api-versions.sh CLI tools — no extra base image to maintain.
FROM apache/kafka:3.9.0

USER root
COPY create-topics.sh /usr/local/bin/create-topics.sh
RUN chmod +x /usr/local/bin/create-topics.sh
USER appuser

ENTRYPOINT ["/usr/local/bin/create-topics.sh"]
