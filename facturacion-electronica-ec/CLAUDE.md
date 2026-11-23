# Facturación electrónica SRI (Quarkus + Kafka)

7 microservicios Quarkus (`identidad`, `firma-electronica`, `inventario`, `comprobantes`, `sri-conector`, `notificacion`,
`facturacion-orchestrator`) y el jar compartido `facturacion-commons`. El detalle de diseño está en `ARQUITECTURA.md`; si
cambias el comportamiento, actualiza ese documento **en presente y en corto, sin historial** ("antes/ahora").

## Compilar y probar
- Desde esta carpeta: `mvn -B test` (todo) o `mvn -B package` (incluye la validación de Quarkus, que `test` no hace).
- Un servicio suelto usa el `facturacion-commons` instalado en `~/.m2`: tras tocar commons, `mvn -f facturacion-commons/pom.xml install -DskipTests`.
- Ambientes: `env/desarrollo.env`, `env/pruebas.env` y `env/produccion.env` (versionados, **sin secretos**) más `env/<ambiente>.secretos.env` (ignorado por git; plantilla `env/secretos.env.example`). Se levantan con `./scripts/compose.sh <ambiente> up -d` (guía paso a paso en `env/README.md`). Nunca pongas una contraseña, clave o token en un archivo versionado.
- Un build completo tarda ~10 min: córrelo en segundo plano. En este equipo (Windows) no hay `python`; para ediciones masivas usa `perl`.

## Reglas del código

**Arquitectura hexagonal.** `domain` no importa `application` ni `infrastructure`; `application` no importa `infrastructure`.
Lo que un caso de uso necesita del exterior (BD, Kafka, SOAP, correo, cifrado, PDF) lo pide por un puerto (`domain.port`,
`domain.repository`) y su implementación va en `infrastructure`. Los DTO (`application.dto`) son solo datos: campos, accesores y constructores, sin lógica ni métodos adicionales (ni `desde(...)`, ni cálculos, ni validaciones a mano); toda conversión va en el mapper. Mappers dominio→DTO en `application.mapper`; entidad↔dominio
en `infrastructure.persistence.mapper`. La lógica de negocio va en el dominio (p. ej. `Comprobante.marcarAutorizado`), no en
setters sueltos desde el servicio.

**REST que escribe** (`@POST/@PUT/@PATCH/@DELETE`): devuelve `Uni<Response>` armado con `Respuestas` (facturacion-commons):
crear = `201` + recurso (+ `Location` si tiene `GET`), actualizar = `200` + recurso, borrar/orden sin resultado = `204`,
asíncrono = `202`. Nunca un DTO suelto ni `Response.status(CREATED)` a mano. Errores: lanzar una `ApiException`.

**Errores y mensajes.** Cada error tiene su entrada en el enum del servicio (`CodigoError`, `FirmaError`, `IdentidadError`…),
con el texto en `mensajes.properties`; un único `ApiExceptionMapper` los convierte. No devuelvas `boolean`/`null`/códigos para
errores.

**Excepciones.** Toda excepción que puede llegar al cliente es una `ApiException` con su entrada del catálogo y parámetros, nunca con texto fijo: `new NotFoundException(CodigoError.ITEM_NO_ENCONTRADO, id)`, `new ConflictoException(...)`, `new SinPermisoException(...)`, `new ErrorNegocioException(...)`; el estado y el texto salen del enum y del `mensajes.properties`, y no existen constructores que reciban un `String`. Lo que es del tipo de api-key (otro RUC, otro establecimiento, rol) se pide con `TokenPrincipal.exigirEmpresa/exigirAlcance/exigirEscritura/exigirAdmin`, no con un `throw` en cada recurso. Una falla de infraestructura que puede ocurrir en ejecución (serializar un payload, generar un XML o un PDF, cifrar, contactar al SRI) lanza `ErrorInternoException(causa, ENTRADA)`: el cliente recibe el texto genérico y el código (5xx), y el detalle con su causa queda en el log `GEN-403`. `IllegalStateException` / `IllegalArgumentException` quedan solo para invariantes de programación (un estado que no debería ocurrir) y validación de configuración al arrancar; nunca para un error que el usuario pueda ver ni para un mensaje de Kafka inválido (ese es una `ErrorNegocioException`).

**Fechas y zona horaria.** Todo formato o lectura de fecha pasa por `FechasSri` y `ZonaHoraria` de `facturacion-commons` (`FechasSri.formatearFecha(instante)`, `leerFecha`, `obtenerHoy`, ...), siempre en hora de Ecuador. No se crea un `DateTimeFormatter`, un `ZoneOffset` ni un `LocalDate.now()` en un servicio: la fecha de "hoy" es la de Ecuador, no la del servidor.

