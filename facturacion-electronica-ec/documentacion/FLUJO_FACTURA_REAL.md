# Flujo real de una factura electrónica, de cero hasta el correo

Todo el recorrido en orden: levantar el entorno, crear cuenta, empresa y api-key, subir el certificado, cargar un producto,
emitir la factura y verificar el correo. Empresa de ejemplo: RUC `1722039953001`, ambiente `PRUEBAS` (usa siempre PRUEBAS
mientras pruebas: el SRI de producción emite documentos con validez legal). Puertos y prefijos de cada servicio:
`GUIA_PRUEBAS_MANUALES.md`, sección 0. **No escribas la contraseña del `.p12` ni las del correo en archivos versionados.**

Hay dos formas de recorrerlo con los mismos pasos: `curl` (secciones 1 a 6) o Postman (sección 8).

## 0. Levantar el entorno desde cero
Rutas relativas a `PROYECTOS/PERSONALES/docker-kafka`; Docker Desktop corriendo.

**1. Kafka** (`kafka/`): broker KRaft más el job que crea los tópicos y Kafka UI.
```bash
cd kafka && cp .env.example .env     # pon un KAFKA_CLUSTER_ID: docker run --rm apache/kafka:3.9.0 /opt/kafka/bin/kafka-storage.sh random-uuid
docker compose up -d --build         # broker en localhost:9094 (host) / broker:9092 (red kafka_net); UI en :8080
```

**2. Postgres** (`D:/inf_sri/CONTENEDORES/postgres`): base `facturacion`, un esquema y un rol por servicio.
```bash
cp .env.example .env                 # contraseñas *_DB_PASSWORD: las mismas que irán en el .env de facturacion-electronica-ec
docker compose up -d --build         # localhost:5434; desde los contenedores: facturacion-db:5432 (red facturacion_net)
```
Las tablas las crea cada servicio al arrancar (Flyway). Para reiniciar la base desde cero: `docker compose down -v` y volver a levantar. Al reiniciarla, los secuenciales de las facturas
vuelven a 1 y el SRI rechaza los ya usados: ver el síntoma `ERROR SECUENCIAL REGISTRADO` en la sección 7.
Con otra instancia de Postgres, apunta a ella con `DB_HOST` y `POSTGRES_PORT` en el `.env` del paso siguiente.

**3. Configuración de los servicios** (`facturacion-electronica-ec/`):
```bash
cd facturacion-electronica-ec && cp .env.example .env
./scripts/generar-claves-jwt.sh >> .env      # par Ed25519: la privada solo la usa identidad; los demás reciben la pública
openssl rand -base64 32                       # → FIRMA_CIFRADO_CLAVE_MAESTRA (cifra el .p12 en la base)
```
Completa en el `.env`: `POSTGRES_*`, las siete `*_DB_PASSWORD`, `FIRMA_CIFRADO_CLAVE_MAESTRA`, `CORS_ORIGINS`, los puertos y el correo.

> **Varios ambientes (desarrollo, pruebas, producción):** en vez de un solo `.env`, usa `env/<ambiente>.env` (versionado, sin secretos) y
> `env/<ambiente>.secretos.env` (ignorado por git), y levanta con `./scripts/compose.sh <ambiente> up -d`. Guía completa: `env/README.md`.

**4. Correo saliente (Gmail).** `notificacion-service` envía por SMTP con estas variables del `.env`:
`SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`, `SMTP_USERNAME=e.du.ingcharles@gmail.com`, `SMTP_FROM=e.du.ingcharles@gmail.com`
y `SMTP_PASSWORD=<contraseña de aplicación>`. Gmail **no acepta la contraseña normal**: la cuenta necesita verificación en
dos pasos y una *contraseña de aplicación* (Cuenta Google → Seguridad → Contraseñas de aplicaciones). El `.env` está en
`.gitignore`; la contraseña no va a ningún otro archivo. El receptor (`e.du.ingcharles@hotmail.com`) no necesita
configuración; revisa también su carpeta de correo no deseado.

