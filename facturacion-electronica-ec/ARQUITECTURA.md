# Arquitectura — Facturación Electrónica SRI Ecuador (Java/Quarkus + Kafka)

Este documento explica **cómo está construido** el stack: el estilo arquitectónico, cómo se relaciona cada microservicio con los demás, el flujo completo de negocio de principio a fin (incluyendo la integración real con el SRI), el diseño detallado de Kafka que hace posible que los servicios de dominio se coordinen sin conocerse entre sí, y la capa de autenticación multi-tenant que protege todo el ecosistema. Sigue el mismo patrón de saga por orquestación que `ecommerce/` (ver `../ecommerce/ARQUITECTURA.md`), pero con una diferencia central: aquí **no hay simulaciones** — la firma digital, el envío al SRI y el envío de correo son integraciones reales con sistemas externos de verdad.

## 1. Estilo arquitectónico

| Decisión | Qué significa aquí |
|---|---|
| **Microservicios con base de datos propia** | 7 servicios Quarkus independientes (6 de dominio + `identidad-service`), cada uno dueño de su propio esquema de Postgres. Ninguno lee ni escribe directamente en las tablas de otro. |
| **Saga por orquestación** (no coreografía) | `facturacion-orchestrator-service` es el **único** que conoce el flujo completo (CREADO→FIRMADO→AUTORIZADO→NOTIFICADO). Los otros 5 no saben que forman parte de una saga — solo saben "me llegó un comando, lo ejecuto, publico si me salió bien o mal". |
| **Comunicación casi 100% asíncrona vía Kafka** | Ningún microservicio expone ni llama un endpoint REST de otro para el flujo transaccional. La única excepción deliberada es la subida del certificado `.p12` (`POST /empresas/{id}/certificado`, multipart — no tiene sentido modelar la subida de un archivo binario como un evento de Kafka). Todo lo demás — incluida la réplica de datos de Empresa (hacia `identidad-service`) e Item (hacia `comprobantes-service`) entre servicios — viaja por eventos. `identidad-service` no participa de la saga y no publica nada a Kafka, pero sí **consume** `facturacion.empresa.eventos.v1` para saber qué rucs tienen una Empresa real (ver sección 8) — ningún otro servicio le llama de vuelta. |
| **Todo el REST externo requiere api-key** | Los 6 servicios de dominio no tienen ninguna ruta pública — cada request necesita `Authorization: Bearer <api-key>`, un JWT emitido por `identidad-service` y verificado localmente (sin llamada de red) por cada servicio. Ver sección 8. |
| **Arquitectura hexagonal / DDD por servicio** | Cada servicio se organiza en capas `domain` (modelo puro, sin anotaciones de framework) → `application` (casos de uso) → `infrastructure` (JPA/Panache, Kafka, REST, SOAP, XML-DSig) → `shared`. El dominio no depende de Quarkus, Kafka, ni de las librerías de firma/SOAP; es al revés. |
| **Transactional Outbox + Inbox idempotente** | Ver sección 6. Igual que en `ecommerce/`. |
| **Stack reactivo** | Quarkus + Java 21 + Mutiny + Hibernate Reactive Panache + SmallRye Reactive Messaging. El I/O bloqueante que no tiene alternativa reactiva (firma XML-DSig del JDK, `java.net.http.HttpClient` SOAP hacia el SRI, `quarkus-mailer`) se offloadea al worker pool y se retoma en el contexto de Vert.x correcto — ver [[feedback-quarkus-reactive-kafka-gotchas]]. |
| **Integraciones externas reales, no simuladas** | A diferencia de `ecommerce/` (pago/envío/notificación simulados a propósito para poder reproducir cualquier camino sin depender de terceros), aquí la firma digital (XAdES-BES sobre un `.p12` real), el envío/autorización SOAP contra el SRI (`celcer.sri.gob.ec`) y el envío de correo (SMTP real) son genuinos. Es el punto central de este proyecto. |

### Diagrama de despliegue

```mermaid
graph TB
    Cliente["Cliente externo<br/>(ERP / POS / integración directa)"]
    SRI["SRI (celcer.sri.gob.ec)<br/>Recepción + Autorización, SOAP real"]
    Gmail["Servidor SMTP real<br/>(Gmail u otro)"]

    subgraph Docker["Docker — 2 stacks"]
        subgraph KafkaStack["kafka/ (compartido)"]
            Broker["broker<br/>Kafka KRaft :9091(host)"]
            KafkaUI["kafka-ui<br/>:8080"]
        end

        subgraph FacturacionStack["facturacion-electronica-ec/"]
            Identidad["identidad-service<br/>:8097 — usuarios, empresas/ruc, roles, api-keys JWT"]
            Firma["firma-electronica-service<br/>:8091 — Empresa + certificado + firma XAdES-BES"]
            Inventario["inventario-service<br/>:8092 — catálogo, kardex, impuestos"]
            Comprobantes["comprobantes-service<br/>:8093 — 6 tipos de comprobante, XML, RIDE"]
            Sri["sri-conector-service<br/>:8094 — SOAP real hacia el SRI"]
            Notificacion["notificacion-service<br/>:8095 — envío de RIDE por correo"]
            Orquestador["facturacion-orchestrator-service<br/>:8096 — la saga completa"]
            DB[("Postgres facturacion<br/>1 esquema por servicio")]
        end
    end

    Cliente -->|"REST: login, api-keys"| Identidad
    Cliente -->|"REST + api-key: POST /facturas, /notas-credito, etc."| Comprobantes
    Cliente -.->|"REST + api-key: consulta de estado"| Orquestador
    Cliente -.->|"REST + api-key: alta de Empresa + certificado"| Firma
    Cliente -.->|"REST + api-key: catálogo"| Inventario

    Identidad -.->|"mismo secreto HS256<br/>IDENTIDAD_JWT_SECRET,<br/>sin llamada de red"| Firma
    Identidad -.-> Inventario
    Identidad -.-> Comprobantes
    Identidad -.-> Sri
    Identidad -.-> Notificacion
    Identidad -.-> Orquestador

    Firma <-->|Kafka| Broker
    Inventario <-->|Kafka| Broker
    Comprobantes <-->|Kafka| Broker
    Sri <-->|Kafka| Broker
    Notificacion <-->|Kafka| Broker
    Orquestador <-->|Kafka| Broker
    Broker -->|"Kafka, solo entrante<br/>(EmpresaRegistrada/Actualizado)"| Identidad

    Sri <-->|"SOAP real"| SRI
    Notificacion -->|"SMTP real"| Gmail

    Identidad --- DB
    Firma --- DB
    Inventario --- DB
    Comprobantes --- DB
    Sri --- DB
    Notificacion --- DB
    Orquestador --- DB

    KafkaUI -.->|solo lectura, observabilidad| Broker
```

**Lectura clave del diagrama:** el cliente primero le habla a `identidad-service` (crear cuenta, login, asociarse a un `ruc`, emitir una api-key) y luego usa esa api-key como `Authorization: Bearer` contra todo lo demás: `comprobantes-service` (para crear cualquiera de los 6 tipos de comprobante), `firma-electronica-service` (para dar de alta un Empresa y subir su certificado) y opcionalmente `facturacion-orchestrator-service` (para consultar el estado de la saga). Las flechas punteadas de `identidad-service` hacia el resto **no son llamadas de red** — representan que todos comparten el mismo secreto HS256 y verifican el JWT de forma local (ver sección 8). `sri-conector-service` es el único servicio que sale fuera de Docker hacia una red externa real (el SRI); `notificacion-service` es el único que sale hacia un servidor SMTP real. Todos los demás servicios son invisibles para el cliente — su única puerta de entrada/salida es Kafka.

## 2. Los 7 microservicios y cómo se relacionan entre sí

| Servicio | Bounded context | Esquema BD | Ruta REST | A quién le manda comandos (Kafka) | Quién le manda comandos a él |
|---|---|---|---|---|---|
| `identidad-service` | Usuarios, asociación usuario↔ruc con rol, emisión/revocación de api-keys JWT | `identidad` | `/identidad/api/v1` | *(no publica nada — solo consume `facturacion.empresa.eventos.v1` para validar rucs, ver sección 8)* | *(nadie le manda comandos — no participa de la saga)* |
| `firma-electronica-service` | Empresa (multi-tenant) + certificado digital + firma XAdES-BES | `firma` | `/empresas/api/v1` | *(nadie, solo responde)* | `facturacion-orchestrator-service` (`FirmarComprobanteCommand`) |
| `inventario-service` | Catálogo (productos/servicios), kardex, impuestos | `inventario` | `/inventario/api/v1` | *(nadie, es origen de eventos, no recibe comandos de la saga)* | *(nadie — no participa en la saga de emisión)* |
| `comprobantes-service` | Los 6 tipos de comprobante SRI, XML, RIDE | `comprobantes` | `/comprobantes/api/v1` | *(nadie — es el origen de la saga)* | `facturacion-orchestrator-service` (`MARCAR_FIRMADO`/`MARCAR_AUTORIZADO`/`MARCAR_NO_AUTORIZADO`/etc.) |
| `sri-conector-service` | Integración SOAP real con el SRI (Recepción + Autorización) | `sri` | `/sri/api/v1` | *(nadie, solo responde)* | `facturacion-orchestrator-service` (`EnviarComprobanteCommand`) |
| `notificacion-service` | Envío del RIDE autorizado por correo al receptor | `notificacion` | `/notificaciones/api/v1` | *(nadie, solo responde)* | *(nadie — no es comandado, actúa directamente sobre el evento `ComprobanteAutorizado`)* |
| `facturacion-orchestrator-service` | La saga completa de emisión | `orquestacion` | `/sagas/api/v1` | `comprobantes-service`, `firma-electronica-service`, `sri-conector-service` | `comprobantes-service` (evento `ComprobanteCreado`, el disparador inicial) |

**Por qué la relación tiene forma de estrella y no de cadena:** los servicios de dominio (firma, comprobantes, sri, notificación) **no se conocen entre sí** — ninguno le manda un mensaje directamente a otro (con la única excepción documentada abajo). Todos hablan únicamente con `facturacion-orchestrator-service`, que decide el orden de ejecución. Igual que en `ecommerce/`, si un cambio en el flujo de negocio agrega o reordena un paso, ese conocimiento vive en un solo lugar: `SagaOrchestratorService.java` del orquestador.

**Las dos excepciones deliberadas a "cero REST/acoplamiento directo entre servicios":**
1. `inventario-service` e `firma-electronica-service` no participan en la saga de comandos — son **fuentes de datos de referencia**. `inventario-service` publica `ItemCreado`/`ItemActualizado`, que `comprobantes-service` consume y mantiene como copia de solo lectura (`item_local`, usada para auto-derivar impuestos por `itemId`) — vista materializada vía evento. `firma-electronica-service` publica `EmpresaRegistrada`/`EmpresaActualizada` (sin datos del certificado); hoy el único consumidor es `identidad-service` (`empresa_local`, ver sección 8) — `comprobantes-service` **ya no** mantiene una réplica de Empresa: desde que el modelo pasó a ser scoped por `ruc` en vez de por `empresaId`, cada request de creación de comprobante trae los datos de la empresa inline (ver `V6__ecosistema_separado.sql`), así que no necesita consultar nada localmente.
2. `notificacion-service` no es comandado por el orquestador — consume directamente `ComprobanteAutorizado` desde `facturacion.comprobante.eventos.v1` (publicado por `comprobantes-service`, no por el orquestador) porque no hay ninguna decisión de negocio que tomar antes de notificar: si un comprobante quedó `AUTORIZADO` y tiene un correo, se notifica, punto. El orquestador igual observa `NotificacionExitosa`/`NotificacionFallida` para completar su propia máquina de estados, pero no es quien dispara el envío.

