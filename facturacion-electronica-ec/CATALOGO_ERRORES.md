# Catálogo de errores HTTP

Toda respuesta de error de los microservicios tiene el mismo cuerpo (`ApiErrorResponse`, en `facturacion-commons`):

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "texto", "details": ["opcional"], "codigo": "GEN-404-001" }
```

- **`codigo`** es estable y es lo que debe usar el cliente para decidir (el `message` puede cambiar de redacción). Formato `<PREFIJO>-<HTTP>-<NNN>`; el segmento HTTP siempre coincide con el `status` (lo verifica `CatalogoErroresTest`).
- Cada servicio declara sus errores en un enum que implementa `ErrorCatalogo` (`codigo`, `estadoHttp`, `claveMensaje`); el texto vive en su `mensajes.properties`, nunca en el código.
- El código de negocio solo lanza la excepción (`throw new NotFoundException(...)`, `new ErrorNegocioException(CodigoError.X, params)`, o una propia que extienda `ApiException`). **No arma `Response`**: el único `ApiExceptionMapper` (jar común) la convierte en HTTP usando su entrada del catálogo.
- Los `throw new NotFoundException("texto")` / `SinPermisoException` / `TokenInvalidoException` / `ConflictoException` con texto literal siguen funcionando y salen con el código genérico `GEN-*`; para un código propio se usa la sobrecarga con la entrada del catálogo del servicio.
- Errores que no son del catálogo: `IllegalArgumentException` → 400 `GEN-400-002`; `ConstraintViolationException` (Bean Validation) → 400 `GEN-400-001` con `details`; cualquier otra no controlada → 500 `GEN-500-001` (sin stack trace al cliente).

## Comunes — `GEN` (facturacion-commons)

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `GEN-400-001` | 400 | Falló la validación |
| `GEN-400-002` | 400 | {0} |
| `GEN-401-001` | 401 | {0} |
| `GEN-403-001` | 403 | {0} |
| `GEN-404-001` | 404 | {0} |
| `GEN-409-001` | 409 | {0} |
| `GEN-500-001` | 500 | Ocurrió un error inesperado |

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
| `COMP-400-601` | 400 | El establecimiento '{0}' no está registrado (o no está activo) para el ruc {1} — puede que el evento EstablecimientoRegistrado no haya llegado aún, que el establecimiento no exista, o que pertenezca a otra empresa. |
| `COMP-400-602` | 400 | El punto de emisión '{0}' no está registrado (o no está activo) para el establecimiento '{1}' del ruc {2}. |
| `COMP-400-603` | 400 | empresa.ambiente debe ser PRUEBAS o PRODUCCION, se recibió: {0} |
| `COMP-400-801` | 400 | El tipo de comprobante {0} todavía no está soportado en esta versión |
| `COMP-404-701` | 404 | No existe el catálogo '{0}'. Disponibles: {1}. |
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

## inventario-service — `INV`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `INV-400-001` | 400 | La categoría es de tipo {0} y no admite ítems de tipo {1} |
| `INV-400-002` | 400 | El ítem {0} es un SERVICIO — no maneja existencias ni kardex |
| `INV-400-003` | 400 | tieneIce=true exige codigoPorcentajeIce. |
| `INV-400-004` | 400 | tieneIce=true exige tarifaIce (>= 0). |
| `INV-400-005` | 400 | {0} |
| `INV-409-001` | 409 | Stock insuficiente para el ítem {0} |

## firma-electronica-service — `FIR`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `FIR-400-001` | 400 | {0} |
| `FIR-400-002` | 400 | {0} |
| `FIR-409-001` | 409 | {0} |
| `FIR-409-002` | 409 | Ya existe una empresa registrada con el RUC {0} |

## identidad-service — `IDE`

| Código | HTTP | Mensaje (`{n}` = parámetro) |
|---|---|---|
| `IDE-401-001` | 401 | {0} |
| `IDE-403-001` | 403 | Se requiere el rol ADMIN sobre el ruc {0} para gestionar sus miembros |
| `IDE-403-003` | 403 | El usuario no está asociado al ruc {0} — pide a un ADMIN de esa empresa que te agregue (POST /empresas/{0}/usuarios) |
| `IDE-409-001` | 409 | Ya existe un usuario registrado con el email {0} |

## facturacion-orchestrator-service — `ORQ` (sin respuesta HTTP)

Motivos que el orquestador registra en la saga (`failure_reason` y bitácora `saga_step`); no se devuelven como error HTTP.

| Código | Mensaje |
|---|---|
| `ORQ-001` | Reintento automático {0} de {1}: sin respuesta en estado {2}, se vuelve a publicar el último comando. |
| `ORQ-002` | Sin respuesta en estado {0} tras {1} reintentos automáticos; requiere revisión manual (POST /sagas/{id}/retry para forzar otro intento). |
| `ORQ-003` | El reintento automático {0} de {1} falló y se descartó: {2} |

## Cómo agregar un error

1. Añade la entrada al enum del servicio (`CodigoError`/`FirmaError`/`IdentidadError`) con su `<PREFIJO>-<HTTP>-<NNN>` único.
2. Añade el texto en el `mensajes.properties` del servicio (la clave no puede repetirse).
3. Lánzalo con `new ErrorNegocioException(CodigoError.X, params)` (o una excepción propia que extienda `ApiException`). No hace falta mapper.
