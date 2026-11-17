# Guía de pruebas manuales — flujo completo por API

JSON listo para pegar en `curl` o Postman (o en Swagger UI, solo en desarrollo), cubriendo los
7 servicios en el orden real en que se usan. **Asume un ambiente vacío** (recién
levantado, o recién reseteado con la sección 8) — las secciones están ordenadas
para que, leyéndolas de arriba hacia abajo, cada paso ya tenga lo que necesita del
paso anterior, sin saltos entre servicios. Si tu ambiente **ya tiene** una empresa
creada por otra persona/sesión, ve a la sección 2.1 en vez de repetir la 1.3.

## 0. Puertos y prefijos

| Servicio | Puerto | Prefijo |
|---|---|---|
| identidad-service | 8097 | `/identidad/api/v1` |
| firma-electronica-service | 8091 | `/empresas/api/v1` |
| inventario-service | 8092 | `/inventario/api/v1` |
| comprobantes-service | 8093 | `/comprobantes/api/v1` |
| sri-conector-service | 8094 | `/sri/api/v1` |
| notificacion-service | 8095 | `/notificaciones/api/v1` |
| facturacion-orchestrator-service | 8096 | `/sagas/api/v1` |

Swagger UI (`http://localhost:<puerto><prefijo>/q/swagger-ui`) solo existe al correr en modo desarrollo (`mvn quarkus:dev`) o en pruebas; en las imágenes Docker (perfil de producción) está apagado, y el documento OpenAPI también. Con las imágenes, usa `curl` o Postman con los ejemplos de abajo.

RUC de ejemplo usado en todos los bodies de abajo — no necesita existir de
antemano, lo crea el propio paso 1.3:

```
ruc: 1722039953001
razonSocial: ANCHUNDIA VALENCIA CARLOS EDUARDO
direccionMatriz: LLANO CHICO
ambiente: PRUEBAS
establecimiento: 001
puntoEmision: 001
```

---

## 1. Bootstrap — cuenta, empresa, api-key y certificado

El único camino obligatorio para poder emitir un comprobante. Cada paso depende
del anterior; en un ambiente vacío no hay atajos.

### 1.1 Crear usuario
`POST http://localhost:8097/identidad/api/v1/usuarios` (sin auth)
```json
{
  "email": "tu-correo@ejemplo.com",
  "password": "Passw0rd!123",
  "nombres": "Carlos",
  "apellidos": "Anchundia"
}
```

### 1.2 Login → token de sesión
`POST http://localhost:8097/identidad/api/v1/usuarios/login` (sin auth)
```json
{
  "email": "tu-correo@ejemplo.com",
  "password": "Passw0rd!123"
}
```
Respuesta: `{ "token": "<TOKEN_SESION>" }`. Se envía como `Authorization: Bearer <TOKEN_SESION>` (en Swagger,
solo en desarrollo, pégalo en el botón **Authorize** sin la palabra "Bearer").

### 1.3 Crear la empresa
`POST http://localhost:8091/empresas/api/v1` (auth: **token de sesión** del 1.2 — es
la única excepción del ecosistema que acepta sesión además de api-key, ver
ARQUITECTURA.md sección 8, "Bootstrap de una empresa nueva")
```json
{
  "ruc": "1722039953001",
  "razonSocial": "ANCHUNDIA VALENCIA CARLOS EDUARDO",
  "nombreComercial": "Prueba Manual",
  "direccionMatriz": "LLANO CHICO",
  "obligadoContabilidad": false,
  "contribuyenteEspecial": false,
  "esGranContribuyente": false,
  "ambiente": "PRUEBAS",
  "establecimientoDefecto": "001",
  "puntoEmisionDefecto": "001"
}
```
Al crearla: tu usuario queda como `ADMIN` de ese ruc automáticamente (ya puedes
pedir una api-key, paso 1.4 — no hace falta asociarte a mano), y se crea un
establecimiento MATRIZ + punto de emisión con los códigos por defecto (no hace
falta repetir 3.2/3.3 para el `001`/`001`). Guarda el `id` de la respuesta — es
el `{empresaId}` de la sección 3.

### 1.4 Crear api-key
`POST http://localhost:8097/identidad/api/v1/api-keys` (auth: token de sesión)

> El `rol` de la key **no** se manda en este request — se toma automáticamente
> de tu asociación usuario↔ruc ya existente (creada en 1.3, o en 2.1 si te
> asociaste a una empresa ajena), para que nadie pueda autoconcederse un rol
> mayor al que ya tiene sobre ese ruc.

Sin restricción de establecimiento/punto de emisión (uso normal):
```json
{
  "nombre": "prueba-manual",
  "ruc": "1722039953001",
  "expiracionEnDias": 365
}
```
Acotada a un establecimiento/punto de emisión específico (opcional):
```json
{
  "nombre": "prueba-manual-caja-1",
  "ruc": "1722039953001",
  "expiracionEnDias": 365,
  "establecimiento": "001",
  "puntoEmision": "001"
}
```
Respuesta: `{ "valor": "<API_KEY>", ... }`. **Este `valor` es el que se pega en
Authorize en los otros 6 servicios** (comprobantes, firma-electronica, inventario,
sri-conector, notificacion, facturacion-orchestrator) — es un JWT distinto al
token de sesión, no funciona contra `identidad-service` salvo `/api-keys`.

### 1.5 Subir el certificado de firma
`POST http://localhost:8091/empresas/api/v1/{empresaId}/certificado` (auth:
api-key) es `multipart/form-data` (campos: `archivo` el `.p12`, `contrasena` la
del certificado), no un body JSON. Con `curl`:
```
curl -X POST -H "Authorization: Bearer <API_KEY>" -F "archivo=@certificado.p12" -F "contrasena=<CLAVE>" \
  http://localhost:8091/empresas/api/v1/{empresaId}/certificado
```

**Obligatorio antes de emitir cualquier comprobante**: sin un certificado activo,
la saga se queda atascada en `FIRMADO` (o cae directo a `FIRMA_FALLIDA`) porque
`firma-electronica-service` no tiene con qué firmar el XML. Necesitas un
certificado de firma electrónica real (Security Data, BCE, etc.) — no hay forma
de fabricar uno de prueba. Cualquier certificado `ACTIVO` previo de la empresa
queda `REVOCADO` automáticamente al subir uno nuevo.

---

## 2. identidad-service — cuenta y equipo

Nada de esta sección es parte del camino obligatorio de la sección 1 — es para
gestionar tu cuenta, unirte a una empresa que ya existe, o administrar quién más
tiene acceso a la tuya.

### 2.1 Acceso a una empresa
No existe la autoasociación. El acceso a un ruc se obtiene de dos formas:

- **Si tú das de alta la empresa** (paso 1.3, con tu token de sesión), quedas como `ADMIN` de ella automáticamente.
- **Si la empresa ya existe**, un `ADMIN` de ese ruc te agrega con 2.4 (`POST /empresas/{ruc}/usuarios`, por tu correo y el rol).

Mientras no tengas acceso, pedir una api-key o gestionar el ruc responde `403`.

### 2.2 Listar / revocar api-keys
`GET http://localhost:8097/identidad/api/v1/api-keys` (auth: token de sesión)
`DELETE http://localhost:8097/identidad/api/v1/api-keys/{id}` (auth: token de sesión) — la revocación es **inmediata en todos los servicios** (se publica en `facturacion.apikey.revocaciones.v1`): la siguiente llamada con esa key responde `401` "La api-key fue revocada". Revocar dos veces la misma key responde igual (`204`).

### 2.3 Perfil y empresas asociadas
`GET http://localhost:8097/identidad/api/v1/usuarios/me` (auth: token de sesión) — datos del usuario autenticado.
`GET http://localhost:8097/identidad/api/v1/usuarios/me/empresas` (auth: token de sesión) — lista de `{ ruc, rol }` a las que está asociado.

`PATCH http://localhost:8097/identidad/api/v1/usuarios/me` (auth: token de sesión) — solo nombres/apellidos, el email de login no se cambia por aquí (PATCH porque no reemplaza el `Usuario` completo, solo el perfil):
```json
{ "nombres": "Carlos", "apellidos": "Anchundia Editado" }
```

`PATCH http://localhost:8097/identidad/api/v1/usuarios/me/password` (auth: token de sesión) — exige la contraseña actual, `401` si no coincide:
```json
{ "passwordActual": "Passw0rd!123", "passwordNueva": "NuevaPassw0rd!456" }
```

`DELETE http://localhost:8097/identidad/api/v1/usuarios/me/empresas/{ruc}` (auth: token de sesión) — abandonar un ruc al que tienes acceso; `404` si no estabas asociado, `409` si eres el único `ADMIN` que le queda.

### 2.4 Gestión de miembros de una empresa (solo ADMIN)
Estos endpoints son la única vía de acceso a una empresa que ya existe: un **ADMIN
existente** de ese ruc da acceso a un usuario ya registrado y controla quién más lo
tiene. No es un flujo de invitación/aprobación (no hay estado pendiente ni se notifica
a nadie). Quitar el acceso a un miembro, o cambiarle el rol, **revoca también sus
api-keys vigentes de ese ruc**. Ninguno de los 3 deja a un ruc sin ningún `ADMIN`
(`409` si lo intentas, incluso contigo mismo).

`POST http://localhost:8097/identidad/api/v1/empresas/{ruc}/usuarios` (auth: token de sesión, debes ser `ADMIN` de `{ruc}`) — da acceso a un usuario **ya registrado**, por su correo (`404` si no existe, `409` si ya tiene acceso):
```json
{ "email": "otra-persona@ejemplo.com", "rol": "OPERADOR" }
```

`GET http://localhost:8097/identidad/api/v1/empresas/{ruc}/usuarios` (auth: token de sesión, debes ser `ADMIN` de `{ruc}`) — lista `{ usuarioId, email, nombres, apellidos, rol, creadoEn }` de cada miembro.

`PATCH http://localhost:8097/identidad/api/v1/empresas/{ruc}/usuarios/{usuarioId}` (auth: token de sesión, debes ser `ADMIN` de `{ruc}`) — PATCH porque solo cambia el campo `rol` de la asociación, no la reemplaza entera:
```json
{ "rol": "OPERADOR" }
```

`DELETE http://localhost:8097/identidad/api/v1/empresas/{ruc}/usuarios/{usuarioId}` (auth: token de sesión, debes ser `ADMIN` de `{ruc}`) — revoca el acceso de ese usuario a `{ruc}` (sin body).

---

## 3. firma-electronica-service — gestión adicional de la empresa

Todo con la **api-key** del paso 1.4. Nada de esta sección es obligatorio para
emitir comprobantes (1.3 ya dejó la empresa, el establecimiento `001` y el punto
de emisión `001` listos) — es para ajustar la empresa, agregar sucursales, o
cambiar el certificado más adelante.

### 3.1 Actualizar empresa
`PUT http://localhost:8091/empresas/api/v1/{id}` (auth: api-key)
```json
{
  "razonSocial": "ANCHUNDIA VALENCIA CARLOS EDUARDO",
  "direccionMatriz": "LLANO CHICO",
  "obligadoContabilidad": false,
  "contribuyenteEspecial": false,
  "esGranContribuyente": false,
  "ambiente": "PRUEBAS",
  "establecimientoDefecto": "001",
  "puntoEmisionDefecto": "001",
  "activo": true
}
```
`GET http://localhost:8091/empresas/api/v1?limit=5` (auth: api-key) — listar, por si necesitas volver a buscar el `{id}` de tu empresa.

### 3.2 Establecimientos
`POST http://localhost:8091/empresas/api/v1/{empresaId}/establecimientos` (auth: api-key)
```json
{
  "codigo": "002",
  "direccion": "Sucursal Norte, Quito",
  "tipo": "SUCURSAL"
}
```
`PUT http://localhost:8091/empresas/api/v1/{empresaId}/establecimientos/{id}` (auth: api-key) — nota que `PUT` no lleva `codigo` (no se puede cambiar, solo se crea uno nuevo):
```json
{
  "direccion": "Sucursal Norte, Quito (reubicada)",
  "tipo": "SUCURSAL",
  "activo": true
}
```
`GET http://localhost:8091/empresas/api/v1/{empresaId}/establecimientos` — listar
`GET http://localhost:8091/empresas/api/v1/{empresaId}/establecimientos/{id}` — uno

### 3.3 Puntos de emisión
`POST http://localhost:8091/empresas/api/v1/{empresaId}/establecimientos/{establecimientoId}/puntos-emision` (auth: api-key)
```json
{
  "codigo": "002",
  "descripcion": "Caja 2"
}
```
`PUT http://localhost:8091/empresas/api/v1/{empresaId}/establecimientos/{establecimientoId}/puntos-emision/{id}` (auth: api-key):
```json
{
  "descripcion": "Caja 2 (reasignada a bodega)",
  "activo": true
}
```
`GET http://localhost:8091/empresas/api/v1/{empresaId}/establecimientos/{establecimientoId}/puntos-emision` — listar
`GET http://localhost:8091/empresas/api/v1/{empresaId}/establecimientos/{establecimientoId}/puntos-emision/{id}` — uno