**Independencia real, no solo de código:** los servicios solo comparten `facturacion-commons` (ver sección 12): errores/excepciones con su mapper, verificación del JWT, paginación — **infraestructura transversal sin lógica de negocio ni contratos de mensajes**. Los records de mensaje Kafka NO se comparten: cada servicio define los suyos en su paquete `application/event`, aunque el *shape* del JSON coincida con el que espera el consumidor. Mismo trade-off que en `ecommerce/`: pequeña duplicación de definiciones a cambio de poder redesplegar cada servicio de forma independiente.

## 3. Flujo completo de emisión de un comprobante (camino feliz)

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant CO as comprobantes-service
    participant OR as facturacion-orchestrator
    participant FI as firma-electronica-service
    participant SR as sri-conector-service
    participant SRI as SRI (real)
    participant NO as notificacion-service
    participant SMTP as Servidor SMTP (real)

    C->>CO: POST /comprobantes/api/v1/facturas (o notas-credito, retenciones, etc.)
    activate CO
    CO->>CO: valida, deriva impuestos desde item_local, guarda Comprobante (estado=BORRADOR) + fila outbox (misma transacción)
    CO-->>C: 201 Created
    deactivate CO
    CO--)OR: (Kafka) ComprobanteCreado (incluye ambiente, xmlGenerado)

    activate OR
    OR->>OR: crea saga_instance (state=CREADO)
    OR--)FI: (Kafka) FirmarComprobanteCommand
    deactivate OR

    activate FI
    FI->>FI: firma XML con XAdES-BES usando el .p12 activo del Empresa (descifrado en memoria)
    FI--)OR: (Kafka) ComprobanteFirmado (xmlFirmado)
    deactivate FI

    activate OR
    OR->>OR: state: CREADO -> FIRMADO
    OR--)CO: (Kafka) MARCAR_FIRMADO
    OR--)SR: (Kafka) EnviarComprobanteCommand (xmlFirmado)
    deactivate OR

    activate SR
    SR->>SRI: SOAP Recepción (inmediata, sin backoff)
    SRI-->>SR: RECIBIDA | DEVUELTA
    SR--)OR: (Kafka) SriRecibido
    Note over SR: poller @Scheduled cada 30s, hasta 20 intentos
    SR->>SRI: SOAP Autorización
    SRI-->>SR: AUTORIZADO | NO AUTORIZADO
    SR--)OR: (Kafka) SriAutorizado (xmlAutorizado, numeroAutorizacion)
    deactivate SR

    activate OR
    OR->>OR: state: FIRMADO -> AUTORIZADO
    OR--)CO: (Kafka) MARCAR_AUTORIZADO
    deactivate OR

    activate CO
    CO->>CO: guarda xmlAutorizado, genera RIDE (PDF), estado=AUTORIZADO
    CO--)NO: (Kafka) ComprobanteAutorizado (RIDE en base64, solo si hay correo)
    deactivate CO

    activate NO
    NO->>SMTP: envío real (asunto, RIDE adjunto)
    SMTP-->>NO: 250 OK | error SMTP
    NO--)OR: (Kafka) NotificacionExitosa | NotificacionFallida
    deactivate NO

    activate OR
    OR->>OR: state: AUTORIZADO -> NOTIFICADO (o se queda en AUTORIZADO si falla la notificación)
    deactivate OR
```

**Puntos importantes de este flujo:**

1. **El cliente nunca espera a la saga completa.** El `POST /comprobantes/...` devuelve `201` en cuanto el comprobante queda guardado como `BORRADOR` — firmar, enviar al SRI y notificar ocurre después, de forma asíncrona. Para saber si terminó, el cliente hace polling a `GET /sagas/api/v1/{comprobanteId}`.
2. **Cada flecha `--)` (Kafka) implica, por debajo, el patrón outbox → relay → broker → inbox → efecto** (ver sección 6), igual que en `ecommerce/`.
3. **La autorización del SRI puede tardar** — el SRI documenta hasta 24h de SLA, aunque en la práctica observada contra el ambiente de pruebas responde en segundos. El poller de `sri-conector-service` está diseñado para esa espera (backoff, no polling agresivo).
4. **El correo es la única rama verdaderamente "best effort"**: un comprobante `AUTORIZADO` ya es legalmente válido tenga o no tenga correo, y tenga o no éxito el envío — ver sección 5.
5. **Tiempo real medido de punta a punta** (CREADO→AUTORIZADO, con el certificado real del usuario, contra el SRI real): ~19 segundos en el peor caso observado.

## 4. Máquina de estados de la saga

```mermaid
stateDiagram-v2
    [*] --> CREADO: ComprobanteCreado
    CREADO --> FIRMADO: ComprobanteFirmado
    FIRMADO --> AUTORIZADO: SriAutorizado
    AUTORIZADO --> NOTIFICADO: NotificacionExitosa
    AUTORIZADO --> NOTIFICACION_FALLIDA: NotificacionFallida

    CREADO --> FIRMA_FALLIDA: ComprobanteFirmaFallida
    FIRMADO --> RECHAZADO_RECEPCION: SriRechazadoRecepcion
    FIRMADO --> NO_AUTORIZADO: SriNoAutorizado

    AUTORIZADO --> AUTORIZADO_SIN_NOTIFICACION: SriAutorizado, comprobante sin correo
    AUTORIZADO_SIN_NOTIFICACION --> [*]
    NOTIFICADO --> [*]
    NOTIFICACION_FALLIDA --> [*]
    FIRMA_FALLIDA --> [*]
    RECHAZADO_RECEPCION --> [*]
    NO_AUTORIZADO --> [*]

    CREADO --> EXPIRADA: sin respuesta tras N reintentos (watchdog)
    FIRMADO --> EXPIRADA: sin respuesta tras N reintentos (watchdog)
    EXPIRADA --> FIRMADO: respuesta tardía de firma
    EXPIRADA --> FIRMA_FALLIDA: respuesta tardía de firma (fallo)
    EXPIRADA --> AUTORIZADO: respuesta tardía del SRI
    EXPIRADA --> NO_AUTORIZADO: respuesta tardía del SRI
    EXPIRADA --> RECHAZADO_RECEPCION: respuesta tardía del SRI