**5. Construir y levantar los 7 servicios.** Cada imagen compila su código dentro de Docker (~10 min en total, pesado en
memoria: constrúyelas de a una).
```bash
for s in comprobantes-service firma-electronica-service facturacion-orchestrator-service inventario-service \
         sri-conector-service notificacion-service identidad-service; do docker compose build $s; done
docker compose up -d
docker compose ps                    # los 7 en "healthy" (≈1 min); facturacion-topic-init termina solo (Exited 0)
```
Tras cambiar el código de un servicio: `docker compose build <servicio> && docker compose up -d --no-deps <servicio>`.
Tras cambiar solo el `.env`: `docker compose up -d <servicio>` (recrea el contenedor).

```bash
ID=http://localhost:8097/identidad/api/v1;  FIR=http://localhost:8091/empresas/api/v1
INV=http://localhost:8092/inventario/api/v1; COM=http://localhost:8093/comprobantes/api/v1
SAG=http://localhost:8096/sagas/api/v1;      NOT=http://localhost:8095/notificaciones/api/v1
```

## 1. Cuenta, empresa y api-key
```bash
curl -s -X POST $ID/usuarios -H 'Content-Type: application/json' \
  -d '{"email":"e.du.ingcharles@gmail.com","password":"<clave-de-la-cuenta>","nombres":"Carlos","apellidos":"Anchundia"}'
TOKEN=$(curl -s -X POST $ID/usuarios/login -H 'Content-Type: application/json' \
  -d '{"email":"e.du.ingcharles@gmail.com","password":"<clave-de-la-cuenta>"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
# Empresa (con el token de sesión): el usuario queda ADMIN y se crea el establecimiento 001 / punto 001
curl -s -X POST $FIR -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "ruc":"1722039953001","razonSocial":"ANCHUNDIA VALENCIA CARLOS EDUARDO","nombreComercial":"Prueba",
  "direccionMatriz":"LLANO CHICO","obligadoContabilidad":false,"contribuyenteEspecial":false,
  "esGranContribuyente":false,"ambiente":"PRUEBAS",
  "establecimientoDefecto":"001","puntoEmisionDefecto":"001"}'      # guarda el "id" → EMPRESA_ID
# El rol ADMIN llega por un evento de Kafka: espera a que aparezca la empresa
curl -s -H "Authorization: Bearer $TOKEN" $ID/usuarios/me/empresas
# Api-key (solo se muestra una vez): sirve para los otros 6 servicios
curl -s -X POST $ID/api-keys -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"nombre":"flujo-real","ruc":"1722039953001","expiracionEnDias":30}'   # guarda el "valor" → APIKEY
```

## 2. Certificado de firma y logo
```bash
curl -s -X POST $FIR/$EMPRESA_ID/certificado -H "Authorization: Bearer $APIKEY" \
  -F "archivo=@/ruta/al/certificado.p12" -F "contrasena=<clave-del-p12>"     # 201; 400 si venció o la clave no abre
curl -s -X POST $FIR/$EMPRESA_ID/logo -H "Authorization: Bearer $APIKEY" -F "archivo=@logo.png;type=image/png"   # opcional
```

## 3. Producto con stock (el descuento de inventario está en `true`)
```bash
curl -s -X POST $INV/items -H "Authorization: Bearer $APIKEY" -H 'Content-Type: application/json' -d '{
  "ruc":"1722039953001","tipo":"PRODUCTO","codigoPrincipal":"TABLET02","descripcion":"Tablet 10 pulgadas",
  "precioUnitario":300.00,"unidadMedida":"unidad","cantidadInicial":20,"costoInicial":220.00,"stockMinimo":5,
  "codigoPorcentajeIva":"4"}'              # guarda el "id" → ITEM_ID
```
Con `INVENTARIO_DESCUENTO_HABILITADO=false` (orquestador) la factura no reserva ni descuenta stock.

