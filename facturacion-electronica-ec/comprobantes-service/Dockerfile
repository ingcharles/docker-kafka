# Builder: compila el layout fast-jar de Quarkus. Las dependencias se
# resuelven en su propia capa para que un pom.xml sin cambios mantenga
# esa capa cacheada entre builds.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
# facturacion-commons (jar compartido) se construye e instala primero en el repo Maven de esta etapa.
COPY facturacion-commons/pom.xml /commons/pom.xml
COPY facturacion-commons/src /commons/src
RUN mvn -q -B -f /commons/pom.xml -DskipTests install

COPY comprobantes-service/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY comprobantes-service/src ./src
RUN mvn -q -B -DskipTests package

# Runtime: solo JRE, no-root, únicamente el layout fast-jar copiado.
FROM eclipse-temurin:21-jre-alpine
# Fuente metricamente compatible con Helvetica: sin ella PDFBox avisa "Using fallback font" en cada RIDE.
RUN apk add --no-cache font-liberation && addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=app:app /build/target/quarkus-app/*.jar ./
COPY --from=build --chown=app:app /build/target/quarkus-app/app/ ./app/
COPY --from=build --chown=app:app /build/target/quarkus-app/quarkus/ ./quarkus/
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 \
  CMD wget -qO- http://localhost:8080/comprobantes/api/v1/q/health/ready || exit 1
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