```

### 4.1 Watchdog: sagas que esperan una respuesta que no llega

La saga es asíncrona por Kafka, así que "no pasó nada" es indistinguible de "todavía no contestan". Antes la única recuperación era manual (`POST /sagas/{id}/retry`): si `firma-electronica-service` o el SRI nunca respondían, la saga quedaba en `CREADO`/`FIRMADO` para siempre sin que nadie lo notara. `SagaWatchdog` (en `facturacion-orchestrator-service`) revisa cada 30 s las sagas en estados *reintentables* y aplica `PoliticaReintentoSaga` (lógica pura, probada sin base de datos):

| Estado | Espera a | Plazo (configurable) | Al vencer el plazo |
|---|---|---|---|
| `CREADO` | firma-electronica-service | `saga.watchdog.plazo-firma` = 2 min | reintenta `FirmarComprobante` |
| `FIRMADO` | el SRI | `saga.watchdog.plazo-sri` = 15 min (el SRI puede tardar más) | reintenta `EnviarComprobante` |
| cualquiera de los dos, con `reintentos >= saga.watchdog.max-reintentos` (3) | — | un plazo más tras el último reintento | pasa a **`EXPIRADA`** (el watchdog ya no la toca; `failure_reason` explica por qué; ver respuestas tardías abajo) |

El reintento vuelve a publicar el **payload exacto** del último comando (mismo `eventId`), así que la idempotencia del servicio destino (inbox) decide si era un reintento genuino o si ya lo había procesado. El contador `reintentos` se incrementa con compare-and-set por estado, y cada acción deja una fila `NOTA` en `saga_step`. Las sagas `EXPIRADA` se listan con `GET /sagas?state=EXPIRADA`; `saga.watchdog.habilitado=false` lo apaga.

**`EXPIRADA` acepta respuestas tardías.** Expirar no significa que el servicio destino no vaya a contestar nunca: puede volver horas después (firma o SRI caídos y recuperados) y su respuesta llega a una saga ya `EXPIRADA`. Antes esa respuesta se ignoraba —las transiciones solo aceptaban `CREADO → FIRMADO → AUTORIZADO`— y la saga quedaba `EXPIRADA` aunque el comprobante sí se hubiera firmado o autorizado (defecto que se encontró probando el flujo completo: firma se levantó, firmó, y la saga seguía `EXPIRADA`). Ahora `ComprobanteFirmado`, `FirmaFallida`, `SriAutorizado`, `SriNoAutorizado` y `SriRechazadoRecepcion` también se aceptan desde `EXPIRADA` y la saga sigue su camino normal. `failure_reason` conserva el motivo de la expiración como historial; manda el estado. `POST /sagas/{id}/retry` no cambia el estado: solo vuelve a publicar el último comando, y si el destino ya lo había procesado lo descarta (inbox), así que sobre una saga expirada sirve cuando el comando nunca llegó a procesarse.

**Estados que el watchdog no toca:** los terminales, y `AUTORIZADO` (espera la notificación pero su último comando ya se cumplió).

**Otros puntos de la revisión de la saga**
- **Sagas sin correo (corregido):** `comprobantes-service` solo publica `ComprobanteAutorizado` si hay `compradorCorreo`, así que antes esas sagas se quedaban en `AUTORIZADO` para siempre. Ahora `ComprobanteCreado` lleva `notificacionEsperada` (un `Boolean`; `null` en eventos anteriores = `true`, el comportamiento previo) y, si es `false`, la saga pasa de `AUTORIZADO` a **`AUTORIZADO_SIN_NOTIFICACION`** (terminal) en cuanto el SRI autoriza.
- **Una saga que falla ya no frena a las demás (corregido):** cada saga se procesa en su propia transacción (`idsParaRecuperar` lee; `recuperarSaga` actúa sobre una). Si un reintento falla, se registra una nota `ORQ-003` en otra transacción y **se cuenta como intento**, así una saga con un dato corrupto termina en `EXPIRADA` en vez de fallar eternamente.
- **Relay del outbox con `FOR UPDATE SKIP LOCKED` (corregido, en los 6 servicios con outbox):** con 2 o más instancias de un servicio, cada una toma lotes distintos en vez de publicar los mismos eventos. Los consumidores siguen siendo idempotentes por inbox: el bloqueo evita tráfico duplicado, no es lo que garantiza la corrección.
- **DLQ sin consumidor ni alerta (no corregido; ver el procedimiento en 6.5):** un mensaje que falla se aparta pero nadie recibe aviso. Falta una alerta sobre el tamaño de los `.dlq` y una herramienta de reproceso.
- **La saga no reintenta el envío de notificación** (`NOTIFICACION_FALLIDA` es terminal): decisión documentada en 5, pero hoy no hay herramienta para reenviar el correo.

`SriRecibido` queda registrado en la bitácora de pasos (`saga_step`) pero **no** cambia `SagaInstance.state` — es un checkpoint informativo entre Recepción y Autorización, no un estado de negocio nuevo (misma idea que `StockReleased`/`PaymentRefunded` en `ecommerce/`). La validación de transición se aplica igual que en `ecommerce/`: `UPDATE saga_instance SET state = ? WHERE id = ? AND state IN (...)` atómico — una redelivery duplicada de Kafka simplemente no afecta ninguna fila.

## 5. Por qué esta saga no tiene compensación (a diferencia de `ecommerce/`)

Esta es la diferencia de diseño más importante frente a `ecommerce/`. Allá, un fallo a mitad de camino dispara comandos explícitos que deshacen lo ya hecho (`ReleaseStock`, `RefundPayment`, `CancelOrder`). **Acá no existe ningún comando de compensación**, porque no hay nada que sea seguro o legal deshacer automáticamente:

| Punto de fallo | Qué pasa | Por qué no hay compensación |
|---|---|---|
| Falla la firma (`ComprobanteFirmaFallida`) | `FIRMA_FALLIDA`, estado terminal | Nada se envió a ningún sistema externo todavía — no hay nada que deshacer. |
| El SRI rechaza en Recepción (`SriRechazadoRecepcion`) | `RECHAZADO_RECEPCION`, estado terminal | El comprobante nunca llegó a existir para el SRI — es como si nunca se hubiera enviado. |
| El SRI no autoriza (`SriNoAutorizado`) | `NO_AUTORIZADO`, estado terminal | Mismo caso — el documento no tiene validez legal, no hay "reversa" que hacer ante el SRI. |
| Falla la notificación (`NotificacionFallida`) | `NOTIFICACION_FALLIDA`, estado terminal — **pero el Comprobante en `comprobantes-service` se queda en `AUTORIZADO`, no cambia** | Una vez que el SRI autoriza, el comprobante **es legalmente válido con o sin correo enviado** — no hay ninguna acción de negocio que "cancelar" solo porque el destinatario no recibió el email. La discrepancia (saga en `NOTIFICACION_FALLIDA`, comprobante en `AUTORIZADO`) es intencional y es el único lugar donde queda registrada: reintentar el envío es una operación aparte (reenviar el correo), no una compensación de la saga. |

Esto es coherente con la resolución NAC-DGERCGC25-00000017 (vigente desde 2026-01-01): un final-consumer invoice ya autorizado no puede anularse salvo dentro de la ventana legal de 7 días del mes siguiente, y ni siquiera eso es una "compensación de saga" — es un proceso de negocio aparte (emitir una Nota de Crédito), no algo que este orquestador automatice.

## 6. Flujo detallado de Kafka

### 6.1 Convención de tópicos

`facturacion.<dominio>.<events|commands>.v1`, más un `.dlq` por cada tópico con consumidor real. 29 tópicos en total (14 pares evento/comando + su dlq, más el tópico de auditoría del orquestador, más el par de configuración), creados al arrancar el stack por `facturacion-topic-init` (reutiliza la imagen `../kafka/topic-init`, igual que en `ecommerce/`).

| Tópico | Productor | Consumidor (`group.id`) |
|---|---|---|
| `facturacion.empresa.eventos.v1` (+ `.dlq`) | firma-electronica-service | identidad-service (réplica local `empresa_local`, ver sección 8) |
| `facturacion.empresa.establecimientos.eventos.v1` (+ `.dlq`) | firma-electronica-service | comprobantes-service (réplica local `establecimiento_local`/`punto_emision_local`, valida antes de emitir — ver sección 7.1) **y** identidad-service (misma réplica, valida el alcance de una api-key nueva — ver sección 8) |
| `facturacion.empresa.configuracion.eventos.v1` (+ `.dlq`) | firma-electronica-service | comprobantes-service, notificacion-service, sri-conector-service (cada uno con su propia réplica local `configuracion_empresa_local` — `ride.color`, `notificacion.asunto`/`notificacion.remitente_nombre`, `sri.recepcion.url`/`sri.autorizacion.url`/`sri.autorizacion.poll.max-intentos` respectivamente — ver sección 7.2) |
| `facturacion.inventario.eventos.v1` (+ `.dlq`) | inventario-service | *(reservas de stock, uso interno de inventario)* |
| `facturacion.inventario.comandos.v1` (+ `.dlq`) | *(reservado, no usado por la saga de emisión)* | inventario-service |
| `facturacion.inventario.item.eventos.v1` (+ `.dlq`) | inventario-service | comprobantes-service (réplica local `item_local`, deriva IVA/ICE) |
| `facturacion.comprobante.eventos.v1` (+ `.dlq`) | comprobantes-service | facturacion-orchestrator (filtra `ComprobanteCreado`) **y** notificacion-service (filtra `ComprobanteAutorizado`) — mismo tópico, dos shapes de evento, cada consumidor ignora la que no le corresponde vía el campo `eventType` |
| `facturacion.comprobante.comandos.v1` (+ `.dlq`) | facturacion-orchestrator | comprobantes-service (`MARCAR_FIRMADO`/`MARCAR_AUTORIZADO`/`MARCAR_NO_AUTORIZADO`/etc.) |
| `facturacion.firma.comandos.v1` (+ `.dlq`) | facturacion-orchestrator | firma-electronica-service |
| `facturacion.firma.eventos.v1` (+ `.dlq`) | firma-electronica-service | facturacion-orchestrator |
| `facturacion.sri.comandos.v1` (+ `.dlq`) | facturacion-orchestrator | sri-conector-service |
| `facturacion.sri.eventos.v1` (+ `.dlq`) | sri-conector-service | facturacion-orchestrator |
| `facturacion.notificacion.comandos.v1` (+ `.dlq`) | *(reservado, no usado — notificación no es comandada, ver sección 2)* | notificacion-service |
| `facturacion.notificacion.eventos.v1` (+ `.dlq`) | notificacion-service | facturacion-orchestrator |
| `facturacion.orquestacion.eventos.v1` | facturacion-orchestrator | *(auditoría, sin consumidor obligatorio)* |

`facturacion-orchestrator-service` es el único servicio que produce hacia **3 tópicos de comando distintos** y consume de **4 tópicos distintos** — su relay del outbox enruta cada mensaje pendiente según la columna `outbox_event.topic` de esa fila, y su lado `inventario-service` sigue el mismo patrón que `ecommerce/` para las dos rutas que expone (`ItemCreado` con su propio tópico separado de `eventos.v1` — ver la nota de "tax auto-derivation" en el historial del proyecto: fue la primera vez que un `OutboxRelay` de este stack necesitó enrutar hacia dos tópicos de salida distintos).

### 6.2 Orden garantizado: `comprobanteId` como key

Igual que `ecommerce/` usa `orderId`, acá **todo mensaje relacionado con un comprobante usa `comprobanteId` como key** (ver `OutboxRelay.java` de cada servicio, `OutgoingKafkaRecordMetadata.withKey(event.aggregateId.toString())`). Kafka enruta siempre la misma key a la misma partición, así que todos los eventos y comandos de un comprobante en particular se procesan siempre en el orden en que se publicaron, sin importar cuántas particiones tenga el tópico.

### 6.3 Outbox transaccional y 6.4 Inbox idempotente

Idénticos en mecánica a `ecommerce/` (ver `../ecommerce/ARQUITECTURA.md` secciones 6.3/6.4): escritura de negocio + fila de outbox en la misma transacción SQL, `OutboxRelay` `@Scheduled(every = "1s")` como único publicador real, e inbox con `processed_message` para descartar redeliveries sin reaplicar el efecto. La cadencia de 1s del relay es, además, una decisión regulatoria explícita: satisface el requisito de "transmisión inmediata" de la Resolución NAC-DGERCGC25-00000017 (vigente 2026-01-01) — no hay ninguna ventana de gracia de días hábiles como existía antes de esa resolución.

### 6.5 Dead Letter Queue

Cada tópico con consumidor real tiene `failure-strategy=dead-letter-queue`: un mensaje que falla su procesamiento de forma repetida se mueve a `<tópico>.dlq` en vez de bloquear la partición — mismo mecanismo de contención que `ecommerce/`.

**Procedimiento para revisar y reprocesar un DLQ** (hoy es manual; no hay alerta automática):
```sh
# ¿cuántos mensajes tiene cada DLQ? (offset final = total acumulado)
docker exec kafka-broker-1 /opt/kafka/bin/kafka-get-offsets.sh --bootstrap-server localhost:9092 --topic <topico>.dlq
# leer los mensajes (el motivo del fallo viaja en los headers dead-letter-reason / dead-letter-cause)
docker exec kafka-broker-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \n  --topic <topico>.dlq --from-beginning --property print.headers=true --timeout-ms 5000
```
Para reprocesar: corregir la causa (un consumidor roto, una migración pendiente, un catálogo sin cargar) y volver a publicar el mensaje en el tópico original (`kafka-console-producer.sh --topic <topico> --property parse.key=true`). Los consumidores son idempotentes por inbox, así que reenviar un mensaje que sí se había procesado es inocuo.

### 6.6 Configuración de confiabilidad (resumen)

| Lado | Configuración | Por qué |
|---|---|---|
| Productor | `enable.idempotence=true`, `acks=all` | Evita duplicados por reintento de red; el productor no considera enviado el mensaje hasta que el broker lo confirmó de forma durable. |
| Consumidor | `enable.auto.commit=false` | El offset solo se confirma después de que el efecto de negocio ya quedó aplicado y persistido. |
| Consumidor | `auto.offset.reset=earliest` | Un consumer group nuevo procesa desde el principio del tópico en vez de perderse mensajes ya publicados — con la contrapartida de que un servicio nuevo arrancado tarde replaya todo el historial (ver nota operativa sobre `facturacion-orchestrator-service` en el historial del proyecto). |
| Consumidor | `failure-strategy=dead-letter-queue` | Ver 6.5. |
| Todos los mensajes | `key = comprobanteId` | Ver 6.2. |

## 7. Base de datos

Un solo contenedor Postgres (`facturacion-db`), con **una sola base de datos** (`facturacion`) y **un esquema por servicio**. El contenedor ya no vive en este proyecto: se movió a `D:\inf_sri\CONTENEDORES\postgres` (su propio `docker-compose.yml`, red externa `facturacion_net`, ver su README) y este `docker-compose.yml` se conecta a él — hay que levantarlo antes que los microservicios. Ese stack crea los esquemas y roles; las tablas las crean las migraciones Flyway de cada servicio (o se pueden crear sin levantar los servicios con `docker compose --profile migrar run --rm migraciones`, desde ese mismo directorio):

```mermaid
graph TB
    subgraph PG["Postgres — base de datos 'facturacion'"]
        subgraph S1["esquema firma_electronica"]
            T1["empresa<br/>establecimiento<br/>punto_emision<br/>certificado_digital<br/>configuracion_empresa<br/>outbox_event<br/>processed_message"]
        end
        subgraph S2["esquema inventario"]
            T2["item<br/>categoria<br/>imagen_item<br/>movimiento_kardex<br/>reserva_stock<br/>outbox_event<br/>processed_message"]
        end
        subgraph S3["esquema comprobantes"]
            T3["comprobante<br/>comprobante_pago<br/>forma_pago<br/>impuesto<br/>tarifa_impuesto<br/>tipo_identificacion<br/>impuesto_retencion<br/>retencion<br/>sustento_tributario<br/>detalle_comprobante<br/>detalle_impuesto<br/>destinatario_guia_remision<br/>item_local<br/>establecimiento_local<br/>punto_emision_local<br/>configuracion_empresa_local<br/>secuencial_comprobante<br/>outbox_event<br/>processed_message"]
        end
        subgraph S4["esquema sri"]
            T4["envio_sri<br/>configuracion_empresa_local<br/>outbox_event<br/>processed_message"]
        end
        subgraph S5["esquema notificacion"]
            T5["notificacion_enviada<br/>configuracion_empresa_local<br/>outbox_event<br/>processed_message"]
        end
        subgraph S6["esquema orquestacion"]
            T6["saga_instance<br/>saga_step<br/>outbox_event<br/>processed_message"]
        end
        subgraph S7["esquema identidad"]
            T7["usuario<br/>usuario_empresa_rol<br/>api_key<br/>empresa_local<br/>establecimiento_local<br/>punto_emision_local"]
        end
    end
    R1["rol firma_service"] --> S1
    R2["rol inventario_service"] --> S2
    R3["rol comprobantes_service"] --> S3
    R4["rol sri_service"] --> S4
    R5["rol notificacion_service"] --> S5
    R6["rol orquestacion_service"] --> S6
    R7["rol identidad_service"] --> S7