### 3.4 Configuración por empresa (`configuracion_empresa`)
`POST http://localhost:8091/empresas/api/v1/{empresaId}/configuracion/{clave}` (auth: api-key) — crea la clave la primera vez; `409` si ya existía (usa `PATCH` para cambiarla):
```json
{ "valor": "#1A73E8" }
```
`PATCH http://localhost:8091/empresas/api/v1/{empresaId}/configuracion/{clave}` (auth: api-key) — cambia el `valor` de una clave que **ya existe**; `404` si todavía no se había creado (usa `POST` primero):
```json
{ "valor": "#00FF00" }
```

Las 7 claves con consumidor real hoy:

| Clave | Ejemplo de `valor` | Efecto |
|---|---|---|
| `ride.color` | `#1A73E8` | Barra de acento en el RIDE |
| `notificacion.asunto` | `Tu comprobante ya está listo` | Asunto del correo |
| `notificacion.remitente_nombre` | `Facturación Anchundia` | Nombre visible del remitente |
| `sri.recepcion.url` | *(url completa del WS)* | Override de la URL de recepción |
| `sri.autorizacion.url` | *(url completa del WS)* | Override de la URL de autorización |
| `sri.autorizacion.poll.max-intentos` | `40` | Máximo de intentos del poller por empresa |
| `sri.proveedor.ruc` | `1719369561001` | RUC del proveedor de software (Anexo 26) |

`GET .../configuracion` — listar todas
`GET .../configuracion/{clave}` — una
`DELETE .../configuracion/{clave}` — volver al default

### 3.5 Certificado — consultar / revocar
> Un certificado vencido no firma: al subir un `.p12` ya vencido responde `400`; si vence con la empresa en uso, pasa a `EXPIRADO` (cada hora y al intentar firmar) y la saga cae en `FIRMA_FALLIDA` con el motivo "venció el …". Se corrige subiendo uno vigente.

`GET http://localhost:8091/empresas/api/v1/{empresaId}/certificado` (auth: api-key)
— metadata del certificado activo (emisor, vigencia, etc.); nunca devuelve el
contenido del `.p12` ni la contraseña. `404` si la empresa no tiene uno activo.

`DELETE http://localhost:8091/empresas/api/v1/{empresaId}/certificado` (auth: api-key)
— revoca el certificado activo **sin** subir uno nuevo en su lugar (ej.
certificado comprometido); `POST .../certificado` (paso 1.5) ya revoca
implícitamente el anterior al reemplazarlo, este endpoint es para cuando no hay
reemplazo todavía. `404` si no tenía uno activo. ⚠️ Úsalo con cuidado sobre una
empresa que ya está emitiendo comprobantes reales — sin certificado activo,
firma-electronica-service no puede firmar el siguiente XML.

### 3.6 Logo de la empresa (para el RIDE)
`POST http://localhost:8091/empresas/api/v1/{empresaId}/logo` (auth: api-key `ADMIN`) es `multipart/form-data`
(campo `archivo`: imagen PNG o JPEG, máx. 200 KB); responde `201` con `{ "tipo": "image/png", "tamanoBytes": 4244 }` y `Location` al `GET` del mismo recurso. Con `curl`:
```
curl -X POST -H "Authorization: Bearer <API_KEY>" -F "archivo=@logo.png;type=image/png" \
  http://localhost:8091/empresas/api/v1/{empresaId}/logo
```
`GET .../{empresaId}/logo` devuelve la imagen; `DELETE .../{empresaId}/logo` la quita. Se guarda en la tabla `empresa`
y aparece en el recuadro del emisor del RIDE de los comprobantes que se **autoricen después** de subirla; `400` si no es
PNG/JPEG o pesa más de 200 KB.


---

## 4. inventario-service — catálogo y kardex

Todo con la **api-key**.

### 4.1 Categoría (opcional)
Las categorías son **de cada empresa**: se crean con el RUC de la api-key, el listado solo trae las de esa empresa, y
una categoría de otra empresa responde `404` (también al asignarla a un ítem). Crear o modificar exige rol
`ADMIN`/`OPERADOR`.

`POST http://localhost:8092/inventario/api/v1/categorias`
```json
{
  "nombre": "Electrónica",
  "tipo": "PRODUCTO",
  "descripcion": "Dispositivos electrónicos"
}
```
`PUT http://localhost:8092/inventario/api/v1/categorias/{id}`:
```json
{
  "nombre": "Electrónica y tecnología",
  "tipo": "AMBOS",
  "descripcion": "Dispositivos electrónicos y accesorios",
  "activa": true
}
```

### 4.2 Ítem — producto con stock inicial
`POST http://localhost:8092/inventario/api/v1/items`
```json
{
  "ruc": "1722039953001",
  "tipo": "PRODUCTO",
  "codigoPrincipal": "TABLET02",
  "codigoAuxiliar": "TAB-02",
  "descripcion": "Tablet 10 pulgadas",
  "precioUnitario": 300.00,
  "unidadMedida": "unidad",
  "cantidadInicial": 20,
  "costoInicial": 220.00,
  "stockMinimo": 5,
  "codigoImpuesto": "2",
  "codigoPorcentaje": "4",
  "tieneIce": false
}
```
Guarda el `id` de la respuesta — es el `itemId` opcional de la sección 5.

### 4.3 Ítem — servicio (sin kardex)
```json
{
  "ruc": "1722039953001",
  "tipo": "SERVICIO",
  "codigoPrincipal": "SERV01",
  "descripcion": "Instalación y configuración",
  "precioUnitario": 50.00,
  "codigoImpuesto": "2",
  "codigoPorcentaje": "4"
}
```

> **Tarifa de IVA:** el ítem guarda solo `codigoImpuesto` + `codigoPorcentaje` (Tabla 16/17). La tarifa no se manda ni se guarda
> ni aparece en la respuesta: se resuelve al **facturar**, desde el catálogo de `comprobantes-service` (sección 9). Un
> `codigoPorcentaje` inexistente se detecta al emitir la factura, no al crear el ítem. `tieneIce=true` exige
> `codigoPorcentajeIce` y `tarifaIce`.

### 4.4 Actualizar un ítem
`PUT http://localhost:8092/inventario/api/v1/items/{id}`
```json
{
  "categoriaId": "{categoriaId}",
  "descripcion": "Tablet 10 pulgadas (modelo actualizado)",
  "precioUnitario": 320.00,
  "unidadMedida": "unidad",
  "stockMinimo": 5,
  "activo": true,
  "codigoImpuesto": "2",
  "codigoPorcentaje": "4",
  "tieneIce": false
}
```
> No permite tocar existencias directamente (`cantidadInicial`/stock actual) —
> eso solo cambia vía kardex (4.6/4.7). `categoriaId` es opcional.