**Nada bloqueante.** El código de producción no usa `.await()`, `Thread.sleep` ni `.join()`. El trabajo bloqueante o de CPU (firma, SOAP, PDF, archivos, bcrypt) va con `TrabajoBloqueante.ejecutar(...)` de `facturacion-commons`; no se arma a mano `runSubscriptionOn` + `emitOn`. Un consumidor que llama a algo lento (SRI, SMTP, firma) no mantiene una transacción abierta mientras espera: lectura corta, trabajo sin base de datos y una transacción corta con el resultado y el inbox.

**`void` solo si no hay nada que informar** (consumidores de Kafka, `DELETE`). Si la operación puede no hacer nada (comando
repetido, evento fuera de estado), devuelve un resultado (`ResultadoComando`, `ResultadoEvento`) y regístralo.

**Kafka.** Escritura de negocio + fila de outbox en la misma transacción (puerto `EventosSalientes`); consumidores
idempotentes por inbox (`MensajesProcesados`). No compartas records de mensaje ni entidades JPA entre servicios.

**Seguridad.** Todo endpoint exige api-key (JWT Ed25519 emitido por identidad, verificado localmente con la llave pública).
Una api-key vale para una sola empresa y un solo rol. No loguees cédulas, correos, razón social, tokens, api-keys ni
contraseñas; usa ids (`comprobanteId`, `claveAcceso`).

**Sin valores quemados.** Ningún literal con significado de negocio o de formato (códigos del SRI, porcentajes, anchos, valores por defecto, nombres de parámetros, rótulos, tipos de contenido, decimales, límites) va suelto en el código: o es una constante con nombre (`private static final`, y `public` si una anotación la necesita) o es un enum. Regla: los códigos estables del SRI que el código trata con lógica propia (tipo de impuesto, tipo de comprobante, ambiente, clasificación de IVA, tipo de tarifa) son enums del dominio con su comportamiento dentro (`ImpuestoSri.IVA.esCodigo(codigo)`), no `"2"` ni constantes repetidas por clase. Lo que cambia con la normativa (tarifas de IVA, ICE e IRBPNR, códigos y porcentajes de retención, formas de pago) NO va en código: vive en el catálogo (`tarifa_impuesto`, `retencion`...) y se cambia con `PUT /catalogos/{catalogo}` sin desplegar. Los valores por defecto de un parámetro REST son constantes del enum o clase del dominio al que pertenecen.

**Estilo.** Métodos con nombre en español que empiece con verbo en infinitivo (`crear`, `obtener`, `registrar`); predicados
`esX`/`tieneX`/`hayX`. Un método que calcula, arma o convierte lleva el verbo (`calcularAltoCajaComprador`, `armarEncabezado`, `convertirAMapa`), no un sustantivo suelto (`dinero`, `encabezado`, `texto`). Las únicas excepciones son las convenciones de otra herramienta: accesores de records, enums y `@ConfigMapping` (`codigo()`, `claveMensaje()`), `map*` de MapStruct, `existsBy*`/`find*`/`persist` de Panache, los niveles de `LogCodificado` (`error`, `info`...), las fábricas con nombre de `Respuestas` (`Respuestas.creado`) y los constructores de columnas del DSL de `CatalogoDef` (`texto`, `decimal`...). Ninguna clase va con su nombre completo en el código (`java.time.Duration timeout;`, `@io.quarkus...WithSession`): se importa y se usa el nombre corto. Imports ordenados:
`java`/`javax`, `jakarta`, `org`, `io`, otros `com`, `com.facturacion`; separados por una línea en blanco y sin imports sin uso.

**Base de datos.** Las migraciones Flyway ya aplicadas no se editan: un cambio es una migración nueva. Toda tabla o columna nueva lleva su
`COMMENT ON TABLE/COLUMN` en la misma migración (qué guarda, valores posibles, unidades).

**Logs.** Todo log del código propio (`DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`) lleva código y se registra con `LogCodificado`, nunca con `LOG.xxx(...)` suelto: `[FIR-401] texto (comprobanteId=…)`. El código es `<PREFIJO>-<NNN>` y el primer dígito es el nivel (1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL). Declara el código en el `CodigoLog` del servicio, su texto en `mensajes.properties` y la fila en `CATALOGO_ERRORES.md`. Todo consumidor de Kafka (`@Incoming`) lleva `@LogFallosMensajeria`. El contexto lleva solo ids.

**Pruebas.** El nombre de todo método de prueba empieza con `testDeberia` (`testDeberiaRechazarUnCorreoSinArroba`). Toda lógica nueva lleva prueba unitaria. Para repositorios usa dobles en memoria o `Proxy` (no hace falta una
base de datos); mapea con `Uni…await()`.