```

**El aislamiento viene de los permisos de Postgres, no de bases físicas separadas** — mismo patrón que `ecommerce/` (ver [[feedback-postgres-schema-per-service]]): cada servicio se conecta con su propio rol, sin permisos fuera de su propio esquema, con `search_path` fijado del lado del servidor. El SQL de cada servicio vive versionado en `src/main/resources/db/migration/V*.sql` (Flyway, `migrate-at-start=true`).

**Particularidad de este proyecto:** los nombres de tabla y columna están en español y calcados de la terminología exacta del SRI (`clave_acceso`, `comprador_razon_social`, `docSustentoNumero`, etc.) — no traducidos libremente — para que el mapeo entre el modelo de datos y la ficha técnica del SRI sea directo al depurar un rechazo.

### 7.1 Establecimientos y puntos de emisión

`firma-electronica-service` expone un CRUD real (`EstablecimientoResource`/`PuntoEmisionResource`, ambos basados en id, sin re-chequeo de ruc — igual que `PUT actualizar`/certificado) para las tablas `establecimiento`/`punto_emision` de la sección 7 — antes de esto, `establecimiento`/`puntoEmision` eran strings sueltos en cada request de creación de comprobante, sin ningún registro real detrás. `EmpresaService.crearEmpresa` crea automáticamente, en la misma transacción, un establecimiento MATRIZ y un punto de emisión con los códigos por defecto de la empresa — ninguna empresa nueva necesita un paso manual aparte.

`comprobantes-service` mantiene su propia réplica (`establecimiento_local`/`punto_emision_local`, indexadas por `(ruc, codigo)` — nunca por el id interno de firma-electronica-service, mismo principio "Empresa sin ID") vía el tópico `facturacion.empresa.establecimientos.eventos.v1` (ver 6.1), y **valida contra ella antes de reservar el secuencial** de cualquiera de los 6 tipos de comprobante (`ComprobanteService.emitir`) — un establecimiento o punto de emisión no registrado (o inactivo) devuelve 400, en vez de dejar pasar cualquier string como ocurría antes.

**Nota operativa:** la empresa semilla (RUC 1722039953001, creada antes de que este CRUD existiera) ya tiene su establecimiento `001`/punto de emisión `001` reales registrados a mano — cualquier otra empresa creada antes de esta funcionalidad necesitaría el mismo paso manual una sola vez.

### 7.2 `configuracion_empresa`: de infraestructura sin uso a ajustes reales

La tabla `configuracion_empresa` (clave/valor genérico, sección 7) existía desde antes con CRUD completo pero **sin ningún consumidor real** — nada leía sus valores para cambiar comportamiento. `ConfiguracionEmpresaService.crear`/`actualizar`/`eliminar` ahora publican un evento en el outbox (tópico de la tabla de 6.1: genérico, cualquier `clave`/`valor`, un `valor` en `null` en el evento de eliminación revierte al comportamiento por defecto). `crear` (`POST /{id}/configuracion/{clave}`) y `actualizar` (`PATCH /{id}/configuracion/{clave}`) están separados a propósito — antes era un único `establecer` que hacía upsert, pero eso rompía el contrato REST del resto de la API (todo lo demás usa `POST` para "esto no debe existir todavía" y `PUT`/`PATCH` para "esto ya existe"): `crear` responde `409` si la clave ya existe (usa `CodigoDuplicadoException`, la misma que ya usaban `Establecimiento`/`PuntoEmision` para su propio código duplicado), `actualizar` responde `404` si todavía no existe. Ambos comparten el mismo `upsert` de bajo nivel del repositorio tras la validación. Tres servicios mantienen su propia réplica `configuracion_empresa_local` (mismo principio "Empresa sin ID", indexada por `(ruc, clave)`), cada uno con sus propias claves reales:

- **`comprobantes-service`** — `ride.color` (hex RGB, ej. `#1A73E8`): `ActualizacionComprobanteService.aplicarComando` la consulta justo antes de generar el RIDE en `MARCAR_AUTORIZADO` y `RideGenerador` la dibuja como una barra de acento bajo el encabezado si es un hex válido; si no está configurada (o es inválida), el RIDE se ve exactamente igual que antes.
- **`notificacion-service`** — `notificacion.asunto` (asunto del correo) y `notificacion.remitente_nombre` (nombre visible del remitente, ej. "Facturación Acme" — nunca cambia la dirección real autenticada ante el SMTP, `quarkus.mailer.from`): `NotificacionService.procesarComprobanteAutorizado` las consulta antes de llamar a `CorreoService.enviarComprobante`.
- **`sri-conector-service`** — `sri.recepcion.url`, `sri.autorizacion.url` (sobreescriben la URL fija por ambiente) y `sri.autorizacion.poll.max-intentos` (sobreescribe el default global de `application.properties`): `SriConectorService` las consulta en `procesarComando`/`consultarAutorizacion`. Esto necesitó agregar `empresaRuc` a `EnviarComprobanteCommand` (facturacion-orchestrator-service) y a `EnvioSri` (columna nueva, nullable — los envíos de prueba anteriores a este cambio quedan sin ella, sin overrides aplicables, sin afectar su comportamiento ya fijado). El poller (`AutorizacionPoller`) dejó de filtrar por intentos a nivel de consulta SQL (ese máximo ahora varía por empresa) — la decisión de cuándo pasar a `ERROR_CONSULTA` se toma fila por fila en `SriConectorService.aplicarResultadoAutorizacion`, y una vez que una fila se agota deja de calificar por su propio cambio de `estado`, así que el filtro SQL nunca hizo falta como mecanismo de corte real.
- **`comprobantes-service`** — `sri.proveedor.ruc`: RUC del proveedor de sistemas informáticos o servicios de facturación electrónica, requisito del **Anexo 26** de la Ficha Técnica de Comprobantes Electrónicos v2.34 (Resolución SRI No. NAC-DGERCGC26-00000027, 27/07/2026 — plazo de 60 días calendario desde su publicación en Registro Oficial). Deliberadamente white-label: cada empresa (o cada cliente de un revendedor del propio sistema) configura su propio RUC de proveedor, no un valor único fijo de la plataforma. `ComprobanteService.emitir` la consulta junto con la construcción del comprobante (antes de generar el XML, no en el momento del RIDE como `ride.color` — este dato sí tiene que ir en el XML firmado y autorizado ante el SRI) y la guarda inline en `Comprobante.proveedorRuc` (columna `empresa_proveedor_ruc`, mismo criterio "Empresa sin ID" que el resto de datos de empresa en esta tabla). `XmlComprobanteUtil.construirInfoAdicional` la auto-inyecta como `<campoAdicional nombre="RUC Proveedor">...</campoAdicional>` en los 6 tipos de comprobante (mismo mecanismo que ya usaba `granContribuyente`, Anexo 24); `RideGenerador` también imprime la línea "RUC Proveedor" en el RIDE si está configurada. Si la empresa no la configuró, `proveedorRuc` queda `null` y no se agrega nada — retrocompatible.

El resto de claves posibles (feature flags, etc.) siguen sin un consumidor real — el patrón queda listo para el próximo ajuste que lo necesite, mismo criterio que los de arriba.

### 7.3 Validación XSD real contra el esquema oficial del SRI

Hasta ahora, la única garantía de que el XML generado fuera aceptable era el propio código de cada `GeneradorXml*` — cualquier desajuste con el esquema real del SRI solo se descubría con un rechazo real de Recepción (ver varios ejemplos ya documentados en este archivo). `XsdValidador` (`comprobantes-service/infrastructure/xml`) cierra ese hueco: valida el XML sin firmar de cada comprobante contra el XSD oficial de su tipo **antes** de persistirlo, usando `javax.xml.validation` (JDK puro, sin dependencia nueva). Firmar no cambia la estructura del documento (`ds:Signature` es `minOccurs="0"` en las 6 XSD), así que validar el XML sin firmar es equivalente a validar lo que finalmente ve el SRI.

Los 6 XSD de referencia (copiados de `documentacion/XML y XSD */` del repo, que trae varias versiones reales por tipo) están empaquetados en `comprobantes-service/src/main/resources/xsd/`, junto con `xmldsig-core-schema.xsd` (el estándar W3C que todos importan para `ds:Signature`, sin el cual `SchemaFactory` no compila). Se usó siempre **la última versión disponible en `documentacion/`, con una única excepción deliberada**:

| Tipo | Versión usada | Última disponible en `documentacion/` |
|---|---|---|
| Factura | 2.1.0 | 2.1.0 |
| Nota de Crédito | 1.1.0 | 1.1.0 |
| Nota de Débito | 1.0.0 | 1.0.0 (única) |
| Guía de Remisión | 1.1.0 | 1.1.0 |
| Comprobante de Retención | 2.0.0 | 2.0.0 |
| Liquidación de Compra | 1.1.0 | 1.1.0 |

**Retención migró de 1.0.0 a 2.0.0 (estilo ATS)**: la 2.0.0 no es un cambio aditivo — reestructura todo el documento bajo un wrapper `docsSustento`/`docSustento`, donde cada documento sustento agrupa su propio `impuestosDocSustento` (el IVA/ICE que ya traía ese documento, no el valor retenido), sus `retenciones` y sus `pagos`, reemplazando el `<impuestos>` plano de la 1.0.0; `infoCompRetencion` suma `parteRel` (`SI|NO`, obligatorio). Esto obligó a un rediseño real del modelo de dominio de este comprobante, no solo del generador XML:
- `CrearRetencionRequest` cambió su forma (`documentosSustento: DocumentoSustentoRequest[]`, cada uno con `codSustento`/`pagoLocExt`/`totalSinImpuestos`/`importeTotal`/`impuestos`/`retenciones`/`pagos` propios, más `parteRelacionada` a nivel de comprobante) — reemplaza la lista plana `retenciones` de la 1.0.0. Los otros 5 tipos no se tocaron.
- Domain: dos VO nuevos, `ImpuestoDocSustento` (el impuesto del documento sustento) y `DocumentoSustento` (agrupa `impuestos`/`retenciones`/`pagos` de un mismo documento) — y dos campos nuevos en `Comprobante` (`parteRelacionada`, `documentosSustento`), ambos `null`/vacíos en los otros 5 tipos. `documentosSustento` es **transitorio**: solo lo usa `GeneradorXmlRetencion` para construir el XML dentro de `ComprobanteService.construirRetencion`, no se persiste (no hay migración nueva) — queda íntegro dentro de `xml_generado`, que sí se persiste. `Comprobante.detalles` se sigue llenando igual que en la 1.0.0 (una fila por línea de retención) para que el RIDE y el listado de comprobantes no necesiten ningún cambio.
- `XsdValidador` valida contra `ComprobanteRetencion_V2.0.0.xsd`; el `.xsd` de la 1.0.0 se retiró de `src/main/resources/xsd/` (ya no lo referencia nada).

Detalle de implementación: los `Schema` se compilan una sola vez por tipo al construir el bean `@ApplicationScoped` (compilar un XSD no es gratis); `Validator` no es thread-safe, así que se crea uno nuevo por llamada. `ComprobanteService.emitir` corre `xsdValidador.validar(...)` en el worker pool de Mutiny (`Infrastructure.getDefaultWorkerPool()`, mismo patrón que `CertificadoService.validarYExtraerMetadata`) y vuelve al contexto de Vert.x capturado al inicio del método antes de tocar Hibernate Reactive de nuevo — necesario porque el parsing/validación DOM es trabajo bloqueante. Un XML que no cumpla el XSD de su tipo responde 400 con el motivo SAX exacto (línea/columna), antes de persistir el comprobante — aunque el secuencial ya fue reservado en ese punto de la cadena (`ComprobanteService.emitir`), como con cualquier otro fallo de este método.

### 7.4 Formas de pago (Tabla 24) y reglas de `<pagos>`

**Qué dice el SRI.** Las formas de pago son la **Tabla 24** de la Ficha Técnica (catálogo del Anexo Transaccional Simplificado). Está idéntica en todas las versiones de la ficha que trae `documentacion/` (2.26 `.txt`, 2.33 `.txt` y 2.34 PDF — la 2.27 a la 2.34 no cambiaron formas de pago):

| Código | Descripción | Vigente desde |
|---|---|---|
| `01` | SIN UTILIZACION DEL SISTEMA FINANCIERO | 01/01/2013 |
| `15` | COMPENSACION DE DEUDAS | 01/01/2013 |
| `16` | TARJETA DE DEBITO | 01/06/2016 |
| `17` | DINERO ELECTRONICO | 01/06/2016 |
| `18` | TARJETA PREPAGO | 01/06/2016 |
| `19` | TARJETA DE CREDITO | 01/06/2016 |
| `20` | OTROS CON UTILIZACION DEL SISTEMA FINANCIERO | 01/06/2016 |
| `21` | ENDOSO DE TITULOS | 01/06/2016 |