### 4.5 Imágenes de un ítem
`POST http://localhost:8092/inventario/api/v1/items/{id}/imagenes` es
`multipart/form-data` (campos: `archivo` la imagen JPEG/PNG/WEBP/GIF máx. 5 MB,
`esPrincipal` boolean, `orden` entero), no un body JSON. Con `curl`:
`curl -X POST -H "Authorization: Bearer <API_KEY>" -F "archivo=@foto.png" -F "esPrincipal=true" -F "orden=0" .../items/{id}/imagenes`.
Si `esPrincipal=true`, desmarca cualquier otra imagen principal del mismo ítem.

`GET http://localhost:8092/inventario/api/v1/items/{id}/imagenes` — listar (solo metadata)
`GET http://localhost:8092/inventario/api/v1/imagenes/{id}/contenido` — descargar los bytes
`DELETE http://localhost:8092/inventario/api/v1/imagenes/{id}` — eliminar

### 4.6 Entrada de kardex (reposición de stock)
`POST http://localhost:8092/inventario/api/v1/items/{id}/kardex/entradas`
```json
{
  "cantidad": 10,
  "costoUnitario": 225.00,
  "referenciaDocumento": "FAC-PROVEEDOR-001",
  "motivo": "Reposición de stock"
}
```

### 4.7 Ajuste de kardex (conteo físico / merma)
`POST http://localhost:8092/inventario/api/v1/items/{id}/kardex/ajustes`
```json
{
  "cantidad": 2,
  "positivo": false,
  "motivo": "Merma detectada en conteo físico"
}
```

### 4.8 Consultas
`GET .../items?tipo=PRODUCTO&limit=20&page=0&categoriaId={categoriaId}&ruc=1722039953001` — listar (todos los filtros son opcionales)
`GET .../items/{id}` — uno
`GET .../items/{id}/kardex?limit=20&page=0` — movimientos
`GET .../categorias?limit=20&page=0` — listar categorías
`GET .../categorias/{id}` — una categoría

---

## 5. comprobantes-service — los 6 tipos de comprobante

Todo con la **api-key** (si la sacaste con `establecimiento`/`puntoEmision`, deben
coincidir exactamente con los del bloque `empresa` de cada request, o da 403).
Sustituye `{itemId}` por el `id` del paso 4.2 si quieres que el impuesto se derive
solo del catálogo — si prefieres no depender de inventario-service, quita
`itemId` y manda el bloque `impuestos` a mano (ver el comentario en cada ejemplo).

### 5.1 Factura

> **Descuento de inventario (bandera `INVENTARIO_DESCUENTO_HABILITADO` del orquestador, por defecto `true`).** Con la
> bandera encendida, una **factura** cuyas líneas llevan `itemId` de un `PRODUCTO` reserva su stock antes de firmar: si no
> alcanza, el comprobante queda en `STOCK_INSUFICIENTE` (el `motivoRechazo` lista los ítems) y no se firma ni se envía al
> SRI; si el SRI autoriza se descuenta del kardex, y si rechaza (o la firma falla) la reserva se libera. Por eso el
> producto debe tener existencias (`cantidadInicial` o una entrada de kardex, 4.6). Los `SERVICIO` y las líneas sin
> `itemId` no descuentan. Con la bandera en `false` el inventario no interviene.

`POST http://localhost:8093/comprobantes/api/v1/facturas`
```json
{
  "empresa": {
    "ruc": "1722039953001",
    "razonSocial": "ANCHUNDIA VALENCIA CARLOS EDUARDO",
    "direccionMatriz": "LLANO CHICO",
    "ambiente": "PRUEBAS",
    "establecimiento": "001",
    "puntoEmision": "001"
  },
  "compradorTipoIdentificacion": "05",
  "compradorIdentificacion": "1710034065",
  "compradorRazonSocial": "Consumidor de Prueba",
  "compradorDireccion": "Quito",
  "compradorCorreo": "cliente@ejemplo.com",
  "moneda": "DOLAR",
  "detalles": [
    {
      "itemId": "{itemId}",
      "codigoPrincipal": "TABLET02",
      "descripcion": "Tablet 10 pulgadas",
      "cantidad": 1,
      "precioUnitario": 300.00
    }
  ]
}
```
Detalle sin `itemId` (impuesto manual, no depende de inventario-service):
```json
{
  "codigoPrincipal": "TABLET02",
  "descripcion": "Tablet 10 pulgadas",
  "cantidad": 1,
  "precioUnitario": 300.00,
  "impuestos": [
    { "codigo": "2", "codigoPorcentaje": "4", "tarifa": 15.00 }
  ]
}
```

#### Formas de pago (`pagos`) — Factura, Nota de débito y Liquidación de compra

Opcional. Si no mandas `pagos`, se asume un único pago `01` por el importe total. Si los mandas, se validan
(400 con el motivo si algo falla) — ver ARQUITECTURA.md sección 7.4:

- `formaPago`: Tabla 24 del SRI — `01` sin utilización del sistema financiero, `15` compensación de deudas,
  `16` tarjeta de débito, `17` dinero electrónico, `18` tarjeta prepago, `19` tarjeta de crédito,
  `20` otros con utilización del sistema financiero, `21` endoso de títulos. Cualquier otro código se rechaza.
- La **suma de `total`** debe ser igual al `importeTotal` del comprobante (con la factura de arriba: 300.00 + 15% IVA = 345.00).
- `plazo` y `unidadTiempo` van juntos o ninguno (pago a crédito).

Dos pagos, uno de ellos a crédito:
```json
"pagos": [
  { "formaPago": "01", "total": 45.00 },
  { "formaPago": "19", "total": 300.00, "plazo": "30", "unidadTiempo": "dias" }
]
```

Factura comercial negociable (Anexo 11): forma de pago `21`, con `plazo`, `unidadTiempo` y `compradorDireccion`:
```json
"pagos": [
  { "formaPago": "21", "total": 345.00, "plazo": "30", "unidadTiempo": "dias" }
]
```

Casos que responden **400** (útiles para probar las validaciones):

| Request | Motivo |
|---|---|
| `{ "formaPago": "05", "total": 345.00 }` | `formaPago` fuera de la Tabla 24 |
| `{ "formaPago": "01", "total": 100.00 }` | la suma de pagos (100.00) no coincide con el importe total (345.00) |
| `{ "formaPago": "19", "total": 345.00, "plazo": "30" }` | `plazo` sin `unidadTiempo` |
| `{ "formaPago": "19", "total": 345.00, "plazo": "abc", "unidadTiempo": "dias" }` | `plazo` no numérico |
| `{ "formaPago": "21", "total": 345.00 }` | forma `21` sin `plazo`/`unidadTiempo` (Anexo 11) |