## 4. Emitir la factura
```bash
curl -s -X POST $COM/facturas -H "Authorization: Bearer $APIKEY" -H 'Content-Type: application/json' -d '{
  "empresa":{"ruc":"1722039953001","razonSocial":"ANCHUNDIA VALENCIA CARLOS EDUARDO","direccionMatriz":"LLANO CHICO",
            "ambiente":"PRUEBAS","establecimiento":"001","puntoEmision":"001"},
  "compradorTipoIdentificacion":"05","compradorIdentificacion":"1710034065","compradorRazonSocial":"Consumidor de Prueba",
  "compradorDireccion":"Quito","compradorCorreo":"e.du.ingcharles@hotmail.com","moneda":"DOLAR",
  "detalles":[{"itemId":"'$ITEM_ID'","codigoPrincipal":"TABLET02","descripcion":"Tablet 10 pulgadas","cantidad":1,"precioUnitario":300.00}]}'
# 201 + "id" → COMPROBANTE_ID. Comprobante BORRADOR; la saga arranca sola.
```
Datos opcionales que la ficha del SRI exige cuando aplican (ver `ARQUITECTURA.md`, sección 13.4):
- En `empresa`: `contribuyenteEspecial` + `numeroContribuyenteEspecial`, `agenteRetencion` (número de resolución, p. ej. `"3257"`),
  `regimenRimpe` (`"RIMPE"` o `"NEGOCIO_POPULAR"`), `esGranContribuyente` + `numeroResolucionGranContribuyente`.
- En cada línea de `detalles`: `"detallesAdicionales":[{"nombre":"Centro Logistico","valor":"23"}]` (hasta 3). Si el producto tiene
  `unidadMedida`, la línea agrega sola `Medida: <unidad>`.
- Campos adicionales del comprobante: `"campoAdicional":[{"nombre":"Lugar Venta","valor":"CORAL 10 DE AGOSTO"}]`.
- `"placa":"PCM4567"` en la factura (nivel raíz, junto a `moneda`): obligatoria en la venta de combustibles líquidos (Anexo 12) y en el transporte comercial (Anexo 25). En combustibles, la línea usa `codigoPrincipal` y `descripcion` de la Tabla 30 (Anexo 16), p. ej. `0103` y `SÚPER`.
- En una línea, `"precioSinSubsidio": 2.50` (opcional, cuando corresponda). Una línea con código de combustible (`0103`, `0101`, `0174`, `0121`, `0104`) sin `placa` en la factura responde `400 COMP-400-504`.
- El RUC del proveedor del sistema de facturación (Anexo 26) sale de la configuración `sri.proveedor.ruc` de la empresa.

## 5. Seguir la saga hasta el final
```bash
curl -s -H "Authorization: Bearer $APIKEY" $SAG/$COMPROBANTE_ID          # repetir cada pocos segundos
```
Camino esperado: `STOCK_PENDIENTE` → `STOCK_RESERVADO` → `FIRMADO` → `AUTORIZADO` → `NOTIFICADO`.
Terminales de error y qué hacer: `STOCK_NO_DISPONIBLE` (cargar stock, emitir otra), `FIRMA_FALLIDA` (revisar el motivo:
certificado vencido o sin certificado), `RECHAZADO_RECEPCION`/`NO_AUTORIZADO` (el motivo del SRI está en `failureReason`;
se corrige y se emite de nuevo), `NOTIFICACION_FALLIDA` (el comprobante sigue autorizado; revisar SMTP, no hay reenvío aún).

## 6. Verificar el resultado
```bash
curl -s -H "Authorization: Bearer $APIKEY" $COM/comprobantes/$COMPROBANTE_ID            # estado AUTORIZADO/NOTIFICADO
curl -s -H "Authorization: Bearer $APIKEY" $COM/comprobantes/$COMPROBANTE_ID/ride -o ride.pdf
curl -s -H "Authorization: Bearer $APIKEY" $COM/comprobantes/$COMPROBANTE_ID/xml  -o factura.xml
curl -s -H "Authorization: Bearer $APIKEY" $NOT/notificaciones/comprobante/$COMPROBANTE_ID   # ENVIADA o FALLIDA + motivo
curl -s -H "Authorization: Bearer $APIKEY" "$INV/items/$ITEM_ID"                        # cantidadDisponible bajó 1 (kardex)
```
El correo llega a `e.du.ingcharles@hotmail.com` con el RIDE (PDF) y el XML autorizado adjuntos.

Si la notificación quedó `FALLIDA` (saga `NOTIFICACION_FALLIDA`), reenvía el correo sin emitir otra factura:
```bash
curl -s -X POST -H "Authorization: Bearer $APIKEY" $COM/comprobantes/$COMPROBANTE_ID/reenviar-correo   # 202
# unos segundos después: la notificación pasa a ENVIADA y la saga a NOTIFICADO (si falla otra vez, la notificación queda `FALLIDA` con el nuevo motivo; la saga conserva el primero)
curl -s -H "Authorization: Bearer $APIKEY" $NOT/notificaciones/comprobante/$COMPROBANTE_ID
curl -s -H "Authorization: Bearer $APIKEY" $SAG/$COMPROBANTE_ID
```
Responde `409` si el comprobante aún no está autorizado o no tiene correo del receptor.