**Qué comprobantes llevan `<pagos>`.** Factura, Nota de Débito y Liquidación de Compra (a nivel de `infoXxx`), y Comprobante de Retención v2.0.0 (dentro de cada `docSustento`). Nota de Crédito y Guía de Remisión no. Cada `<pago>` es `formaPago` + `total` + `plazo`/`unidadTiempo` opcionales — salvo en Retención v2.0.0, cuyo XSD define `pago` solo con `formaPago` y `total`.

**Por qué no basta el XSD.** El patrón de `formaPago` en los XSD de los 4 tipos que llevan `<pagos>` acepta cualquier código de `01` a `21`, que es más amplio que la Tabla 24 (los `02`–`14` no existen). Además el XSD no relaciona los pagos con el `importeTotal`, ni obliga a mandar `plazo` y `unidadTiempo` juntos. Esas reglas las aplica este servicio, antes de generar el XML, y devuelven **400** con el motivo (`PagoRequest` valida el formato; `PagosValidador` las reglas de negocio):

| Regla | Fuente |
|---|---|
| `formaPago` debe existir y estar vigente a la fecha de emisión en el catálogo de formas de pago (Tabla 24, tabla `forma_pago`, ver 7.6) | Ficha técnica, Tabla 24 |
| `plazo` y `unidadTiempo` van juntos o ninguno; `plazo` numérico `>= 0`, hasta 2 decimales; `unidadTiempo` de 1 a 10 caracteres | Ficha técnica ("obligatorio cuando corresponda") + XSD (`plazo` decimal 14,2) |
| La suma de los `total` (cada uno a 2 decimales, como sale en el XML) debe ser igual al `importeTotal` impreso | **Regla de este sistema** — la ficha no la dice expresamente |
| Retención v2.0.0: el pago **no admite** `plazo`/`unidadTiempo` | XSD `ComprobanteRetencion_V2.0.0` (antes llegaba al XSD y fallaba con un error SAX poco claro) |
| Factura con forma de pago `21` (endoso de títulos): exige `plazo`, `unidadTiempo` y `compradorDireccion` | Anexo 11 (factura comercial negociable) |

El vocabulario de `unidadTiempo` no está definido por la ficha (todos sus ejemplos usan `dias`), por eso solo se valida longitud y que vaya acompañado de `plazo`. Si el llamador no manda `pagos`, se asume un único pago `01` por el importe total.

El Anexo 11 también menciona `<tipoNegociable><correo>` para la notificación masiva de facturas negociables; eso **no** está implementado (aplica solo a quien notifica masivamente por el portal del SRI).

### 7.5 `comprobante_pago` y `detalle_impuesto`: de JSON a tablas

Antes, los pagos de un comprobante vivían en `comprobante.pagos_json` y los impuestos de cada línea en `detalle_comprobante.impuestos_json` (texto con un arreglo JSON). Eso impedía consultar, sumar o proteger esos datos con constraints. La migración `V11__pagos_e_impuestos_formalizados.sql` los pasó a tablas con columnas tipadas, **migró los datos existentes** desde el JSON y eliminó las dos columnas:

```
forma_pago (codigo PK, descripcion, vigente_desde, vigente_hasta)        ← catálogo, Tabla 24
   ▲ FK
comprobante_pago (id, comprobante_id FK→comprobante, orden, forma_pago,
                  total numeric(14,2) >= 0, plazo numeric(14,2) >= 0, unidad_tiempo varchar(10))

detalle_impuesto (id, detalle_id FK→detalle_comprobante, orden, codigo, codigo_porcentaje,
                  tarifa numeric(7,2), base_imponible numeric(22,8), valor numeric(14,2),
                  codigo_retencion, doc_sustento_codigo, doc_sustento_numero, doc_sustento_fecha date)
```

Decisiones:
- **`orden`** preserva la posición dentro del comprobante/línea, porque el orden de `<pago>` e `<impuesto>` en el XML sale de ahí. Su `unique (padre_id, orden)` también sirve de índice por padre.
- **`base_imponible` es `numeric(22,8)`, no `(14,2)`**, a propósito: es `cantidad × precioUnitario − descuento` (hasta 8 decimales) y los totales de encabezado se suman sobre ese valor sin redondear antes de formatear, igual que cuando estaba en JSON. Redondearlo al guardar habría cambiado totales al recargar un comprobante.
- **Constraints `NOT VALID`** (`fk_comprobante_pago_forma_pago`, `chk_comprobante_pago_plazo_unidad`): se exigen a toda fila nueva o modificada, pero no a las históricas — el XSD sí dejaba pasar códigos `02`–`14` y `plazo` sin `unidadTiempo`, y esas filas no deben impedir que la migración corra. Cuando se depuren: `ALTER TABLE comprobantes.comprobante_pago VALIDATE CONSTRAINT <nombre>`.
- **En Retención**, cada línea de retención sigue siendo una fila de `detalle_comprobante` con su `detalle_impuesto`: `codigo_porcentaje` queda en `null` y se llenan `codigo_retencion` y los `doc_sustento_*` (`doc_sustento_fecha` es `date` en la base; el dominio y el XML siguen usando `dd/MM/aaaa`). Los `pagos` por documento sustento siguen siendo transitorios (solo viven en `xml_generado`, ver 7.3) — `comprobante_pago` solo guarda los de Factura/Nota de Débito/Liquidación de Compra.
- `campos_adicionales_json` **sigue siendo JSON**: es información libre (`nombre`/`valor`) sin esquema fijo ni consultas previstas.

Ahora sí se puede, por ejemplo, el total vendido por forma de pago:

```sql
select fp.codigo, fp.descripcion, count(*) as pagos, sum(p.total) as total
from comprobantes.comprobante_pago p
join comprobantes.forma_pago fp on fp.codigo = p.forma_pago
group by fp.codigo, fp.descripcion order by total desc;
```

### 7.6 Catálogos del SRI: un módulo de `comprobantes-service`

Los catálogos de la ficha técnica (formas de pago, impuestos y tarifas de IVA, tipos de identificación, retenciones, sustento) son datos **globales** —no pertenecen a una empresa— y cambian cuando el SRI publica una nueva versión de la ficha. Viven en `comprobantes-service` porque es el servicio que **aplica** las reglas del SRI (XSD, generadores de XML, validaciones) y el único que los necesita para emitir: un cambio de catálogo y un cambio de XSD/regla van juntos.

**Historia y decisión.** Primero los catálogos se repetían (la tarifa de IVA se copiaba en cada ítem de inventario y otra vez en `item_local`; las formas de pago eran un `enum`). Luego se separaron en un `catalogos-service` propio que publicaba por Kafka y dejaba réplicas en `comprobantes` e `inventario`. Con el servicio funcionando se vio que **solo `comprobantes-service` consumía 5 de los 7 catálogos**, e inventario los usaba únicamente para validar el código de un ítem y mostrar su tarifa. El costo de un octavo servicio (tópico, siete réplicas, consumidores con DLQ, serializadores, invalidación de caché entre servicios) no se justificaba, y los cuatro defectos que aparecieron al probar con Kafka real estaban todos en esa tubería de réplicas. Se volvió a un módulo dentro de `comprobantes-service`, primero con caché en memoria y después —por decisión del dueño del proyecto— leyendo la base con entidades en cada emisión. **Si más adelante hay un segundo consumidor real** (un servicio de ATS o reportes, una pantalla que liste catálogos), conviene volver a separarlo: la API y las tablas ya tienen la forma de un servicio.

| Alternativa evaluada | Costo por emisión | Acoplamiento | Decisión |
|---|---|---|---|
| Servicio aparte + REST en cada emisión | salto de red + JSON; si cae, no se emite | en tiempo de ejecución | Descartada: rompe "cero REST en la ruta de emisión" y crea un punto único de falla para datos que casi nunca cambian |
| Servicio aparte + réplicas por Kafka | consulta local | eventos entre servicios | Probada y retirada (ver arriba): complejidad sin segundo consumidor |
| Leer el esquema de otro servicio | 1 consulta | **de datos** (comparten tablas) | Descartada: rompe el aislamiento por esquema y rol |
| Entidades JPA por catálogo, leídas en cada emisión | 7 consultas (~8–11 ms más por emisión, medido) | ninguno | **Elegida** (decisión del dueño del proyecto: el valor de la base manda, sin desfase entre instancias) |
| Tablas propias + caché en memoria | 0 consultas (acierto de caché) | ninguno | Probada y retirada: ahorraba ~8–11 ms por emisión a cambio de hasta 30 s de desfase entre instancias y una invalidación que mantener |

**Cómo funciona**
- **Datos:** las 7 tablas (`forma_pago`, `impuesto`, `tarifa_impuesto`, `tipo_identificacion`, `impuesto_retencion`, `retencion`, `sustento_tributario`) son del esquema `comprobantes`. La migración `V14` las **siembra con los datos de la ficha v2.34** y les pone las restricciones (FK, `check`) que antes garantizaba el servicio de catálogos.
- **API** (`/comprobantes/api/v1/catalogos`, todo con api-key): `GET /catalogos` (lista los catálogos), `GET /catalogos/{catalogo}?vigentes=true&fecha=yyyy-MM-dd`, `PUT /catalogos/{catalogo}` (crea o actualiza por clave). **Escribir exige además el header `X-Catalogos-Admin-Token`** (variable `CATALOGOS_ADMIN_TOKEN`): los catálogos son globales y una api-key de una empresa no debe poder cambiarlos para todas las demás. Sin token configurado la escritura está deshabilitada (403). Dar de baja = `"activo": false` (no se borra: el histórico lo necesita). Para cambiar un catálogo **no hace falta redesplegar**.
- **Sin caché:** `CatalogoSriRepositoryImpl` lee las siete tablas con entidades de solo lectura (`FormaPagoEntity`, `ImpuestoEntity`, `TarifaImpuestoEntity`, …) en cada emisión, dentro de la transacción del comprobante, y evalúa la vigencia a la fecha de emisión (hora de Ecuador, UTC-5) en memoria. Un `PUT /catalogos/...` aplica a la siguiente factura en cualquier instancia. La administración (`GET`/`PUT`) sigue siendo genérica sobre `CatalogoDef` con SQL directo, porque los 7 catálogos se operan igual.
- **Vigencia:** un registro aplica en la fecha F si está activo y `vigente_desde <= F <= vigente_hasta` (sin `hasta` = sin fin). `vigente_desde = 1900-01-01` significa "la ficha no publica fecha de inicio".
- **Catálogo vacío:** las validaciones que dependen de un catálogo sin filas **no bloquean**, solo se validan por formato (ver `CatalogosSriAFecha`). La excepción es la tarifa de IVA: sin tarifas en la tabla y sin tarifa en el request, la emisión falla con `COMP-400-204`.
- **Inventario** solo guarda `codigoImpuesto` + `codigoPorcentaje` de cada ítem y **ya no los valida ni muestra la tarifa**: comprobantes los valida y resuelve la tarifa al emitir. Un código inexistente se detecta al facturar, no al crear el ítem.

| Catálogo | Tabla de la ficha | Qué se valida con él |
|---|---|---|
| `forma_pago` | 24 | `formaPago` de cada pago existe y está vigente |
| `impuesto` | 16 | `codigo` del impuesto de cada línea |
| `tarifa_impuesto` | 17 (IVA) | `codigoPorcentaje` existe y vigente; la **tarifa se toma de aquí**; si el cliente manda tarifa debe coincidir |
| `tipo_identificacion` | 6 | tipo de identificación; `07` (consumidor final) solo en Factura |
| `impuesto_retencion` | 19 | impuesto a retener (1 Renta, 2 IVA, 6 ISD) |
| `retencion` | 20 | `codigoRetencion` existe y vigente para ese impuesto y `porcentajeRetener` coincide (IVA e ISD; el ISD 4580 tiene una fila por período) |
| `sustento_tributario` | ATS | `codSustento` |