En **Comprobante de retención** el pago va dentro de cada `documentosSustento[]` (ver 5.5) y solo admite
`formaPago` y `total`: mandar `plazo`/`unidadTiempo` también responde 400.

Para ver cómo quedaron guardados:
```sql
select p.orden, p.forma_pago, fp.descripcion, p.total, p.plazo, p.unidad_tiempo
from comprobantes.comprobante_pago p
join comprobantes.forma_pago fp on fp.codigo = p.forma_pago
where p.comprobante_id = '{comprobanteId}' order by p.orden;
```

### 5.2 Nota de crédito
`POST http://localhost:8093/comprobantes/api/v1/notas-credito`
```json
{
  "empresa": {
    "ruc": "1722039953001",
    "razonSocial": "ANCHUNDIA VALENCIA CARLOS EDUARDO",
    "direccionMatriz": "LLANO CHICO",
    "ambiente": "PRUEBAS",
    "establecimiento": "001",
    "puntoEmision": "001"
  },
  "compradorTipoIdentificacion": "05",
  "compradorIdentificacion": "1710034065",
  "compradorRazonSocial": "Consumidor de Prueba",
  "compradorDireccion": "Quito",
  "docModificadoTipo": "01",
  "docModificadoNumero": "001-001-000000009",
  "docModificadoFecha": "29/07/2026",
  "motivo": "DEVOLUCION DE MERCADERIA",
  "moneda": "DOLAR",
  "detalles": [
    {
      "codigoPrincipal": "TABLET02",
      "descripcion": "Tablet 10 pulgadas",
      "cantidad": 1,
      "precioUnitario": 300.00,
      "impuestos": [
        { "codigo": "2", "codigoPorcentaje": "4", "tarifa": 15.00 }
      ]
    }
  ]
}
```

### 5.3 Nota de débito
`POST http://localhost:8093/comprobantes/api/v1/notas-debito`
```json
{
  "empresa": {
    "ruc": "1722039953001",
    "razonSocial": "ANCHUNDIA VALENCIA CARLOS EDUARDO",
    "direccionMatriz": "LLANO CHICO",
    "ambiente": "PRUEBAS",
    "establecimiento": "001",
    "puntoEmision": "001"
  },
  "compradorTipoIdentificacion": "05",
  "compradorIdentificacion": "1710034065",
  "compradorRazonSocial": "Consumidor de Prueba",
  "docModificadoTipo": "01",
  "docModificadoNumero": "001-001-000000009",
  "docModificadoFecha": "29/07/2026",
  "moneda": "DOLAR",
  "motivos": [
    { "razon": "Interés por mora", "valor": 15.00 }
  ],
  "impuestos": [
    { "codigo": "2", "codigoPorcentaje": "4", "tarifa": 15.00 }
  ]
}
```

### 5.4 Guía de remisión
`POST http://localhost:8093/comprobantes/api/v1/guias-remision`
```json
{
  "empresa": {
    "ruc": "1722039953001",
    "razonSocial": "ANCHUNDIA VALENCIA CARLOS EDUARDO",
    "direccionMatriz": "LLANO CHICO",
    "ambiente": "PRUEBAS",
    "establecimiento": "001",
    "puntoEmision": "001"
  },
  "dirPartida": "LLANO CHICO, Quito",
  "transportistaRazonSocial": "Transportes de Prueba SA",
  "transportistaTipoIdentificacion": "04",
  "transportistaRuc": "1790000000001",
  "fechaIniTransporte": "29/07/2026",
  "fechaFinTransporte": "29/07/2026",
  "placa": "PBX1234",
  "destinatarios": [
    {
      "identificacion": "1710034065",
      "razonSocial": "Consumidor de Prueba",
      "direccion": "Av. Amazonas y Naciones Unidas, Quito",
      "motivoTraslado": "Venta de mercadería",
      "detalles": [
        {
          "codigoPrincipal": "TABLET02",
          "descripcion": "Tablet 10 pulgadas",
          "cantidad": 1
        }
      ]
    }
  ]
}
```

### 5.5 Comprobante de retención (XSD v2.0.0, estilo ATS)
`POST http://localhost:8093/comprobantes/api/v1/retenciones`
```json
{
  "empresa": {
    "ruc": "1722039953001",
    "razonSocial": "ANCHUNDIA VALENCIA CARLOS EDUARDO",
    "direccionMatriz": "LLANO CHICO",
    "ambiente": "PRUEBAS",
    "establecimiento": "001",
    "puntoEmision": "001"
  },
  "sujetoRetenidoTipoIdentificacion": "04",
  "sujetoRetenidoIdentificacion": "1790000000001",
  "sujetoRetenidoRazonSocial": "Proveedor de Prueba SA",
  "periodoFiscal": "07/2026",
  "parteRelacionada": "NO",
  "documentosSustento": [
    {
      "codSustento": "01",
      "docSustentoCodigo": "01",
      "docSustentoNumero": "001001000000123",
      "docSustentoFecha": "20/07/2026",
      "pagoLocExt": "01",
      "totalSinImpuestos": 300.00,
      "importeTotal": 345.00,
      "impuestos": [
        { "codigo": "2", "codigoPorcentaje": "4", "baseImponible": 300.00, "tarifa": 15.00, "valor": 45.00 }
      ],
      "retenciones": [
        {
          "codigo": "2",
          "codigoRetencion": "9",
          "baseImponible": 300.00,
          "porcentajeRetener": 10,
          "valorRetenido": 30.00
        }
      ],
      "pagos": [
        { "formaPago": "01", "total": 345.00 }
      ]
    }
  ]
}
```
> `documentosSustento` agrupa, por cada documento retenido, sus propias líneas de `retenciones` junto con el impuesto (`impuestos` = IVA/ICE que ya traía ESE documento, no el valor retenido) y la forma de pago (`pagos`) de ese mismo documento — puede haber más de un `documentoSustento` por comprobante. `docSustentoNumero` (XML: `numDocSustento`) debe tener exactamente 15 dígitos, sin guiones (el XSD exige el patrón `[0-9]{15}`): es `estab`+`ptoEmi`+`secuencial` concatenados, ej. `001001000000123`. `pagos` es opcional (si no se manda, se asume "01" por el `importeTotal` del documento); `parteRelacionada` es opcional (default `"NO"`).