Para reenviar **todos** los fallidos de la empresa de una vez:
```bash
curl -s -X POST -H "Authorization: Bearer $APIKEY" "$SAG/reenviar-notificaciones?limite=100"   # 202 {"solicitados":N}
curl -s -H "Authorization: Bearer $APIKEY" "$SAG?state=NOTIFICACION_FALLIDA"                     # los que siguen fallando
```
Cada una pasa a `NOTIFICADO` si el envío sale bien; las que fallan de nuevo vuelven a `NOTIFICACION_FALLIDA`. Si hay más de `limite`, repite la llamada.

## 7. Problemas frecuentes
| Síntoma | Causa y solución |
|---|---|
| `403 IDE-403-003` al crear la api-key | El rol `ADMIN` se concede por Kafka tras crear la empresa; espera unos segundos (`GET $ID/usuarios/me/empresas` muestra cuándo ya está). |
| `401 Falta el header Authorization` en registro o login | `identidad-service` debe correr con `facturacion.filtro-api-key.habilitado=false` (ya viene en su `application.properties`). |
| `401 El token no es una api-key` en comprobantes, inventario, firma, sri-conector, notificacion u orquestador | Usaste el token de sesión (el del login). Ese solo vale en `identidad-service`; los otros 6 servicios piden la api-key (`POST $ID/api-keys`, paso 1). |
| Saga en `FIRMA_FALLIDA` | Certificado vencido, sin certificado o clave del `.p12` incorrecta. |
| Saga en `NOTIFICACION_FALLIDA` | SMTP: contraseña de aplicación incorrecta o cuenta sin verificación en dos pasos. El comprobante sigue autorizado. |
| Saga en `RECHAZADO_RECEPCION` con `ERROR SECUENCIAL REGISTRADO` | El SRI recuerda los secuenciales ya autorizados para ese RUC, establecimiento y punto aunque la base local se reinicie, que vuelve a emitir desde el 1. Sube el contador por encima del último autorizado: `update comprobantes.secuencial_comprobante set ultimo_secuencial = 10 where ruc='1722039953001' and tipo_comprobante='FACTURA';` (en `docker exec <postgres> psql -U postgres -d facturacion`). Si la fila no existe, emite una factura para que se cree y repite. |
| Saga `NOTIFICACION_FALLIDA` con motivo `ClosedChannelException` (o el log de notificacion con `Connection closed while SSL/TLS handshake was in progress`) | Algo en la red (firewall, antivirus con análisis de correo, ISP) corta el TLS en los puertos SMTP 587 y 465 aunque HTTPS funcione. Comprobación desde el contenedor: `docker exec <notificacion> sh -c 'echo QUIT \| openssl s_client -starttls smtp -connect smtp.gmail.com:587 -crlf'` debe terminar con `Cipher is TLS_AES_...`; si dice `unexpected eof`, el problema es de la red, no de la contraseña. Prueba desde otra red (p. ej. datos del celular) o pide al administrador abrir 587/465 hacia `smtp.gmail.com`.  Cuando la red ya lo permita, reenvía el correo sin emitir otra factura: `curl -s -X POST -H "Authorization: Bearer $APIKEY" $COM/comprobantes/$COMPROBANTE_ID/reenviar-correo` (`202`; si sale bien, la notificación pasa a `ENVIADA`, la saga a `NOTIFICADO` y el comprobante también; si falla, la notificación muestra el nuevo motivo). |
| Notificación `FALLIDA` con `IOException: socket was closed unexpected` y en el log `AUTH XOAUTH2 failed: 535-5.7.8 Username and Password not accepted` | El TLS ya funciona pero el cliente de correo intentó `XOAUTH2` con la contraseña como si fuera un token y Gmail cerró la conexión. `notificacion-service` fija `quarkus.mailer.auth-methods=PLAIN LOGIN`. Si persiste con `PLAIN`, la contraseña de aplicación es incorrecta: prueba `AUTH PLAIN` a mano (debe responder `235 Accepted`). |
| `409` al crear empresa o producto | Ya existen (RUC o `codigoPrincipal` únicos): reutiliza los ids o cambia el código. |

