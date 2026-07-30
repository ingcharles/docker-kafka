# Guía de pruebas manuales — flujo completo por API

JSON listo para pegar en Swagger UI (botón "Try it out") o en `curl`, cubriendo los
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

Swagger UI de cada uno: `http://localhost:<puerto><prefijo>/q/swagger-ui`.

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
Respuesta: `{ "token": "<TOKEN_SESION>" }`. En Swagger, pega `<TOKEN_SESION>` en el
botón **Authorize** (sin la palabra "Bearer").

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
  "vendeCombustibles": false,
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
del certificado) — pruébalo desde Swagger UI directamente, no aplica un body
JSON.

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

### 2.1 Asociarte a una empresa que ya existe
Úsalo en vez de 1.3 si la empresa **ya fue creada** por otra persona/sesión y
solo necesitas que tu usuario quede asociado a ella. Si la empresa todavía no
existe, esto da `404` — créala primero con 1.3.

`POST http://localhost:8097/identidad/api/v1/usuarios/me/empresas` (auth: token de sesión)
```json
{
  "ruc": "1722039953001",
  "rol": "ADMIN"
}
```

### 2.2 Listar / revocar api-keys
`GET http://localhost:8097/identidad/api/v1/api-keys` (auth: token de sesión)
`DELETE http://localhost:8097/identidad/api/v1/api-keys/{id}` (auth: token de sesión)

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

`DELETE http://localhost:8097/identidad/api/v1/usuarios/me/empresas/{ruc}` (auth: token de sesión) — abandonar un ruc al que te habías autoasociado; `404` si no estabas asociado, `409` si eres el único `ADMIN` que le queda.

### 2.4 Gestión de miembros de una empresa (solo ADMIN)
Cualquier usuario puede autoasociarse a un ruc con `POST /usuarios/me/empresas`
(2.1) — estos 3 endpoints son la otra mitad: le dan a un **ADMIN ya
existente** de ese ruc visibilidad y control sobre quién más tiene acceso.
No es un flujo de invitación/aprobación (no hay estado pendiente ni se
notifica a nadie): el ADMIN actúa directamente sobre asociaciones que el
propio usuario ya creó. Ninguno de los 3 deja a un ruc sin ningún `ADMIN`
(`409` si lo intentas, incluso contigo mismo).

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
  "vendeCombustibles": false,
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

---

## 4. inventario-service — catálogo y kardex

Todo con la **api-key**.

### 4.1 Categoría (opcional)
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
  "tarifaIva": 15.00,
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
  "codigoPorcentaje": "4",
  "tarifaIva": 15.00
}
```

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
  "tarifaIva": 15.00,
  "tieneIce": false
}
```
> No permite tocar existencias directamente (`cantidadInicial`/stock actual) —
> eso solo cambia vía kardex (4.6/4.7). `categoriaId` es opcional.

### 4.5 Imágenes de un ítem
`POST http://localhost:8092/inventario/api/v1/items/{id}/imagenes` es
`multipart/form-data` (campos: `archivo` la imagen JPEG/PNG/WEBP/GIF máx. 5 MB,
`esPrincipal` boolean, `orden` entero) — pruébalo desde Swagger UI, no aplica
JSON. Si `esPrincipal=true`, desmarca cualquier otra imagen principal del mismo ítem.

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
docker exec -i facturacion-electronica-ec-facturacion-db-1 \
  psql -U postgres -d facturacion <<'EOF'
-- (pega aquí uno de los dos bloques de abajo)
EOF
```
O interactivo: `docker exec -it facturacion-electronica-ec-facturacion-db-1 psql -U postgres -d facturacion`, y pega el bloque directo en la sesión.

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

-- comprobantes (hijos antes que comprobante; el resto no tiene FK)
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