**Medición que justificó la caché (hoy retirada)** (una máquina, emisión completa de factura —validación, catálogos, secuencial, XML+XSD, persistencia—; 60 muestras por pasada, descartando el calentamiento; se comparó contra leer las 7 tablas en cada emisión):

| | mediana | media | p90 |
|---|---|---|---|
| Caché activa | 35.5 / 32.2 ms | 39.5 / 34.2 ms | 52.6 / 49.5 ms |
| Caché desactivada (7 consultas) | 46.1 / 40.1 ms | 51.3 / 46.9 ms | 75.3 / 69.2 ms |

Ahorro de ~8–11 ms por emisión (~20–25 %). La pasada "desactivada" arrancó con una JVM recién iniciada, así que parte de la diferencia puede ser calentamiento: es un orden de magnitud, no una cifra exacta. El generador de XML y el XSD siguen dominando el tiempo.

**Defecto que dejó esta historia y sigue vigente como lección:** un parámetro declarado `$n::numeric` hace que el cliente de Postgres exija un tipo numérico y rechace el `String` con el que viajan los decimales y las fechas del body JSON. Ahora se declara `::text` y se convierte (`$n::text::numeric`, `$n::text::date`), tanto en el `PUT` como en el filtro `vigentes` del `GET`; `CatalogoAdminServiceSqlTest` fija la forma del SQL.

**Qué NO está cargado, y por qué** (la documentación del proyecto no lo trae y no se inventó):
- **ICE (Tabla 18):** la tabla de la ficha no es utilizable (columnas por fecha ilegibles); el ítem sigue llevando `codigoPorcentajeIce` + `tarifaIce`, y el request de ICE debe traer su `tarifa`.
- **Códigos de retención de Renta** (303, 304, 312, …) y **`codSustento`**: pertenecen al Catálogo del Anexo Transaccional Simplificado (ATS), que no está en `documentacion/`. Las tablas existen (la de sustento, vacía); se cargan con `PUT /catalogos/retencion` y `PUT /catalogos/sustento_tributario` desde el catálogo oficial. Mientras estén vacías solo se valida el formato; desde que tengan filas la validación pasa a ser estricta.
- **Fechas de vigencia de las tarifas de IVA:** la ficha lista los códigos (0, 2, 3, 4, 5, 6, 7, 8, 10) sin fechas; se cargaron sin fecha. Para acotar una tarifa cuando el SRI la cambie: `PUT /catalogos/tarifa_impuesto` con `vigente_hasta`.
- **Países, tipos de comprobante del ATS y demás tablas** de la ficha: nadie los necesita todavía.

Para agregar un catálogo nuevo: tabla + entrada en `Catalogo` y en `CatalogoDef.TODOS` (el REST y el SQL son genéricos), y la lectura en `CatalogoSriRepositoryImpl` si alguna validación lo usa.

### 7.7 Otras validaciones de negocio al emitir

Controles que el XSD no hace y que se aplican antes de generar el XML (todos devuelven 400 con el motivo):

| Regla | Dónde |
|---|---|
| Un ítem dado de baja (`activo=false`) no se puede facturar cuando el impuesto se deriva del ítem | `comprobantes` |
| `tieneIce=true` exige `codigoPorcentajeIce` y `tarifaIce >= 0` (antes fallaba al facturar con un error interno) | `inventario` |
| `07` (consumidor final) exige la identificación `9999999999999`, y ese número solo es válido con `07` (Tabla 6) | `comprobantes` |
| RUC (`04`) = 13 dígitos; cédula (`05`) = 10 dígitos (máscaras de la Tabla 5). No se valida el dígito verificador: el algoritmo no está en la ficha | `comprobantes` |
| `valorRetenido` = `baseImponible × porcentajeRetener / 100` (tolerancia de 1 centavo) | `comprobantes` |
| `docSustentoFecha` debe ser una fecha real (no 31/02) | `comprobantes` |

**Monto máximo a consumidor final.** Una factura con identificación `07` por encima del monto máximo se rechaza con `COMP-400-306` (ficha técnica 8.10: más de 50 USD exige los datos del adquirente). El monto es configurable (`comprobantes.consumidor-final.monto-maximo`, variable `CONSUMIDOR_FINAL_MONTO_MAXIMO`, 50.00 por defecto) porque el historial de la ficha dice que se ha actualizado y no da el valor vigente. Lo que se pudo confirmar (fuentes de prensa y un estudio jurídico, no el texto del Registro Oficial): desde el 10/11/2022 (Decreto Ejecutivo 586, que reformó el artículo 19 numeral 1 del Reglamento de Comprobantes de Venta) el tope general bajó de 200 a 50 USD. Una sola fuente secundaria dice que para RIMPE Negocio Popular el tope es 200 USD; no está confirmado y esos contribuyentes no están obligados a facturar electrónicamente, así que el sistema aplica un único tope. Tampoco queda claro si exactamente 50,00 puede ir a consumidor final (la implementación lo permite: rechaza solo lo que supere el tope). **Confirmar ambos puntos con el texto oficial del Reglamento.**

**Sin implementar**: validación del dígito verificador de RUC/cédula (a propósito, ver sección 11) y que una nota de crédito no exceda el comprobante que modifica (requiere consultar otro comprobante).

### 7.8 Errores de negocio con código y textos en `mensajes.properties`

Las reglas de negocio nuevas (catálogos, pagos, identificación, retención, ICE, saga) **no lanzan `IllegalArgumentException` ni texto suelto**. Cada servicio tiene el mismo esquema:

| Pieza | Qué hace |
|---|---|
| `shared/error/CodigoError` (enum) | Catálogo cerrado de errores. Cada uno lleva `codigo` estable (`COMP-400-101`...), `estadoHttp` y `claveMensaje`. |
| `src/main/resources/mensajes.properties` | El **texto** de cada error, con `{0}`, `{1}`... para los parámetros (UTF-8). Cambiar un texto no cambia el código que recibe el cliente. |
| `Mensajes` (en `facturacion-commons`) | Lee `mensajes.properties` del servicio (y `mensajes-comunes.properties` del jar) y reemplaza los parámetros (reemplazo literal, sin reglas de comillas ni formato de números). |
| `ErrorNegocioException` (en `facturacion-commons`) | Excepción de las reglas: lleva la entrada del catálogo (`ErrorCatalogo`), sus parámetros y, si agrupa varios problemas, una lista de `detalles`. |
| `ApiExceptionMapper` (en `facturacion-commons`) | **Único** mapper de toda excepción que extienda `ApiException` (NotFound, SinPermiso, TokenInvalido, Conflicto, ErrorNegocio y las propias de cada servicio): status y `codigo` salen de su entrada del catálogo. El código de negocio solo lanza, nunca arma un `Response`. |

El catálogo completo (códigos, HTTP y mensajes) está en [`CATALOGO_ERRORES.md`](CATALOGO_ERRORES.md).

Respuesta de error: `{"timestamp", "status", "error", "message", "codigo": "COMP-400-101", "details": [...]}` — **ahora toda respuesta de error lleva `codigo`**: los `throw new NotFoundException("texto")` y similares salen con el código genérico `GEN-*`.

Prefijos: `GEN-` comunes (jar), `COMP-` comprobantes, `INV-` inventario, `FIR-` firma, `IDE-` identidad, `ORQ-` orquestador (sin HTTP). En cada servicio hay un `MensajesTest` que **falla** si un `CodigoError` no tiene clave en `mensajes.properties` o si sobra una clave sin código; los demás tests verifican el **código** devuelto, no el texto.

**Códigos fijos como enums, no como strings:** las formas de pago con regla propia (`FormaPagoSri`: `01` por defecto y `21` endoso/factura negociable), los tipos de identificación y sus reglas de forma (`TipoIdentificacionSri`), los impuestos (`ImpuestoSri`), los nombres de catálogo del contrato de Kafka (`Catalogo`) y la zona horaria (`ZonaHoraria.ECUADOR`). Los enums de dominio **no son el catálogo**: la lista de formas de pago, tarifas, etc. y su vigencia viven en las tablas de catálogo de `comprobantes-service` (ver 7.6).

Alcance: **los 7 servicios** usan el mismo esquema. Las excepciones propias de cada servicio (`StockInsuficienteException`, `CertificadoInvalidoException`, `EmailDuplicadoException`...) extienden `ApiException`, tienen su entrada en el enum del servicio (`CodigoError`, `FirmaError`, `IdentidadError`) y ya no tienen mapper propio: los ~30 mappers duplicados se reemplazaron por el `ApiExceptionMapper` del jar. `IllegalArgumentException` → 400 `GEN-400-002`, Bean Validation → 400 `GEN-400-001`, el resto → 500 `GEN-500-001`. Un 401 del filtro de api-key ahora también sale por el mapper (JSON con `codigo`), ya no como texto plano.

### 7.9 Organización del código de `comprobantes-service` (una responsabilidad por clase)

`ComprobanteService` llegó a 940 líneas con cinco responsabilidades mezcladas. Se repartió sin cambiar el comportamiento (los 6 tipos se siguen emitiendo y autorizando igual; se verificó con la prueba de integración):

| Clase | Responsabilidad |
|---|---|
| `ComprobanteService` | Solo el flujo común de emisión (`emitir`): validar establecimiento y punto de emisión, reservar el secuencial, generar y validar el XML y dejarlo en el outbox. |
| `construccion/Constructor*` (6) | Armar el agregado de UN tipo (factura, nota de crédito, nota de débito, guía de remisión, retención, liquidación de compra), todos detrás de `ConstructorComprobante<R>`. **Agregar un tipo = agregar una clase** (abierto a extensión, sin tocar el flujo). |
| `construccion/ConstructorComun` | Encabezado (clave de acceso, snapshot de la empresa), pagos, campos adicionales y fechas dd/MM/aaaa. |
| `construccion/ConstructorDetalles` | Líneas de detalle: ítems en lote, impuestos desde el catálogo, totales. |
| `ActualizacionComprobanteService` | Aplicar al comprobante las transiciones que manda el orquestador (`MARCAR_*`) y generar el RIDE al autorizar. |
| `ComprobanteConsultaService` | Consultas (por id, XML, RIDE, listado), siempre acotadas a la empresa de la api-key. |
| `EventosComprobante` | Contenido y serialización de `ComprobanteCreado` y `ComprobanteAutorizado` para el outbox. |

Dos reglas que salieron de esa revisión: (1) **el ciclo de vida solo avanza** — `EstadoComprobante.puedePasarA` rechaza un comando repetido o fuera de orden (antes un `MARCAR_FIRMADO` tardío podía devolver un comprobante autorizado a "firmado", o regenerar el RIDE y reenviar el correo); (2) **las fechas dd/MM/aaaa son estrictas** — `31/02/2026` ya no se corrige en silencio a `28/02` (`COMP-400-408`).

## 8. Autenticación y autorización multi-tenant (api-key JWT)

`identidad-service` no participa de la saga de emisión, pero sí consume un tópico de Kafka (ver más abajo) — su razón de ser central es emitir y (junto con los otros 6) verificar las credenciales que protegen todo el ecosistema.