### 5.6 Liquidación de compra
`POST http://localhost:8093/comprobantes/api/v1/liquidaciones-compra`
```json
{
  "empresa": {
    "ruc": "1722039953001",
    "razonSocial": "ANCHUNDIA VALENCIA CARLOS EDUARDO",
    "direccionMatriz": "LLANO CHICO",
    "ambiente": "PRUEBAS",
    "establecimiento": "001",
    "puntoEmision": "001"
  },
  "proveedorTipoIdentificacion": "05",
  "proveedorIdentificacion": "1710034065",
  "proveedorRazonSocial": "Persona Natural Proveedora",
  "moneda": "DOLAR",
  "detalles": [
    {
      "codigoPrincipal": "SERV01",
      "descripcion": "Servicio de mantenimiento",
      "cantidad": 1,
      "precioUnitario": 50.00,
      "impuestos": [
        { "codigo": "2", "codigoPorcentaje": "4", "tarifa": 15.00 }
      ]
    }
  ]
}
```

### 5.7 Consultas
`GET http://localhost:8093/comprobantes/api/v1/comprobantes/{id}` — el comprobante
`GET http://localhost:8093/comprobantes/api/v1/comprobantes?limit=20&page=0&estado=AUTORIZADO&empresaRuc=1722039953001` — listar (filtros opcionales)
`GET http://localhost:8093/comprobantes/api/v1/comprobantes/{id}/xml?tipo=generado` — XML; `tipo` = `generado` (default, sin firmar, siempre existe) | `firmado` | `autorizado` — 404 si esa etapa todavía no ocurrió
`GET http://localhost:8093/comprobantes/api/v1/comprobantes/{id}/ride` — RIDE en PDF (solo si ya está `AUTORIZADO`)

---

## 6. Observabilidad — solo lectura (api-key sin alcance)

La saga tarda unos segundos en mover el comprobante de `BORRADOR` → `FIRMADO` →
`AUTORIZADO` (la autorización real del SRI puede tardar hasta ~30-60s más por el
poller). Usa estos endpoints para seguirla sin volver a tocar comprobantes-service.

### 6.1 sri-conector-service
`GET http://localhost:8094/sri/api/v1/envios/comprobante/{comprobanteId}` — estado del envío/autorización
`GET http://localhost:8094/sri/api/v1/envios/clave/{claveAcceso}` — lo mismo, por clave de acceso
`GET http://localhost:8094/sri/api/v1/envios/comprobante/{comprobanteId}/xml?tipo=firmado` — `tipo` = `firmado` (default, lo que se mandó a Recepción) | `autorizado` (el bloque `<autorizacion>` completo del SRI)

### 6.2 notificacion-service
`GET http://localhost:8095/notificaciones/api/v1/notificaciones/comprobante/{comprobanteId}`

### 6.3 facturacion-orchestrator-service
`GET http://localhost:8096/sagas/api/v1/{comprobanteId}` — estado y línea de tiempo completa de la saga
`GET http://localhost:8096/sagas/api/v1?limit=20&page=0&state=FAILED` — listar (filtro opcional por estado)
`POST http://localhost:8096/sagas/api/v1/{comprobanteId}/retry` — reintentar una saga atascada (sin body)
`POST http://localhost:8096/sagas/api/v1/reenviar-notificaciones?limite=100` — reenvío masivo de correos (api-key de escritura, sin body). Toma las sagas **de la empresa de la api-key** que están en `NOTIFICACION_FALLIDA` (hasta `limite`, de 1 a 500; por defecto 100), las devuelve a `AUTORIZADO` y le pide a comprobantes-service que vuelva a publicar cada comprobante autorizado. Responde `202` con `{ "solicitados": N }`: es asíncrono, así que `N` es cuántas se mandaron a reenviar, no cuántos correos salieron. Unos segundos después, cada saga está en `NOTIFICADO` (el envío salió bien) o de nuevo en `NOTIFICACION_FALLIDA` con el motivo nuevo. Una segunda llamada inmediata responde `{ "solicitados": 0 }` (ya no están en `NOTIFICACION_FALLIDA`). Si hay más sagas que `limite`, repite la llamada. Para un solo comprobante: `POST http://localhost:8093/comprobantes/api/v1/comprobantes/{comprobanteId}/reenviar-correo` (`202`; `409` si no está autorizado o no tiene correo del receptor).

---

## 7. Orden recomendado para una corrida completa

Las secciones ya están en el orden en que se necesitan — esto es solo el
resumen del camino mínimo, con los pasos opcionales marcados:

1. **1.1 → 1.2 → 1.3 → 1.4 → 1.5** (bootstrap): usuario, sesión, empresa, api-key, certificado. *(Si la empresa ya existe, usa 2.1 en vez de 1.3.)*
2. *(Opcional)* **4.1 → 4.2** (inventario-service): categoría e ítem — guarda el `id` del ítem.
3. **5.1** (comprobantes-service): crear una factura (con o sin `itemId`).
4. Espera ~15-20s y consulta **5.7** (`GET .../comprobantes/{id}`) hasta ver
   `"estado": "AUTORIZADO"` — si se queda en `FIRMADO` más de un minuto, revisa
   **6.3** (`GET /sagas/api/v1/{comprobanteId}`) para ver en qué paso está atascada
   (la causa más común: falta el paso 1.5, sin certificado activo).
5. **5.7** `GET .../ride` para bajar el PDF ya autorizado.
6. Repite 5.2–5.6 para probar los otros 5 tipos de comprobante.

La sección 2 (cuenta/equipo) y la sección 3 (establecimientos/puntos de
emisión/configuración adicionales) son extras — ninguna es parte de este
camino mínimo.

---

## 8. Reset de datos de prueba (SQL)

> Ninguno de los endpoints agregados en esta guía (2.3 autogestión, 2.4
> gestión de miembros, 3.5 revocar certificado, 4.1/4.4 actualizar
> categoría/ítem, 3.4 configuración con POST+PATCH) agrega tablas nuevas —
> todos reutilizan tablas ya cubiertas en los dos scripts de abajo (8.1 las
> borra, 8.2 las conserva salvo que se indique lo contrario). No hace falta
> ningún script adicional para probar nada de esto desde cero.

Una sola base de datos ("facturacion"), un esquema Postgres por servicio
(`inventario`, `firma_electronica`, `comprobantes`, `sri`, `notificacion`,
`orquestacion`, `identidad` — ver ARQUITECTURA.md sección 5) y **cero FKs entre
esquemas** (cada servicio es dueño del suyo) — así que el orden solo importa
**dentro** de cada esquema, nunca entre esquemas. Los scripts de abajo borran
en orden hijo→padre para no toparse con violaciones de constraint. Ninguno
toca `flyway_schema_history` (es el propio control de versión de Flyway —
borrar de ahí rompe el arranque del servicio en el siguiente `docker compose up`).