## 8. Con Postman
Los mismos pasos, listos para importar en `documentacion/postman/`:
- `Flujo_Factura_Real.postman_collection.json`: colección con los 6 bloques (identidad, firma, inventario, comprobantes, saga, verificación).
- `Local.postman_environment.json`: URLs de los servicios locales y dos variables secretas vacías.

1. **Importar** ambos archivos (Import → arrastrar) y elegir el entorno *Facturación local (docker compose)*.
2. En el entorno, llenar **solo en tu Postman** `password_cuenta` y `p12_password` (tipo *secret*; no se suben al repo).
3. Ejecutar en orden. Los scripts de *Tests* guardan solos `token`, `empresa_id`, `apikey`, `item_id` y `comprobante_id`; las
   peticiones siguientes los usan como `{{variable}}`. La colección autentica con `Bearer {{apikey}}`; el registro y el login
   no llevan auth, y la empresa y la api-key usan `{{token}}` (sesión).
4. En **2.1** y **2.2** elige el archivo (`.p12` y logo) en el campo `archivo` (*Select Files*): Postman no guarda rutas locales.
5. Entre **1.3** y **1.5** espera unos segundos: ejecuta **1.4** hasta que aparezca la empresa (si no, la api-key responde
   `403 IDE-403-003`).
6. **5.1** se repite sola en el *Collection Runner* (cada 3 s, máx. 30 intentos) hasta un estado final y falla si no es
   `NOTIFICADO`. Enviándola a mano, repítela tú.
7. **6.2** y **6.3**: *Send and Download* para guardar el PDF y el XML.

Variables de la colección que puedes cambiar: `ruc`, `razon_social`, `email_cuenta`, `email_comprador`. Para una prueba
repetida cambia `codigoPrincipal` del producto (3.1 y 4.1), que es único por RUC.

## 9. Resultado de la última corrida
Imágenes reconstruidas con el código actual, base de datos vacía y los pasos de las secciones 0 a 6 con el certificado real
(RUC `1722039953001`, ambiente `PRUEBAS`, factura de 1 tablet: 300.00 + IVA 15 % = 345.00).

| Paso | Resultado |
|---|---|
| Registro, login, empresa, api-key | `201`; el rol `ADMIN` ya estaba al primer intento |
| Certificado `.p12` | `201`, estado `ACTIVO`, vence 2031-04-18 |
| Logo, producto (20 en stock) | `201` |
| Factura | `201`; saga `STOCK_RESERVADO` → `FIRMADO` → `NOTIFICADO` en ~45 s |
| SRI pruebas | autorizada (`No. 001-001-000000011`) |
| Stock | 20 → 19, reserva liberada |
| Notificación | **No llegó el correo.** El envío falló (`ClosedChannelException`: la red cortó el TLS hacia `smtp.gmail.com`) pero el servicio lo guardó como `ENVIADA` y la saga como `NOTIFICADO`: un error de código ya corregido (el motivo salía vacío). Ver sección 7 |
| RIDE | formato SRI con logo, código de barras y totales; el logo no tapa la razón social |

Hallazgos de la corrida:
- Con la base reiniciada, la primera factura salió `RECHAZADO_RECEPCION` por `ERROR SECUENCIAL REGISTRADO` (ver sección 7);
  tras subir `ultimo_secuencial` la siguiente se autorizó.
- Un `.p12` con otra contraseña responde `400 FIR-400-001` y la saga de una factura sin certificado queda en `FIRMA_FALLIDA`
  (`La empresa … no tiene un certificado activo`).

### Segunda corrida: datos tributarios nuevos y correo
Con las 7 imágenes reconstruidas (comprobantes en V17), sobre la misma base, una factura con `regimenRimpe: RIMPE`,
`esGranContribuyente` con resolución y la línea con `detallesAdicionales` (`Centro Logistico: 23`):

