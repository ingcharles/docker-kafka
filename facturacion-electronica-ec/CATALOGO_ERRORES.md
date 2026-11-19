# Catálogo de errores HTTP

Toda respuesta de error de los microservicios tiene el mismo cuerpo (`ApiErrorResponse`, en `facturacion-commons`):

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "texto", "details": ["opcional"], "codigo": "GEN-404-001" }
```

- **`codigo`** es estable y es lo que debe usar el cliente para decidir (el `message` puede cambiar de redacción). Formato `<PREFIJO>-<HTTP>-<NNN>`; el segmento HTTP siempre coincide con el `status` (lo verifica `CatalogoErroresTest`).
- Cada servicio declara sus errores en un enum que implementa `ErrorCatalogo` (`codigo`, `estadoHttp`, `claveMensaje`); el texto vive en su `mensajes.properties`, nunca en el código.
- El código de negocio solo lanza la excepción (`throw new NotFoundException(...)`, `new ErrorNegocioException(CodigoError.X, params)`, o una propia que extienda `ApiException`). **No arma `Response`**: el único `ApiExceptionMapper` (jar común) la convierte en HTTP usando su entrada del catálogo.
- Los `throw new NotFoundException("texto")` / `SinPermisoException` / `TokenInvalidoException` / `ConflictoException` con texto literal siguen funcionando y salen con el código genérico `GEN-*`; para un código propio se usa la sobrecarga con la entrada del catálogo del servicio.
- Errores que no son del catálogo: `IllegalArgumentException` → 400 `GEN-400-002`; `ConstraintViolationException` (Bean Validation) → 400 `GEN-400-001` con `details`; una consulta o un bloqueo que vence su plazo en la base de datos (`lock_timeout`, `statement_timeout`, `idle_in_transaction_session_timeout`) → 503 `GEN-503-001` con `Retry-After: 5` y el mismo formato; cualquier otra no controlada → 500 `GEN-500-001` (sin stack trace al cliente).

## Comunes — `GEN` (facturacion-commons)

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `GEN-400-001` | 400 | Falló la validación |
| `GEN-400-002` | 400 | {0} |
| `GEN-401-001` | 401 | {0} |
| `GEN-401-002` | 401 | Falta el header Authorization: Bearer <api-key> |
| `GEN-401-003` | 401 | Falta el header Authorization: Bearer <api-key o token de sesión> |
| `GEN-401-004` | 401 | El token no es una api-key |
| `GEN-401-005` | 401 | El token no es una api-key ni un token de sesión válido |
| `GEN-401-006` | 401 | El token no es un token de sesión |
| `GEN-401-007` | 401 | Token inválido o expirado |
| `GEN-401-008` | 401 | La api-key fue revocada |
| `GEN-403-001` | 403 | {0} |
| `GEN-403-002` | 403 | La api-key usada es para el ruc {0}, no para {1} |
| `GEN-403-003` | 403 | La api-key usada es solo para el establecimiento {0}, no para {1} |
| `GEN-403-004` | 403 | La api-key usada es solo para el punto de emisión {0}, no para {1} |
| `GEN-403-005` | 403 | La api-key tiene rol LECTURA: solo puede consultar, no crear ni modificar. |
| `GEN-403-006` | 403 | Esta operación requiere una api-key con rol ADMIN (la actual es {0}). |
| `GEN-404-001` | 404 | {0} |
| `GEN-409-001` | 409 | {0} |
| `GEN-503-001` | 503 | El servicio está ocupado en este momento: reintenta en unos segundos |
| `GEN-500-001` | 500 | Ocurrió un error inesperado |
| `GEN-500-002` | 500 | No se pudo serializar el payload del outbox (el cliente recibe el texto genérico; el detalle queda en el log GEN-403) |

## comprobantes-service — `COMP`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `COMP-400-101` | 400 | {0}.formaPago '{1}' no existe o no está vigente en la Tabla 24 del SRI. Códigos válidos: {2}. |
| `COMP-400-102` | 400 | {0}: en el Comprobante de Retención v2.0.0 el pago solo admite formaPago y total; quita plazo/unidadTiempo. |
| `COMP-400-103` | 400 | {0}: plazo y unidadTiempo van juntos — se mandó plazo sin unidadTiempo. |
| `COMP-400-104` | 400 | {0}: plazo y unidadTiempo van juntos — se mandó unidadTiempo sin plazo. |
| `COMP-400-105` | 400 | {0}.plazo '{1}' debe ser numérico. |
| `COMP-400-106` | 400 | {0}.plazo '{1}' debe ser >= 0 y tener como máximo 2 decimales. |
| `COMP-400-107` | 400 | {0}.unidadTiempo debe tener como máximo {1} caracteres. |
| `COMP-400-108` | 400 | La suma de los pagos ({0}) no coincide con el importe total del comprobante ({1}). |
| `COMP-400-109` | 400 | {0}: la forma de pago {1} (endoso de títulos, factura comercial negociable — Anexo 11) exige plazo y unidadTiempo. |
| `COMP-400-110` | 400 | La factura comercial negociable (forma de pago {0} — Anexo 11) exige compradorDireccion. |
| `COMP-400-201` | 400 | El impuesto '{0}' no existe en la Tabla 16 del SRI. Códigos válidos: {1}. |
| `COMP-400-202` | 400 | El codigoPorcentaje '{0}' no existe o no está vigente para el impuesto '{1}' (Tabla 17). Válidos: {2}. |
| `COMP-400-203` | 400 | La tarifa {0} no coincide con la del catálogo ({1}) para el impuesto '{2}' codigoPorcentaje '{3}'. |
| `COMP-400-204` | 400 | No hay tarifa para el impuesto '{0}' codigoPorcentaje '{1}': el catálogo de tarifas de ese impuesto está vacío (ICE/IRBPNR no lo tienen; para IVA puede que catalogos-service aún no haya sincronizado) y el request no trae tarifa. |
| `COMP-400-205` | 400 | baseImponible solo se puede indicar para el ICE y el IRBPNR (la base del IVA la calcula el servidor); impuesto '{0}'. |
| `COMP-400-206` | 400 | El impuesto '{0}' codigoPorcentaje '{1}' no tiene tarifa de tipo {2} vigente a la fecha de emisión. Tipos disponibles: {3}. |
| `COMP-400-301` | 400 | {0} '{1}' no existe en la Tabla 6 del SRI. Códigos válidos: {2}. |
| `COMP-400-302` | 400 | {0} '{1}' ({2}) solo se admite en Factura; este comprobante debe identificar al receptor. |
| `COMP-400-303` | 400 | {0}: el tipo {1} ({2}) debe identificarse con {3}. |
| `COMP-400-304` | 400 | {0}: {1} solo es válido con el tipo de identificación {2} ({3}). |
| `COMP-400-305` | 400 | {0}: {1} debe tener exactamente {2} dígitos (se recibió '{3}'). |
| `COMP-400-306` | 400 | {0}: una factura a consumidor final no puede superar {1} USD (el total es {2}); identifique al adquirente (ficha técnica, 8.10). |
| `COMP-400-401` | 400 | El impuesto a retener '{0}' no existe en la Tabla 19 del SRI. Códigos válidos: {1}. |
| `COMP-400-402` | 400 | El codigoRetencion '{0}' no existe para el impuesto a retener '{1}' (Tabla 20). Vigentes: {2}. |
| `COMP-400-403` | 400 | El codigoRetencion '{0}' existe pero no está vigente a la fecha de emisión ({3}) para el impuesto a retener '{1}' (Tabla 20). Vigentes: {2}. |
| `COMP-400-404` | 400 | El porcentajeRetener {0} no coincide con el del catálogo ({1}) para el codigoRetencion '{2}' del impuesto '{3}'. |
| `COMP-400-405` | 400 | {0}: valorRetenido {1} no corresponde a baseImponible {2} x porcentajeRetener {3}% (= {4}). |
| `COMP-400-406` | 400 | El codSustento '{0}' no existe en el catálogo de sustento tributario. Válidos: {1}. |
| `COMP-400-407` | 400 | docSustentoFecha '{0}' no es una fecha válida (formato dd/MM/aaaa). |
| `COMP-400-408` | 400 | La fecha '{0}' no es válida: debe existir en el calendario y tener el formato dd/MM/aaaa. |
| `COMP-400-501` | 400 | El ítem '{0}' ({1}) está dado de baja en inventario y no se puede facturar. |
| `COMP-400-502` | 400 | La línea '{0}' no trae impuestos y tampoco un itemId del que derivarlos. |
| `COMP-400-503` | 400 | La línea '{0}' no trae impuestos y el itemId {1} no está (todavía) en el catálogo local de inventario-service para esta empresa — puede que el evento ItemCreado no haya llegado aún, que el ítem no exista, o que pertenezca a otra empresa. |
| `COMP-400-504` | 400 | La línea con el código de combustible {0} requiere la placa del vehículo en la factura (Anexo 12 de la ficha técnica) |
| `COMP-400-601` | 400 | El establecimiento '{0}' no está registrado (o no está activo) para el ruc {1} — puede que el evento EstablecimientoRegistrado no haya llegado aún, que el establecimiento no exista, o que pertenezca a otra empresa. |
| `COMP-400-602` | 400 | El punto de emisión '{0}' no está registrado (o no está activo) para el establecimiento '{1}' del ruc {2}. |
| `COMP-400-603` | 400 | empresa.ambiente debe ser PRUEBAS o PRODUCCION, se recibió: {0} |
| `COMP-400-801` | 400 | El tipo de comprobante {0} todavía no está soportado en esta versión |
| `COMP-400-811` | 400 | El ticket solo está disponible para factura, nota de crédito, nota de débito y liquidación de compra; el comprobante es {0}. |
| `COMP-400-812` | 400 | El parámetro '{0}' no admite el valor '{1}'. Valores válidos: {2}. |
| `COMP-400-821` | 400 | El XML generado no cumple el esquema XSD del SRI ({0}): {1} |
| `COMP-400-831` | 400 | Tipo de comando de comprobante desconocido: {0} |
| `COMP-400-841` | 400 | La fecha de inicio del transporte ({0}) debe ser la fecha de emisión de la guía ({1}): el SRI toma una como la otra |
| `COMP-409-901` | 409 | El comprobante está {0}: solo se reenvía el correo de un comprobante autorizado por el SRI y con RIDE |
| `COMP-409-902` | 409 | El comprobante no tiene correo del receptor al cual reenviarlo |
| `COMP-409-911` | 409 | Comando {0} fuera de orden: el comprobante {1} está {2} y no puede pasar a {3} sin aplicar antes los pasos intermedios |
| `COMP-400-851` | 400 | Falta la cabecera {0}: manda una clave única por cada comprobante que quieres emitir, para poder reintentar sin duplicarlo |
| `COMP-400-852` | 400 | La cabecera {0} debe tener entre {1} y {2} caracteres (letras, números y . _ : -) |
| `COMP-422-851` | 422 | La clave de idempotencia ya se usó con una solicitud distinta; usa una clave nueva para un comprobante nuevo |
| `COMP-409-921` | 409 | La solicitud con esa clave de idempotencia sigue en curso; reintenta en unos segundos |
| `COMP-400-861` | 400 | docModificadoNumero '{0}' no tiene el formato 001-001-000000001 (establecimiento, punto de emisión y secuencial de 9 dígitos) |
| `COMP-400-862` | 400 | docModificadoFecha ({0}) no puede ser posterior a hoy ({1}) |
| `COMP-400-863` | 400 | El documento que modifica ({0}) está {1}: solo se puede modificar un comprobante autorizado por el SRI |
| `COMP-400-864` | 400 | docModificadoFecha ({0}) no coincide con la fecha de emisión del documento {1}, que es {2} |
| `COMP-400-865` | 400 | La nota de crédito por {0}, sumada a las ya emitidas sobre el documento {1} ({2}), supera el valor del documento ({3}) |
| `COMP-404-001` | 404 | Comprobante no encontrado: {0} |
| `COMP-404-701` | 404 | No existe el catálogo '{0}'. Disponibles: {1}. |
| `COMP-500-001` | 500 | No se pudo serializar los campos adicionales del comprobante |
| `COMP-500-002` | 500 | No se pudo leer los campos adicionales del comprobante |
| `COMP-500-003` | 500 | No se pudo serializar los detalles adicionales de la línea |
| `COMP-500-004` | 500 | No se pudo leer los detalles adicionales de la línea |
| `COMP-500-005` | 500 | No se pudo generar el XML del comprobante {0} ({1}) |
| `COMP-500-006` | 500 | No se pudo generar el RIDE del comprobante {0} |
| `COMP-500-007` | 500 | No se encontró el XSD empaquetado: {0} |
| `COMP-500-008` | 500 | No se pudo compilar el XSD {0} |
| `COMP-500-009` | 500 | Error de E/S validando el XML contra el XSD |
| `COMP-403-701` | 403 | La escritura de catálogos está deshabilitada: configura CATALOGOS_ADMIN_TOKEN. |
| `COMP-403-702` | 403 | Falta o es inválido el header {0}. |
| `COMP-400-701` | 400 | La fecha '{0}' debe tener el formato yyyy-MM-dd. |
| `COMP-400-702` | 400 | El body es obligatorio. |
| `COMP-400-703` | 400 | El campo '{0}' no existe en el catálogo '{1}'. Campos: {2}. |
| `COMP-400-704` | 400 | Catálogo '{0}': hay campos inválidos. |
| `COMP-400-705` | 400 | Dato inválido para el catálogo: {0} |
| `COMP-400-711` | 400 | '{0}' es obligatorio |
| `COMP-400-712` | 400 | '{0}' debe tener entre 1 y {1} caracteres |
| `COMP-400-713` | 400 | '{0}' debe ser numérico |
| `COMP-400-714` | 400 | '{0}' debe ser una fecha yyyy-MM-dd |
| `COMP-400-715` | 400 | '{0}' debe ser true o false |
| `COMP-400-716` | 400 | '{0}' debe estar entre 0 y {1} |
| `COMP-400-717` | 400 | 'vigente_hasta' no puede ser anterior a 'vigente_desde' |
| `COMP-400-718` | 400 | '{0}' debe ser uno de: {1} |

## inventario-service — `INV`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `INV-400-001` | 400 | La categoría es de tipo {0} y no admite ítems de tipo {1} |
| `INV-400-002` | 400 | El ítem {0} es un SERVICIO — no maneja existencias ni kardex |
| `INV-400-005` | 400 | El archivo de imagen está vacío |
| `INV-400-006` | 400 | No se pudo leer el archivo de imagen subido |
| `INV-400-007` | 400 | La imagen supera el tamaño máximo permitido de {0} MB |
| `INV-400-008` | 400 | Tipo de imagen no soportado: {0} (permitidos: {1}) |
| `INV-404-001` | 404 | Ítem no encontrado: {0} |
| `INV-404-002` | 404 | Categoría no encontrada: {0} |
| `INV-404-003` | 404 | Imagen no encontrada: {0} |
| `INV-409-001` | 409 | Stock insuficiente para el ítem {0} |

## firma-electronica-service — `FIR`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `FIR-400-001` | 400 | El archivo no es un .p12 válido o la contraseña dada no lo abre |
| `FIR-400-002` | 400 | El archivo del logo está vacío |
| `FIR-400-003` | 400 | No se pudo leer el archivo del logo subido |
| `FIR-400-004` | 400 | No se pudo leer el archivo del certificado subido |
| `FIR-400-005` | 400 | Comando de firma inválido: falta eventId o comprobanteId ({0}) |
| `FIR-400-006` | 400 | El archivo .p12 no contiene ningún alias/entrada |
| `FIR-400-007` | 400 | No se encontró un certificado X.509 en el .p12 |
| `FIR-400-008` | 400 | El certificado venció el {0}: sube uno vigente |
| `FIR-400-009` | 400 | El certificado todavía no es válido: empieza el {0} |
| `FIR-400-010` | 400 | El certificado de la empresa con ruc {0} venció el {1}: sube uno vigente |
| `FIR-400-011` | 400 | No hay ninguna empresa registrada con ruc {0} |
| `FIR-400-012` | 400 | La empresa con ruc {0} no tiene un certificado activo |
| `FIR-400-013` | 400 | No se pudo firmar el comprobante: {0} |
| `FIR-400-014` | 400 | El logo pesa {0} KB; el máximo es {1} KB |
| `FIR-400-015` | 400 | El logo debe ser una imagen PNG o JPEG |
| `FIR-404-001` | 404 | Empresa no encontrada: {0} |
| `FIR-404-002` | 404 | Establecimiento no encontrado: {0} |
| `FIR-404-003` | 404 | Punto de emisión no encontrado: {0} |
| `FIR-404-004` | 404 | La empresa {0} no tiene un certificado activo |
| `FIR-404-005` | 404 | No existe la configuración '{0}' para la empresa {1} |
| `FIR-404-006` | 404 | La empresa {0} no tiene logo |
| `FIR-409-001` | 409 | Ya existe un establecimiento activo con el código {0} para esta empresa |
| `FIR-409-002` | 409 | Ya existe una empresa registrada con el RUC {0} |
| `FIR-409-003` | 409 | Ya existe un punto de emisión activo con el código {0} para este establecimiento |
| `FIR-409-004` | 409 | Ya existe la configuración '{0}' para la empresa {1} |
| `FIR-500-001` | 500 | No se pudo cifrar el material criptográfico |
| `FIR-500-002` | 500 | No se pudo descifrar el material criptográfico |
| `FIR-500-003` | 500 | No se pudo derivar la clave de cifrado |

## identidad-service — `IDE`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `IDE-400-001` | 400 | puntoEmision requiere que también se indique establecimiento |
| `IDE-401-001` | 401 | Email o contraseña incorrectos |
| `IDE-401-002` | 401 | La contraseña actual no es correcta |
| `IDE-429-001` | 429 | Hay demasiadas autenticaciones en curso: reintenta en unos segundos |
| `IDE-403-001` | 403 | Se requiere el rol ADMIN sobre el ruc {0} para gestionar sus miembros |
| `IDE-403-003` | 403 | El usuario no está asociado al ruc {0} — pide a un ADMIN de esa empresa que te agregue (POST /empresas/{0}/usuarios) |
| `IDE-404-001` | 404 | Usuario no encontrado: {0} |
| `IDE-404-002` | 404 | No hay ningún usuario registrado con el correo {0} |
| `IDE-404-003` | 404 | No estás asociado al ruc {0} |
| `IDE-404-004` | 404 | El usuario {0} no está asociado al ruc {1} |
| `IDE-404-005` | 404 | No existe un establecimiento activo '{0}' para el ruc {1} |
| `IDE-404-006` | 404 | No existe un punto de emisión activo '{0}' para el establecimiento '{1}' del ruc {2} |
| `IDE-404-007` | 404 | No hay una api-key {0} para este usuario |
| `IDE-409-001` | 409 | Ya existe un usuario registrado con el email {0} |
| `IDE-409-002` | 409 | Ya existe una api-key llamada '{0}' para este usuario |
| `IDE-409-003` | 409 | El usuario {0} ya tiene acceso al ruc {1} (para cambiarle el rol: PATCH /empresas/{1}/usuarios/{2}) |
| `IDE-409-004` | 409 | No se puede dejar al ruc {0} sin ningún ADMIN |

## notificacion-service — `NOT`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `NOT-400-001` | 400 | El tipo de comprobante {0} no tiene plantilla de correo; usa uno de: {1} |
| `NOT-400-002` | 400 | La variable {0} no existe; las permitidas son: {1} |
| `NOT-400-003` | 400 | La variable {0} está dentro de una etiqueta HTML: las variables solo pueden ir en el texto del correo, no en atributos como href o src |
| `NOT-400-004` | 400 | El asunto debe ir en una sola línea |
| `NOT-400-005` | 400 | La variable {0} no se puede usar en el asunto: es una imagen |
| `NOT-400-006` | 400 | El cuerpo HTML queda vacío después de quitar lo que no se permite (scripts, formularios, marcos, estilos y direcciones peligrosas) |
| `NOT-404-001` | 404 | No hay ninguna notificación para el comprobante {0} |
| `NOT-404-002` | 404 | La empresa no tiene una plantilla de correo propia para {0}: el correo sale con la plantilla por defecto |

## sri-conector-service — `SRI`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `SRI-400-001` | 400 | Comando de envío al SRI inválido: falta eventId o comprobanteId ({0}) |
| `SRI-404-001` | 404 | No hay ningún envío al SRI para el comprobante {0} |
| `SRI-404-002` | 404 | No hay ningún envío al SRI con clave de acceso {0} |
| `SRI-404-003` | 404 | EnvioSri no encontrado: {0} |
| `SRI-502-001` | 502 | No se pudo contactar al WS de Recepción del SRI ({0}) |
| `SRI-502-002` | 502 | No se pudo contactar al WS de Autorización del SRI ({0}) |

## facturacion-orchestrator-service — `ORQ`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `ORQ-404-001` | 404 | No hay saga para el comprobante {0} |
| `ORQ-500-001` | 500 | No se pudo leer el payload guardado de la saga |
| `ORQ-409-001` | 409 | La saga {0} no tiene el comando de firma pendiente |

Motivos que el orquestador registra en la saga (`failure_reason` y bitácora `saga_step`); no se devuelven como error HTTP.

Sus códigos son logs (`ORQ-2xx` informativos, `ORQ-4xx` de error) y están en la sección "Logs" de este documento.

## Logs — `<PREFIJO>-<NNN>`

Todo log del código propio lleva un código estable: `[FIR-401] texto (comprobanteId=…)`. El **primer dígito del número es el nivel**; el prefijo es el del servicio.

| Nivel | Identificador | Ejemplo | Cuándo se usa |
|---|---|---|---|
| DEBUG | 100 | 101, 102, … 199 | Solo al investigar un error específico. |
| INFO | 200 | 201, 202, … 299 | Información general (inicio, decisiones importantes, cambios de configuración, hechos del flujo). |
| WARN | 300 | 301, 302, … 399 | Advertencia o información que no representa un problema por sí misma. |
| ERROR | 400 | 401, 402, … 499 | Un problema sobre el que hay que tomar acción. |
| FATAL | 500 | 501, 502, … 599 | Estado irrecuperable de la aplicación (p. ej. `OutOfMemoryError`, apagado del servidor). Hoy ningún servicio lo emite. |

- Cada servicio declara sus logs en un enum `CodigoLog` (`codigo`, `claveMensaje`) y se registran con `LogCodificado` (en `facturacion-commons`): `debug`, `info`, `warn`, `error` y `fatal`. El texto vive en su `mensajes.properties`, igual que el de los errores. El orquestador usa su `CodigoError` (los motivos de la saga son también logs).
- El contexto lleva solo identificadores (`comprobanteId`, `claveAcceso`, `eventoId`), nunca correos, razón social, tokens ni contraseñas.
- Un código nuevo: agrégalo al enum con el primer dígito de su nivel, escribe su texto en `mensajes.properties` y añádelo aquí. Todo consumidor de Kafka (`@Incoming`) debe llevar `@LogFallosMensajeria`.
- Quedan fuera los logs propios de las librerías (SmallRye, Quarkus, Hibernate). Los mensajes que fallan en un consumidor de Kafka (y van a su DLQ o se descartan) los registra `GEN-402` con el canal y los ids.

### facturacion-commons (compartido) — `GEN`

| Código | Nivel | Texto |
|---|---|---|
| `GEN-101` | DEBUG | Token rechazado: {0} |
| `GEN-301` | WARN | Revocación de api-key ignorada: {0} |
| `GEN-302` | WARN | La base de datos venció un plazo (estado SQL {0}: bloqueo, sentencia o transacción inactiva); el cliente recibe 503 y reintenta |
| `GEN-401` | ERROR | Excepción no controlada en un endpoint; el cliente recibe el 500 genérico |
| `GEN-402` | ERROR | Falló el procesamiento de un mensaje del canal {0} ({1}): según la estrategia del canal pasa a su DLQ (.dlq) o se descarta |
| `GEN-403` | ERROR | Falló una operación interna ({0}): {1} — la lanzó una `ErrorInternoException`; el cliente recibe solo el texto genérico y el código |

### comprobantes-service — `COMP`

| Código | Nivel | Texto |
|---|---|---|
| `COMP-201` | INFO | Se descarta el comando {0}: ya se procesó antes |
| `COMP-202` | INFO | Comando {0} ({1}): {2} |
| `COMP-203` | INFO | Se devolvió el comprobante ya emitido por una solicitud repetida (clave de idempotencia) |
| `COMP-204` | INFO | Se purgaron las claves de idempotencia vencidas |
| `COMP-301` | WARN | Se ignora el comando {0} ({1}): no se puede pasar de {2} a {3} |
| `COMP-302` | WARN | No se reenvía la notificación: {0} |
| `COMP-303` | WARN | El ajuste ride.logo no es base64 válido; el RIDE sale sin logo |
| `COMP-401` | ERROR | El comando {0} ({1}) salta pasos del ciclo ({2} -> {3}): el comprobante puede quedar atascado |
| `COMP-402` | ERROR | No se pudo publicar el evento del outbox en Kafka; se reintenta en el siguiente ciclo |

### firma-electronica-service — `FIR`

| Código | Nivel | Texto |
|---|---|---|
| `FIR-201` | INFO | Se descarta el comando {0}: ya se procesó antes |
| `FIR-301` | WARN | {0} certificado(s) de firma vencieron y pasaron a EXPIRADO: hay que subir uno nuevo |
| `FIR-401` | ERROR | Falló la firma del comprobante: revisa el certificado de la empresa (activo, vigente y con su clave correcta) |
| `FIR-402` | ERROR | No se pudo publicar el evento del outbox en Kafka; se reintenta en el siguiente ciclo |
| `FIR-403` | ERROR | No hay emitter configurado para el tópico del evento del outbox (error de configuración); se reintenta luego |

### inventario-service — `INV`

| Código | Nivel | Texto |
|---|---|---|
| `INV-201` | INFO | Se descarta el comando {0}: ya se procesó antes |
| `INV-301` | WARN | No hay stock para el comprobante: faltan {0} ítem(s) y la saga lo cancela |
| `INV-302` | WARN | Tipo de comando de stock desconocido: {0} |
| `INV-401` | ERROR | No se pudo publicar el evento del outbox en Kafka; se reintenta en el siguiente ciclo |

### notificacion-service — `NOT`

| Código | Nivel | Texto |
|---|---|---|
| `NOT-201` | INFO | Se descarta el evento {0}: ya se procesó antes |
| `NOT-202` | INFO | Plantilla de correo {0} guardada (versión {1}) |
| `NOT-203` | INFO | Plantilla de correo {0} eliminada: el correo vuelve a la plantilla por defecto |
| `NOT-301` | WARN | ComprobanteAutorizado sin correo del receptor: no debería haberse publicado, se descarta |
| `NOT-302` | WARN | La plantilla propia de la empresa no se pudo aplicar ({0}): el correo sale con la plantilla por defecto |
| `NOT-303` | WARN | El logo de la empresa no es una imagen PNG o JPEG válida: el correo sale sin logo |
| `NOT-401` | ERROR | No se pudo enviar el correo del comprobante; la saga queda en NOTIFICACION_FALLIDA y se reenvía con POST /comprobantes/{id}/reenviar-correo o en masa con POST /sagas/reenviar-notificaciones |
| `NOT-402` | ERROR | No se pudo publicar el evento del outbox en Kafka; se reintenta en el siguiente ciclo |

### sri-conector-service — `SRI`

| Código | Nivel | Texto |
|---|---|---|
| `SRI-201` | INFO | Recepción SRI: {0} |
| `SRI-202` | INFO | Autorización SRI pendiente (intento {0} de {1}) |
| `SRI-203` | INFO | Autorización SRI: {0} |
| `SRI-204` | INFO | Se descarta el comando {0}: ya se procesó antes |
| `SRI-205` | INFO | El WS de Autorización del SRI respondió HTTP {0} |
| `SRI-206` | INFO | El WS de Recepción del SRI respondió HTTP {0} para el ambiente {1} |
| `SRI-301` | WARN | Se ignora el override de URL del SRI {0}: no es https o su host no está en sri.url.hosts-permitidos; se usa la URL oficial |
| `SRI-401` | ERROR | El SRI rechazó la recepción del comprobante: {0} |
| `SRI-402` | ERROR | El SRI no autorizó el comprobante: {0} |
| `SRI-403` | ERROR | El SRI no dio una respuesta final tras {0} intentos: requiere revisión manual |
| `SRI-404` | ERROR | Falló la consulta de autorización al SRI; se reintenta en el siguiente ciclo |
| `SRI-405` | ERROR | No se pudo publicar el evento del outbox en Kafka; se reintenta en el siguiente ciclo |

### facturacion-orchestrator-service — `ORQ`

| Código | Nivel | Texto |
|---|---|---|
| `ORQ-201` | INFO | Reintento automático {0} de {1}: sin respuesta en estado {2}, se vuelve a publicar el último comando. |
| `ORQ-202` | INFO | {0} saga(s) reintentadas o expiradas |
| `ORQ-203` | INFO | Reenvío automático {0} de {1} del correo: se pide a comprobantes-service que vuelva a publicar el comprobante autorizado. |
| `ORQ-204` | INFO | {0} correo(s) reenviado(s) y {1} saga(s) sin respuesta devuelta(s) a NOTIFICACION_FALLIDA |
| `ORQ-301` | WARN | No llegó el resultado del envío del correo en {0} minuto(s); la saga vuelve a NOTIFICACION_FALLIDA para reintentarlo. |
| `ORQ-401` | ERROR | Sin respuesta en estado {0} tras {1} reintentos automáticos; requiere revisión manual (POST /sagas/{id}/retry para forzar otro intento). |
| `ORQ-402` | ERROR | El reintento automático {0} de {1} falló y se descartó: {2} |
| `ORQ-403` | ERROR | No se pudo publicar el evento del outbox en Kafka; se reintenta en el siguiente ciclo |
| `ORQ-404` | ERROR | No hay emitter configurado para el tópico del evento del outbox (error de configuración); se reintenta luego |
| `ORQ-405` | ERROR | Falló la revisión periódica de sagas; se vuelve a intentar en el siguiente ciclo |
| `ORQ-406` | ERROR | Falló el reintento automático de la saga; se cuenta como intento |
| `ORQ-407` | ERROR | No se pudo registrar el reintento fallido de la saga |
| `ORQ-408` | ERROR | Falló la revisión periódica de reenvío de correos; se vuelve a intentar en el siguiente ciclo |
| `ORQ-409` | ERROR | Falló el reenvío automático del correo de la saga; no se contó como intento y se vuelve a intentar en el siguiente ciclo |

## Cómo agregar un error

1. Añade la entrada al enum del servicio (`CodigoError`/`FirmaError`/`IdentidadError`) con su `<PREFIJO>-<HTTP>-<NNN>` único.
2. Añade el texto en el `mensajes.properties` del servicio (la clave no puede repetirse).
3. Lánzalo con `new ErrorNegocioException(CodigoError.X, params)` (o una excepción propia que extienda `ApiException`). No hace falta mapper.