Ejecución (contenedor real de este proyecto, ajusta el nombre si tu `docker
compose` usa otro prefijo):
```bash
docker exec -i facturacion-db \
  psql -U postgres -d facturacion <<'EOF'
-- (pega aquí uno de los dos bloques de abajo)
EOF
```
O interactivo: `docker exec -it facturacion-db psql -U postgres -d facturacion`, y pega el bloque directo en la sesión.

> **Nota sobre `secuencial_comprobante`**: a propósito **no** se borra en
> ningún script de abajo. El SRI real (`celcer.sri.gob.ec`, ambiente de
> pruebas) recuerda los secuenciales ya registrados aunque tu Postgres local
> se reinicie — si reseteas este contador a 0 y vuelves a emitir desde el
> secuencial 1, el SRI real rechaza cada comprobante con
> `ERROR SECUENCIAL REGISTRADO` (ya visto en este proyecto). Déjalo intacto
> salvo que también estés apuntando a un ambiente SRI nuevo/limpio.

### 8.1 Reset completo (todos los esquemas — incluye usuarios, empresas, api-keys)
Deja la base literalmente vacía — después de correrlo tienes que rehacer el
bootstrap completo (sección 1) para poder seguir probando.
```sql
begin;

-- comprobantes (hijos antes que padre; forma_pago es catálogo del SRI: NO se borra)
delete from comprobantes.detalle_impuesto;
delete from comprobantes.comprobante_pago;
delete from comprobantes.detalle_comprobante;
delete from comprobantes.destinatario_guia_remision;
delete from comprobantes.comprobante;
delete from comprobantes.outbox_event;
delete from comprobantes.processed_message;
delete from comprobantes.item_local;
delete from comprobantes.establecimiento_local;
delete from comprobantes.punto_emision_local;
delete from comprobantes.configuracion_empresa_local;
-- delete from comprobantes.secuencial_comprobante; -- ver nota de arriba

-- sri (sin FK)
delete from sri.envio_sri;
delete from sri.configuracion_empresa_local;
delete from sri.outbox_event;
delete from sri.processed_message;

-- notificacion (sin FK)
delete from notificacion.notificacion_enviada;
delete from notificacion.configuracion_empresa_local;
delete from notificacion.outbox_event;
delete from notificacion.processed_message;

-- orquestacion (saga_step antes que saga_instance)
delete from orquestacion.saga_step;
delete from orquestacion.saga_instance;
delete from orquestacion.outbox_event;
delete from orquestacion.processed_message;

-- inventario (hijos de item antes que item; item antes que categoria)
delete from inventario.imagen_item;
delete from inventario.movimiento_kardex;
delete from inventario.reserva_stock;
delete from inventario.item;
delete from inventario.categoria;
delete from inventario.outbox_event;
delete from inventario.processed_message;

-- firma_electronica (hijos de empresa antes que empresa; punto_emision antes que establecimiento)
delete from firma_electronica.certificado_digital;
delete from firma_electronica.configuracion_empresa;
delete from firma_electronica.punto_emision;
delete from firma_electronica.establecimiento;
delete from firma_electronica.empresa;
delete from firma_electronica.outbox_event;
delete from firma_electronica.processed_message;

-- identidad (hijos de usuario antes que usuario; las réplicas *_local no tienen FK)
delete from identidad.api_key;
delete from identidad.usuario_empresa_rol;
delete from identidad.usuario;
delete from identidad.empresa_local;
delete from identidad.establecimiento_local;
delete from identidad.punto_emision_local;

commit;
```

### 8.2 Reset solo de datos transaccionales (mantiene usuarios/empresas/api-keys)
Para repetir pruebas de comprobantes/inventario/saga sin rehacer el bootstrap
de `identidad`/`firma_electronica` cada vez — conserva tu usuario, tu empresa,
sus establecimientos/puntos de emisión, el certificado y las api-keys ya
emitidas.
```sql
begin;

delete from comprobantes.detalle_impuesto;
delete from comprobantes.comprobante_pago;
delete from comprobantes.detalle_comprobante;
delete from comprobantes.destinatario_guia_remision;
delete from comprobantes.comprobante;
delete from comprobantes.outbox_event;
delete from comprobantes.processed_message;
delete from comprobantes.item_local;
delete from comprobantes.establecimiento_local;
delete from comprobantes.punto_emision_local;
delete from comprobantes.configuracion_empresa_local;
-- delete from comprobantes.secuencial_comprobante; -- ver nota de arriba

delete from sri.envio_sri;
delete from sri.configuracion_empresa_local;
delete from sri.outbox_event;
delete from sri.processed_message;

delete from notificacion.notificacion_enviada;
delete from notificacion.configuracion_empresa_local;
delete from notificacion.outbox_event;
delete from notificacion.processed_message;

delete from orquestacion.saga_step;
delete from orquestacion.saga_instance;
delete from orquestacion.outbox_event;
delete from orquestacion.processed_message;

delete from inventario.imagen_item;
delete from inventario.movimiento_kardex;
delete from inventario.reserva_stock;
delete from inventario.item;
delete from inventario.categoria;
delete from inventario.outbox_event;
delete from inventario.processed_message;

commit;
```
> Tras 8.2, `inventario.item`/`categoria` también quedan vacíos (están en el
> bloque de arriba), así que las réplicas locales de ítem en `comprobantes`
> (`item_local`) quedan vacías y se repueblan solas apenas recrees ítems con
> 4.2 (el evento `ItemRegistrado` nuevo las llena de nuevo, sin ningún paso
> manual). `establecimiento_local`/`punto_emision_local`/
> `configuracion_empresa_local` (en `comprobantes`, `sri` y `notificacion`)
> quedan vacías pero su fuente (`firma_electronica.establecimiento`/
> `punto_emision`/`configuracion_empresa`) **no** se tocó en 8.2 — solo se
> repueblan si vuelves a hacer un `PUT`/`POST`/`PATCH` sobre esos recursos en
> firma-electronica-service (3.2/3.3/3.4), no automáticamente.

---

## 9. Catálogos del SRI (comprobantes-service)

Los catálogos de la ficha técnica (formas de pago, impuestos y tarifas de IVA, tipos de identificación, retenciones,
sustento) son tablas de `comprobantes-service`, sembradas con la ficha v2.34 (ver ARQUITECTURA.md 7.6). Todo requiere
api-key; **escribir además exige el header `X-Catalogos-Admin-Token`** (variable `CATALOGOS_ADMIN_TOKEN` del servicio;
sin ella la escritura responde 403). Un cambio rige desde la siguiente emisión, sin redesplegar.