**Modelo de datos** (`identidad`): `Usuario` (email + password con login) puede asociarse a cualquier número de `ruc` vía `POST /usuarios/me/empresas`, sin flujo de invitación/aprobación — pero el `ruc` **sí se valida contra una Empresa real**: el servicio mantiene `empresa_local`, una copia local de solo lectura alimentada consumiendo `facturacion.empresa.eventos.v1` (mismo patrón de vista materializada por eventos que `item_local` en `comprobantes-service`, ver sección 6.1), y `UsuarioService.asociarEmpresa` responde `404` si el ruc no corresponde a ninguna Empresa activa. Autoasociarse (`POST /usuarios/me/empresas`) **solo es posible mientras la empresa no tenga ningún miembro** (reclamar una empresa recién creada y sin dueño); si ya tiene, responde `403` y el acceso lo da un `ADMIN` del ruc con `POST /empresas/{ruc}/usuarios`. Antes cualquier usuario podía asociarse a cualquier ruc con rol `ADMIN` y operar la empresa de otro (crear api-key, subir o revocar su certificado). Tampoco se puede cambiar el propio rol volviendo a llamar a ese endpoint (antes un `LECTURA` podía ascenderse a `ADMIN`): si ya estás asociado, se devuelve tu asociación sin tocarla; el cambio de rol es solo de un `ADMIN` (`PATCH`). Cada asociación usuario↔ruc lleva un `Rol`: `ADMIN` (todo, incluida gestión de api-keys y de otros miembros), `OPERADOR` (puede emitir comprobantes), `LECTURA` (solo consulta: los servicios la rechazan con `403` en cualquier creación o modificación — ver "Autorización en los servicios").

**Gobernanza de miembros — la otra mitad, deliberadamente separada de la autoasociación**: que cualquiera pueda autoasociarse resuelve el bootstrap (sección "Bootstrap de una empresa nueva" más abajo) pero deja un hueco real — nadie más podía ver ni revocar esas asociaciones. `EmpresaMiembroResource` (`GET`/`PATCH`/`DELETE /empresas/{ruc}/usuarios/{usuarioId}`) lo cierra: solo un `ADMIN` ya existente de ese `ruc` puede listar sus miembros, cambiarle el rol a otro, o revocarle el acceso — `UsuarioService.exigirAdmin` lo comprueba consultando la propia fila `usuario_empresa_rol` del solicitante antes de tocar la del objetivo (403 `RolInsuficienteException` si no es `ADMIN`). El cambio de rol es `PATCH` (no `PUT`) porque solo toca el campo `rol` de la asociación, no la reemplaza entera. No es un flujo de invitación/aprobación (no hay estado pendiente ni se notifica a nadie): el ADMIN da acceso directamente a un usuario ya registrado (`POST /empresas/{ruc}/usuarios` con su correo y el rol) o gestiona las asociaciones existentes. `UsuarioService.asegurarNoUltimoAdmin` bloquea (`409 ConflictoException`) cualquier cambio de rol o revocación que dejaría al `ruc` sin ningún `ADMIN`, incluso si quien lo pide es el propio afectado — sin esto, un `ADMIN` podría autodegradarse (o ser degradado, aunque hoy solo un `ADMIN` puede llegar a ese endpoint) y dejar la empresa sin nadie que pueda seguir gestionándola. `DELETE /usuarios/me/empresas/{ruc}` (abandonar un ruc uno mismo) reutiliza exactamente el mismo `asegurarNoUltimoAdmin` — mismo motivo, sin importar si quien revoca es un ADMIN gestionando a otro o el propio usuario saliéndose.

**Autorización en los servicios (aislamiento por empresa y roles).** Una api-key es de UN ruc y lleva un rol; ningún servicio puede fiarse solo de que el JWT sea válido:
- **Por empresa.** Todo recurso se resuelve contra el RUC de la api-key. Un recurso de otra empresa responde **404** (igual que uno inexistente, para no revelar que existe); pedir en un listado el RUC de otra empresa responde **403**. Cubre comprobantes (por id, `/xml`, `/ride` y el listado), sagas (consulta y `retry`), envíos al SRI, notificaciones, ítems/categorías/kardex/imágenes de inventario y, en firma-electronica-service, la empresa, su certificado, su configuración, sus establecimientos y puntos de emisión (y `GET /empresas` solo devuelve la de la api-key). Antes la comprobación existía únicamente en los `POST` de creación: con la api-key de cualquier empresa se podía leer comprobantes ajenos o subir y revocar el certificado de firma de otra empresa. Cada servicio tiene su `TokenPrincipal.esDeLaEmpresa(ruc)` y, donde hay varios recursos hijos, una guardia `AccesoEmpresa` (inventario, firma) que resuelve el dueño del recurso. notificacion-service deduce el RUC de la propia clave de acceso (dígitos 11–23). Las categorías de inventario también son por empresa (columna `ruc`, migración `V6__categoria_por_empresa.sql`): un ítem solo puede usar categorías de su propio RUC.
- **Por rol.** `LECTURA` solo consulta (`403` al crear o modificar comprobantes, ítems, kardex, imágenes, categorías o al reintentar una saga); `OPERADOR` además emite y gestiona inventario; `ADMIN` además gestiona la empresa (certificado, configuración, establecimientos y puntos de emisión de firma-electronica-service) y los miembros (identidad-service).
- **Revocación de api-keys.** Cada servicio verifica el JWT localmente, sin consultar a identidad-service, así que **revocar una api-key (`DELETE /api-keys/{id}`) no la invalida hasta que vence** (como máximo 365 días: `identidad.jwt.apikey.max-dias`, que también se aplica si piden `0` o un valor mayor). Ver la sección 11.

**Autogestión de cuenta**: `PATCH /usuarios/me` (nombres/apellidos — el email de login no cambia por ahí, es un flujo más sensible fuera de alcance de esta versión) y `PATCH /usuarios/me/password` (exige la contraseña actual, `401` si no coincide) cierran el hueco de que una cuenta, una vez creada, no tuviera ninguna forma de editarse a sí misma — ambos son `PATCH`, no `PUT`, porque cada uno toca solo una parte del recurso `Usuario` (nunca las 7 columnas completas a la vez).

**`PUT` vs `PATCH` vs `POST` en este ecosistema**: se usa `PUT` cuando el body reemplaza por completo el estado mutable del recurso direccionado por la URL (`ActualizarEmpresaRequest`/`ActualizarEstablecimientoRequest`/`ActualizarPuntoEmisionRequest`/`ActualizarItemRequest`/`ActualizarCategoriaRequest` — cada uno espejea casi todos los campos de su `Crear*Request` correspondiente, más `activo`), y `PATCH` cuando el body toca solo un subconjunto de un recurso más amplio (perfil de usuario, contraseña, rol de un miembro). `configuracion_empresa` es el único recurso que expone `POST` y `PATCH` sobre la misma URL (`/{id}/configuracion/{clave}`) — `POST` crea la clave (`409` si ya existe) y `PATCH` cambia el valor de una que ya existe (`404` si no) — ver 7.2, mismo criterio de "`POST` = no debe existir todavía" que el resto de la API (`POST /categorias`, `POST /items`, etc., todos `409`/lo que aplique en su dominio si hay choque de identidad). Los 6 endpoints de creación de comprobante siguen siendo `POST` (crean un recurso nuevo, nunca se actualizan después de creados — ver sección 5).

**Dos tipos de JWT, un solo secreto compartido (`IDENTIDAD_JWT_SECRET`, HS256)**:

| Tipo | Emitido por | Dónde sirve | Duración |
|---|---|---|---|
| **Sesión** | `POST /usuarios/login` | Solo contra `identidad-service` (`/usuarios/me/*`, `/api-keys/*`) **y, como única excepción, `POST /empresas` de firma-electronica-service** (ver más abajo) | Corta (`identidad.jwt.sesion.duracion-horas`) |
| **Api-key** | `POST /api-keys` (requiere sesión) | Contra los 6 servicios de dominio | Configurable, default 365 días — el valor solo se muestra una vez, al crearla |

**Bootstrap de una empresa nueva — el ciclo que había que romper**: una api-key solo se emite para un ruc al que el usuario ya esté asociado (`POST /usuarios/me/empresas`), y esa asociación exige que el ruc ya corresponda a una Empresa real (`empresa_local`, arriba). Para una empresa genuinamente nueva eso nunca se cumple — no hay forma de conseguir una api-key para darla de alta. `PermiteSesion` (`firma-electronica-service/infrastructure/security`) es la única excepción a "todo endpoint exige una api-key": marca `EmpresaResource.crear`, que además de una api-key acepta un token de sesión — `JwtVerificador.verificarApiKeyOSesion` reconoce ambos tipos (con una sesión, `ruc`/`rol`/`establecimiento`/`puntoEmision` quedan en `null`, así que el chequeo de titularidad se salta: no hay ningún dueño previo que proteger todavía). `EmpresaService.crearEmpresa` incluye el `usuarioCreadorId` en el evento `EmpresaRegistrada`, y `identidad-service.EmpresaEventConsumer` lo usa para otorgar automáticamente el rol `ADMIN` a ese usuario para ese ruc — así, apenas se crea la empresa, el usuario ya puede pedir su primera api-key sin ningún paso manual.

El api-key **es** el JWT — no hay lookup en base de datos del lado de quien lo verifica. Lleva `ruc` y `rol` como claims siempre, y **opcionalmente** `establecimiento`/`puntoEmision` — eso es lo que cada servicio necesita para decidir si el caller puede operar sobre ese ruc (y, si aplica, sobre ese establecimiento/punto de emisión específico).

**Verificación 100% local, cero llamadas de red**: cada uno de los 6 servicios de dominio tiene su propia copia (no una librería compartida — mismo criterio de independencia real que el resto del stack, ver sección 2) de un paquete `infrastructure/security/`:
- `ApiKeyAuthFilter` — un `ContainerRequestFilter` **global** (sin `@NameBinding`): exige `Authorization: Bearer <api-key>` en *todo* endpoint, no existen rutas públicas en ninguno de los 6 servicios.
- `JwtVerificador` — valida la firma HS256 contra `identidad.jwt.secreto` (mismo valor que `IDENTIDAD_JWT_SECRET`) y confirma que el claim `tipo` sea `api_key` (rechaza un token de sesión si alguien lo usa por error).
- `TokenPrincipal` — request-scoped, con `usuarioId`/`ruc`/`rol`/`establecimiento`/`puntoEmision` ya verificados, disponible para inyectar en cualquier recurso. Los dos últimos son `null` en cualquier api-key emitida antes de que existieran — sin restricción, comportamiento sin cambios (retrocompatible a propósito).

**Validación de propiedad del ruc — solo donde el body la trae directamente**: `comprobantes-service` (los 6 endpoints `crearX`, comparando `request.getEmpresa().getRuc()`), `inventario-service` (`POST /items`) y `firma-electronica-service` (`POST /empresas`) comparan el `ruc` del request contra el `ruc` del token y responden `403 Forbidden` (`SinPermisoException`) si no coinciden. Los endpoints basados en `id` (actualizar, subir certificado, kardex, configuración, `/retry` de saga) **no** repiten esa validación — solo exigen una api-key válida de *cualquier* ruc, igual que las superficies puramente de observabilidad (`sri-conector-service`, `notificacion-service`, `facturacion-orchestrator-service`), que son GET-mayoritariamente y donde filtrar por ruc se dejó fuera de alcance a propósito.

**Api-key acotada a establecimiento/punto de emisión (opcional)**: pensado para empresas con varias sucursales/cajas que no quieren que la key de un punto de venta pueda emitir con el de otro — sin tocar en nada el principio "Empresa sin ID" (comprobantes-service sigue sin conocer `usuarioId` ni ningún id interno, solo compara valores del propio request contra los claims del token, igual que ya hacía con `ruc`).
- `POST /api-keys` (identidad-service) acepta `establecimiento`/`puntoEmision` opcionales — `puntoEmision` sin `establecimiento` es 400. `ApiKeyService.crear` valida que sean reales y estén activos contra su propia réplica `establecimiento_local`/`punto_emision_local` (alimentada por el mismo tópico `facturacion.empresa.establecimientos.eventos.v1` que ya consume comprobantes-service — ver 6.1) antes de emitir el JWT.
- `ComprobanteResource.validarAlcance` (antes `validarRuc`) además del ruc, si el token trae `establecimiento`/`puntoEmision`, exige que el request de creación coincida exactamente — `403` si no. Una api-key sin esos claims (el caso normal) no se ve afectada.
- Alcance deliberadamente acotado: solo se aplica en la creación de comprobantes (donde vive el establecimiento/punto de emisión del request) — no en el CRUD de establecimientos/puntos de emisión de firma-electronica-service (esos endpoints siguen siendo solo por `id`, mismo criterio que el resto de mutaciones basadas en `id`).