| Paso | Resultado |
|---|---|
| Factura | `201`; autorizada por el SRI (`No. 001-001-000000013`) |
| XML autorizado | `<contribuyenteRimpe>`, `<detAdicional nombre="Medida" valor="UN"/>` (automático desde la unidad del ítem), `<detAdicional nombre="Centro Logistico" valor="23"/>` y `<campoAdicional nombre="Gran Contribuyente">…` |
| RIDE | leyenda RIMPE en el recuadro del emisor, `Medida: UN, Centro Logistico: 23` en la línea y `Gran Contribuyente` en Información Adicional |
| Notificación | `FALLIDA` con motivo `ClosedChannelException`; la saga quedó en `NOTIFICACION_FALLIDA` (la red corta el TLS hacia SMTP, sección 7) |

- El SRI **valida `agenteRetencion` contra el RUC**: con una resolución inventada respondió `NO_AUTORIZADO` con
  `ERROR AL VALIDAR AGENTE RETENCION`. Envíalo solo si el emisor figura como agente de retención.
- Una factura rechazada por el SRI también consume el secuencial.

### Reenvío del correo (misma factura, sin emitir otra)
Tras pasar a una red que no corta el TLS hacia SMTP, `POST /comprobantes/{id}/reenviar-correo` sobre la factura `001-001-000000013`:

| Intento | Resultado |
|---|---|
| 1.º | `202`; falló con `535-5.7.8` en `AUTH XOAUTH2` y el socket se cerró: el cliente probaba `XOAUTH2` antes que `PLAIN`. Se fijó `quarkus.mailer.auth-methods=PLAIN LOGIN` (la contraseña sí era válida: `AUTH PLAIN` respondió `235 Accepted`) |
| 2.º | `202`; notificación `ENVIADA`, saga `NOTIFICADO` (sin motivo de fallo), comprobante `NOTIFICADO` |

### Reenvío masivo de correos (`POST /sagas/reenviar-notificaciones`)
Prueba real del 2026-11-17 sobre la empresa `1722039953001`, con una sola saga en `NOTIFICACION_FALLIDA` (factura
`001-001-000000014`, motivo `ClosedChannelException`) y la red que todavía corta el TLS hacia SMTP:

| Llamada | Resultado |
|---|---|
| Sin api-key | `401` |
| `?limite=0` y `?limite=501` | `400` `GEN-400-001` (`must be greater than or equal to 1` / `less than or equal to 500`) |
| `?limite=100` | `202` `{"solicitados":1}` |
| La misma llamada otra vez, de inmediato | `202` `{"solicitados":0}`: la saga ya no estaba en `NOTIFICACION_FALLIDA` |

Recorrido de esa saga, con los logs de cada servicio: el orquestador la pasó a `AUTORIZADO` y emitió el comando
`REENVIAR_NOTIFICACION` (paso `SENT`); comprobantes-service lo atendió con `[COMP-202] … REENVIO_PUBLICADO`;
notificacion-service intentó el envío (la fecha `enviadoEn` de la notificación se actualizó) y falló con
`[NOT-401] … ClosedChannelException`; el orquestador recibió `NotificacionFallida` y la saga volvió a
`NOTIFICACION_FALLIDA`, con los pasos nuevos en su línea de tiempo (unos 7 segundos en total).

Cubierto: autenticación, validación del límite, filtro por estado, idempotencia de la segunda llamada, cadena completa
orquestador → comprobantes → notificación y vuelta a `NOTIFICACION_FALLIDA` cuando el envío falla otra vez.

#### Segunda corrida, en una red que no corta el TLS hacia SMTP
Misma saga (`001-001-000000014`), tras comprobar que `openssl s_client -starttls smtp` conecta con TLS 1.3:

| Llamada | Resultado |
|---|---|
| `?limite=100` | `202` `{"solicitados":1}` |
| Unos 12 s después | saga `NOTIFICADO` sin motivo de fallo; notificación `ENVIADA` (`enviadoEn` actualizado) |
| La misma llamada otra vez | `202` `{"solicitados":0}`: ya no hay sagas en `NOTIFICACION_FALLIDA` |

Queda probado el camino completo: `NOTIFICACION_FALLIDA` → `AUTORIZADO` → `REENVIAR_NOTIFICACION` → `NOTIFICACION_FALLIDA` o `NOTIFICADO`
según salga el correo. Probado con una sola saga; no se ejerció el límite ni el recorrido de varias a la vez.
