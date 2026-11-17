# Facturación electrónica SRI (Quarkus + Kafka)

7 microservicios Quarkus (`identidad`, `firma-electronica`, `inventario`, `comprobantes`, `sri-conector`, `notificacion`,
`facturacion-orchestrator`) y el jar compartido `facturacion-commons`. El detalle de diseño está en `ARQUITECTURA.md`; si
cambias el comportamiento, actualiza ese documento **en presente y en corto, sin historial** ("antes/ahora").

## Compilar y probar
- Desde esta carpeta: `mvn -B test` (todo) o `mvn -B package` (incluye la validación de Quarkus, que `test` no hace).
- Un servicio suelto usa el `facturacion-commons` instalado en `~/.m2`: tras tocar commons, `mvn -f facturacion-commons/pom.xml install -DskipTests`.
- Un build completo tarda ~10 min: córrelo en segundo plano. En este equipo (Windows) no hay `python`; para ediciones masivas usa `perl`.

## Reglas del código

**Arquitectura hexagonal.** `domain` no importa `application` ni `infrastructure`; `application` no importa `infrastructure`.
Lo que un caso de uso necesita del exterior (BD, Kafka, SOAP, correo, cifrado, PDF) lo pide por un puerto (`domain.port`,
`domain.repository`) y su implementación va en `infrastructure`. Mappers dominio→DTO en `application.mapper`; entidad↔dominio
en `infrastructure.persistence.mapper`. La lógica de negocio va en el dominio (p. ej. `Comprobante.marcarAutorizado`), no en
setters sueltos desde el servicio.

**REST que escribe** (`@POST/@PUT/@PATCH/@DELETE`): devuelve `Uni<Response>` armado con `Respuestas` (facturacion-commons):
crear = `201` + recurso (+ `Location` si tiene `GET`), actualizar = `200` + recurso, borrar/orden sin resultado = `204`,
asíncrono = `202`. Nunca un DTO suelto ni `Response.status(CREATED)` a mano. Errores: lanzar una `ApiException`.

**Errores y mensajes.** Cada error tiene su entrada en el enum del servicio (`CodigoError`, `FirmaError`, `IdentidadError`…),
con el texto en `mensajes.properties`; un único `ApiExceptionMapper` los convierte. No devuelvas `boolean`/`null`/códigos para
errores.

**`void` solo si no hay nada que informar** (consumidores de Kafka, `DELETE`). Si la operación puede no hacer nada (comando
repetido, evento fuera de estado), devuelve un resultado (`ResultadoComando`, `ResultadoEvento`) y regístralo.

**Kafka.** Escritura de negocio + fila de outbox en la misma transacción (puerto `EventosSalientes`); consumidores
idempotentes por inbox (`MensajesProcesados`). No compartas records de mensaje ni entidades JPA entre servicios.

**Seguridad.** Todo endpoint exige api-key (JWT Ed25519 emitido por identidad, verificado localmente con la llave pública).
Una api-key vale para una sola empresa y un solo rol. No loguees cédulas, correos, razón social, tokens, api-keys ni
contraseñas; usa ids (`comprobanteId`, `claveAcceso`).

**Estilo.** Métodos con nombre en español que empiece con verbo en infinitivo (`crear`, `obtener`, `registrar`); predicados
`esX`/`tieneX`/`hayX`; los accesores de records y los métodos de MapStruct/Panache siguen su convención. Imports ordenados:
`java`/`javax`, `jakarta`, `org`, `io`, otros `com`, `com.facturacion`; separados por una línea en blanco y sin imports sin uso.

**Base de datos.** Las migraciones Flyway ya aplicadas no se editan: un cambio es una migración nueva. Toda tabla o columna nueva lleva su
`COMMENT ON TABLE/COLUMN` en la misma migración (qué guarda, valores posibles, unidades).

**Logs.** Todo log del código propio (`DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`) lleva código y se registra con `LogCodificado`, nunca con `LOG.xxx(...)` suelto: `[FIR-401] texto (comprobanteId=…)`. El código es `<PREFIJO>-<NNN>` y el primer dígito es el nivel (1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL). Declara el código en el `CodigoLog` del servicio, su texto en `mensajes.properties` y la fila en `CATALOGO_ERRORES.md`. Todo consumidor de Kafka (`@Incoming`) lleva `@LogFallosMensajeria`. El contexto lleva solo ids.

**Pruebas.** Toda lógica nueva lleva prueba unitaria. Para repositorios usa dobles en memoria o `Proxy` (no hace falta una
base de datos); mapea con `Uni…await()`.
