# Reutiliza la imagen del broker únicamente por sus herramientas CLI
# incluidas kafka-topics.sh / kafka-broker-api-versions.sh — así no hay
# que mantener una imagen base extra.
FROM apache/kafka:3.9.0

USER root
COPY create-topics.sh /usr/local/bin/create-topics.sh
RUN chmod +x /usr/local/bin/create-topics.sh
USER appuser

ENTRYPOINT ["/usr/local/bin/create-topics.sh"]