**Por qué HS256 con secreto compartido y no RSA/OIDC**: ningún consumidor de estos tokens es un navegador — todos son el propio backend de este ecosistema, que ya recibe sus credenciales por variable de entorno igual que el resto de secretos del stack (contraseñas de DB, clave maestra de cifrado del `.p12`). No hay necesidad de distribuir una llave pública por separado ni de correr un proveedor OIDC (ver sección 11, sigue fuera de alcance como *proveedor de identidad externo* — esto es autenticación propia del ecosistema, no reemplaza un futuro Keycloak si el cliente externo lo necesitara).

## 9. Particularidades técnicas de la integración real

Esta sección no tiene equivalente en `ecommerce/` porque allá todo lo externo es simulado. Acá sí importa cómo se resolvió cada integración:

| Integración | Cómo se resolvió | Por qué |
|---|---|---|
| Firma XAdES-BES | Hand-built sobre `javax.xml.crypto.dsig` del JDK (sin librería externa) | No existe una librería Java mantenida que implemente el perfil exacto de firma que exige el SRI; se verificó contra un certificado real (Security Data S.A.) que el X509 resultante y las 3 References (comprobante/KeyInfo/SignedProperties) son correctas. |
| SOAP hacia el SRI | Envoltorios SOAP hand-built + `java.net.http.HttpClient` + parsing DOM (sin CXF/JAX-WS ni generación de código desde el WSDL) | Mismo criterio "solo JDK" que la firma; los contratos reales (`RecepcionComprobantesOffline?wsdl`, `AutorizacionComprobantesOffline?wsdl`) se verificaron a mano contra el WSDL real antes de escribir los clientes. |
| Correo con el RIDE | `quarkus-mailer` estándar (SMTP real, STARTTLS puerto 587) | A diferencia de la firma/SOAP, acá sí hay una extensión estándar que cubre el caso sin necesidad de nada a medida. |
| Clave de acceso | 49 dígitos, mod-11 check digit, calculada en `comprobantes-service` | Algoritmo documentado en la ficha técnica del SRI, es el identificador único del comprobante ante el SRI y ante el propio sistema. |
| RIDE (representación impresa) | Generado en `comprobantes-service` a partir del comprobante autorizado, antes de que exista cualquier necesidad de notificarlo | Se genera una sola vez y viaja embebido (base64) en el evento `ComprobanteAutorizado` — `notificacion-service` no tiene ni necesita lógica de renderizado propia. |

## 10. Observabilidad y operación

| Qué | Dónde |
|---|---|
| Swagger UI de cada servicio | `http://localhost:<puerto>/<root-path>/q/swagger-ui/` |
| OpenAPI JSON | `http://localhost:<puerto>/<root-path>/q/openapi` |
| Health check | `http://localhost:<puerto>/<root-path>/q/health/ready` |
| Estado + línea de tiempo de una saga | `GET http://localhost:8096/sagas/api/v1/{comprobanteId}` |
| Inspeccionar tópicos/mensajes/consumer groups de Kafka | Kafka UI, `http://localhost:8080` |

| Servicio | Puerto host | Root path |
|---|---|---|
| firma-electronica-service | 8091 | `/empresas/api/v1` |
| inventario-service | 8092 | `/inventario/api/v1` |
| comprobantes-service | 8093 | `/comprobantes/api/v1` |
| sri-conector-service | 8094 | `/sri/api/v1` |
| notificacion-service | 8095 | `/notificaciones/api/v1` |
| facturacion-orchestrator-service | 8096 | `/sagas/api/v1` |
| identidad-service | 8097 | `/identidad/api/v1` |
| Postgres (`facturacion-db`) | 5434 | — |
| Kafka broker (host) | 9091 (`kafka/.env` → `KAFKA_HOST_PORT`) | — |
| Kafka UI | 8080 | — |

## 11. Fuera de alcance, a propósito

- **Sin proveedor de identidad externo (OIDC/Keycloak)** — `identidad-service` es autenticación propia del ecosistema (JWT HS256 con secreto compartido, ver sección 8), no un reemplazo de un IdP externo si un cliente lo necesitara más adelante. Sin rate-limiting/lockout en `/usuarios/login` (fuerza bruta fuera de alcance) y sin flujo de **reset** de contraseña por correo (el típico "olvidé mi contraseña", sin conocer la actual) — sí existe **cambio** de contraseña autenticado (`PATCH /usuarios/me/password`, exige la contraseña actual, ver sección 8), que es un caso distinto.
- **Sin manifiestos de Kubernetes** — el objetivo de despliegue es Docker Compose.
- **Sin esquema de contingencia del SRI** — solo se implementó el esquema offline (el que usa el SRI en producción); el esquema de contingencia online queda fuera de alcance.
- **IVA e ICE calculados de forma independiente sobre la misma base imponible** — la ley ecuatoriana real calcula IVA sobre (base + ICE) para ítems con ICE; documentado como simplificación conocida en el código de `comprobantes-service`.
- **Factura 1.1.0 (bloque de reembolso)** — fuera de alcance; si se agrega en el futuro, cada tipo de comprobante nuevo o campo opcional nuevo debe re-verificarse contra una llamada real de Recepción antes de darse por bueno (la ficha técnica por sí sola no fue suficiente para detectar 2 de los mismatches de XSD encontrados durante la construcción — ver historial del proyecto).
- **No hay reintento automático de notificación** — si `NotificacionFallida` queda como estado terminal de la saga, reenviar el correo es una operación manual/aparte, no algo que el orquestador reintente solo (a propósito: reintentar SMTP indefinidamente sin intervención podría enmascarar un problema real de configuración, como una contraseña de aplicación inválida).

### Observaciones de una revisión de calidad y seguridad que quedan abiertas (decisión pendiente)

- **Cualquier usuario registrado puede asociarse como `ADMIN` a cualquier RUC existente** (`UsuarioService.asociarEmpresa`, sin verificar titularidad ni invitación): con eso obtiene api-keys ADMIN de esa empresa, puede subir/revocar su certificado y emitir comprobantes. El aislamiento por tenant es nominal hasta que haya invitación/aprobación o verificación del RUC.
- **Un solo secreto HS256 compartido por todos los servicios**: comprometer uno permite fabricar tokens de cualquier RUC y rol. Alternativa: firma asimétrica (EdDSA/RS256) con la llave privada solo en identidad-service.
- **Arquitectura hexagonal incompleta**: `application` importa clases de `infrastructure` (outbox, mappers JPA, clientes SOAP, `XsdValidador`, `RideGenerador`) en ~19 archivos; faltan puertos (`EventPublisher`, `XmlValidator`, `RideRenderer`, `SriGateway`) y un test ArchUnit que lo haga cumplir. El dominio `Comprobante` es anémico (56 setters, sin comportamiento).
- **Llamada SOAP dentro de la transacción de BD** en `SriConectorService.procesarComando`/`consultarAutorizacion` (hasta 30 s con una conexión retenida).
- *(Corregido en esta revisión)* override de URL del SRI sin validar (SSRF): ahora solo se respeta `https` + host en `sri.url.hosts-permitidos` (por defecto `*.sri.gob.ec`); XXE en los parsers de XML; timeout de 30 s en el relay del outbox; comando de saga fuera de orden ya no se descarta en silencio (falla visible → DLQ); bcrypt fuera del event loop.

- **Revocar una api-key no la invalida** hasta que vence: los 6 servicios verifican el JWT localmente (sin consultar a identidad-service) y no conocen la lista de revocadas. Opciones: (a) vida corta (p. ej. 1 h) con renovación; (b) publicar un evento `ApiKeyRevocada` que cada servicio guarde (tabla de `jti` revocados) y consulte en el filtro; (c) consultar a identidad-service con caché corta. La (b) encaja con el resto de la arquitectura (eventos + réplica local).
- **El login no tiene límite de intentos** (fuerza bruta de contraseñas): conviene un limitador en la puerta de entrada o un bloqueo temporal por cuenta/IP. El tiempo de respuesta ya no delata si un correo existe, pero el registro (`POST /usuarios`) sí responde `409` a un correo repetido.
- **La nota de crédito no valida contra el documento que modifica** (que exista, que sea de la misma empresa, que no se exceda su valor): hoy confía en el número y la fecha que manda el cliente.
- **No se valida el dígito verificador de cédula/RUC** a propósito: el SRI es la autoridad y tiene excepciones; rechazar de más bloquearía emisiones válidas. La longitud y el tipo sí se validan.
- **Los consumidores de Kafka** (17) no validan el sobre del mensaje de forma uniforme; firma-electronica-service y sri-conector-service rechazan un comando sin `eventId`/`comprobanteId` (va al DLQ con un motivo claro), el resto depende de que el mensaje venga bien formado. Los DLQ siguen sin consumidor ni alerta (ver 6.5).

## 12. `facturacion-commons`: el jar de recursos compartidos

Un análisis de duplicados (mismo nombre y mismo contenido, normalizando el paquete) encontró ~40 clases idénticas copiadas en 5–7 servicios: excepciones y mappers, `ApiErrorResponse`, `Mensajes`, `JwtVerificador`/`ApiKeyAuthFilter`/`TokenPrincipal`, `SecurityRequirementOpenApiFilter`, paginación. Cada arreglo de seguridad costaba 7 cambios. Ahora viven una sola vez en `facturacion-commons` (jar, paquete `com.facturacion.commons`):

| Paquete | Contenido |
|---|---|
| `error` | `ErrorCatalogo`/`MensajeCatalogo` (contrato del catálogo), `ErrorComun` (`GEN-*`), `Mensajes`, `ApiErrorResponse`, mappers (`ApiExceptionMapper`, Bean Validation, `IllegalArgument`, catch-all) |
| `exception` | `ApiException` y sus derivadas: `NotFoundException`, `SinPermisoException`, `TokenInvalidoException`, `ConflictoException`, `ErrorNegocioException` |
| `security` | `JwtVerificador`, `ApiKeyAuthFilter`, `TokenPrincipal`, `@PermiteSesion`, `SecurityRequirementOpenApiFilter` (los 6 servicios de dominio; identidad conserva su propia seguridad de sesión) |
| `paging` | `PageRequest`, `PagedResponse`, `PageResult` |
| `reactive` | `TrabajoBloqueante`: corre trabajo bloqueante (p. ej. bcrypt) fuera del event loop y vuelve al contexto de Vert.x |

**Qué NO se comparte, a propósito:** los records de mensajes Kafka, las entidades JPA/outbox/inbox, los modelos de réplica local (`ConfiguracionEmpresaLocal`, `EstablecimientoLocal`...) y cualquier regla de negocio — compartirlos acoplaría los bounded contexts y su despliegue independiente. El jar es solo infraestructura transversal y todas sus dependencias son `provided` (no cambia el classpath de los servicios). Un `META-INF/beans.xml` hace que Quarkus lo indexe y descubra sus `@Provider` y beans CDI.

**Construcción:** el jar debe estar instalado antes de compilar un servicio. `mvn install` en esta carpeta (pom agregador) construye todo en orden; o solo el jar: `mvn -f facturacion-commons/pom.xml install`. Los Dockerfiles construyen el jar dentro de la imagen, por eso el contexto de build de `docker-compose.yml` pasó a ser esta carpeta (`context: .` + `dockerfile: <servicio>/Dockerfile`; hay un `.dockerignore` en la raíz). Versión de Quarkus: 3.40 LTS en los 8 módulos.

**Reglas del catálogo (`CatalogoErroresTest` en el jar; el `MensajesTest` de cada servicio las aplica a su enum):** código único con formato `PREFIJO-HTTP-NNN`, el segmento HTTP coincide con `estadoHttp`, y toda entrada tiene su texto en un `.properties`.