### 9.1 Consultar
```
GET http://localhost:8093/comprobantes/api/v1/catalogos                                  # lista de catálogos
GET http://localhost:8093/comprobantes/api/v1/catalogos/forma_pago
GET http://localhost:8093/comprobantes/api/v1/catalogos/tarifa_impuesto?vigentes=true
GET http://localhost:8093/comprobantes/api/v1/catalogos/retencion?vigentes=true&fecha=2023-03-01
```
Catálogos: `impuesto`, `tarifa_impuesto`, `forma_pago`, `tipo_identificacion`, `impuesto_retencion`, `retencion`, `sustento_tributario`.

### 9.2 Cambiar una tarifa (una sola vez, sin tocar ítems ni código)
Por ejemplo, cerrar la vigencia del IVA 14% (código `3`):
`PUT http://localhost:8093/comprobantes/api/v1/catalogos/tarifa_impuesto` con `X-Catalogos-Admin-Token: <token>`
```json
{
  "impuesto_codigo": "2",
  "codigo_porcentaje": "3",
  "descripcion": "IVA 14%",
  "tarifa": 14.00,
  "vigente_hasta": "2017-05-31"
}
```
Desde la siguiente factura (no hay caché: cada emisión lee la tabla) ese código deja de aceptarse para fechas posteriores a `vigente_hasta`.
Dar de baja un registro: mandar `"activo": false` (nunca se borra).

### 9.3 Cargar los catálogos que faltan (ATS)
Los códigos de retención de **Renta** y los `codSustento` vienen del Catálogo del ATS del SRI (no están en `documentacion/`); las tablas
existen vacías y mientras lo estén solo se valida el formato. Una vez cargadas, la validación pasa a ser estricta:
```json
{ "impuesto_codigo": "1", "codigo": "312", "descripcion": "Transferencia de bienes muebles de naturaleza corporal", "porcentaje": 1.75 }
```
`PUT /catalogos/retencion` (y `PUT /catalogos/sustento_tributario` con `{ "codigo": "01", "descripcion": "..." }`).

### 9.4 Validaciones que verás en 400
- `formaPago` fuera de la Tabla 24 o no vigente · `codigoPorcentaje` inexistente/vencido · tarifa del cliente distinta a la del catálogo.
- `compradorTipoIdentificacion` fuera de la Tabla 6; `07` (consumidor final) en nota de crédito/débito, retención o liquidación; `07` sin `9999999999999`;
  RUC (`04`) ≠ 13 dígitos; cédula (`05`) ≠ 10 dígitos.
- Retención: `codigoRetencion` inexistente/no vigente para el impuesto, `porcentajeRetener` distinto al del catálogo, `valorRetenido` ≠ `baseImponible × porcentaje`.
- Ítem dado de baja (`activo=false`) al facturar por `itemId`.

### 9.5 Formato de los errores de negocio
Los errores de las reglas de negocio traen un `codigo` estable (el texto puede cambiar, el código no) y, si agrupan varios problemas, `details`:
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request",
  "message": "La suma de los pagos (100.00) no coincide con el importe total del comprobante (345.00).",
  "codigo": "COMP-400-108" }
```
Prefijos: `COMP-` comprobantes, `INV-` inventario, `ORQ-` orquestador. Los textos están en `mensajes.properties` de cada servicio.

---

## 10. Sagas atascadas — watchdog (facturacion-orchestrator-service)

Una saga que espera la respuesta de firma (`CREADO`) o del SRI (`FIRMADO`) y no la recibe se reintenta sola: tras el plazo
(2 min firma / 15 min SRI) vuelve a publicar su último comando; tras 3 reintentos pasa a **`EXPIRADA`**.
```
GET  http://localhost:8096/sagas/api/v1?state=EXPIRADA        # sagas que requieren revisión
GET  http://localhost:8096/sagas/api/v1/{comprobanteId}       # incluye "reintentos" y el detalle de pasos (status NOTA = reintento/expiración)
POST http://localhost:8096/sagas/api/v1/{comprobanteId}/retry # reintento manual: vuelve a publicar el último comando
```
Ajustes (variables de entorno del servicio): `SAGA_WATCHDOG_HABILITADO`, `SAGA_WATCHDOG_INTERVALO` (30s), `SAGA_WATCHDOG_PLAZO_FIRMA` (PT2M),
`SAGA_WATCHDOG_PLAZO_SRI` (PT15M), `SAGA_WATCHDOG_MAX_REINTENTOS` (3). Ver ARQUITECTURA.md 4.1.

Otros estados terminales que verás en `GET /sagas`: `AUTORIZADO_SIN_NOTIFICACION` (el comprobante se autorizó pero no tiene correo del
receptor, así que no habrá notificación) `STOCK_NO_DISPONIBLE` (con el descuento de inventario habilitado, no había stock: el comprobante queda en `STOCK_INSUFICIENTE`) y `EXPIRADA` (el watchdog agotó los reintentos).
Cada saga se reintenta en su propia transacción: si una falla, queda una nota `ORQ-402` en su bitácora y cuenta como intento, sin afectar a las demás.

### 10.1 Reenvío automático de correos fallidos (job, apagado por defecto)
Además del reenvío manual (`POST /sagas/reenviar-notificaciones`, sección 6.3, que funciona siempre), el orquestador puede reenviar solo los
correos que fallaron. Se enciende con `SAGA_REENVIO_NOTIFICACIONES_HABILITADO=true` en el `.env` y `docker compose up -d facturacion-orchestrator-service`:
cada 5 min (`..._INTERVALO`) toma hasta 20 sagas (`..._LOTE`) en `NOTIFICACION_FALLIDA` quietas desde hace 15 min (`..._ESPERA_MINIMA`) y las reenvía;
cada saga se reenvía a lo más 3 veces (`..._MAX_INTENTOS`). Si el resultado del envío no llega en 10 min (`..._PLAZO_RESPUESTA`), la saga vuelve
a `NOTIFICACION_FALLIDA` (nota `ORQ-301`). Para probarlo sin esperar, baja los tiempos: `..._INTERVALO=20s` y `..._ESPERA_MINIMA=PT1M`.
```
GET http://localhost:8096/sagas/api/v1?state=NOTIFICACION_FALLIDA   # incluye "reintentosNotificacion": cuántos reenvíos automáticos lleva
GET http://localhost:8096/sagas/api/v1/{comprobanteId}              # pasos NOTA ORQ-203 (cada reenvío) y ORQ-301 (resultado perdido)
```
Resultado esperado: la saga termina en `NOTIFICADO` en cuanto un envío sale bien. Con `reintentosNotificacion = 3` y la saga todavía en
`NOTIFICACION_FALLIDA`, el job la dejó: corrige la causa (p. ej. la contraseña SMTP) y reenvíala por REST, que no tiene tope. Ver ARQUITECTURA.md 4.2.
