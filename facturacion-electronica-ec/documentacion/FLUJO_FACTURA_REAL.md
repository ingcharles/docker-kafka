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
  "esGranContribuyente":false,"vendeCombustibles":false,"ambiente":"PRUEBAS",
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
  "codigoImpuesto":"2","codigoPorcentaje":"4","tieneIce":false}'              # guarda el "id" → ITEM_ID
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

## 7. Problemas frecuentes
| Síntoma | Causa y solución |
|---|---|
| `403 IDE-403-003` al crear la api-key | El rol `ADMIN` se concede por Kafka tras crear la empresa; espera unos segundos (`GET $ID/usuarios/me/empresas` muestra cuándo ya está). |
| `401 Falta el header Authorization` en registro o login | `identidad-service` debe correr con `facturacion.filtro-api-key.habilitado=false` (ya viene en su `application.properties`). |
| Saga en `FIRMA_FALLIDA` | Certificado vencido, sin certificado o clave del `.p12` incorrecta. |
| Saga en `NOTIFICACION_FALLIDA` | SMTP: contraseña de aplicación incorrecta o cuenta sin verificación en dos pasos. El comprobante sigue autorizado. |
| Saga en `RECHAZADO_RECEPCION` con `ERROR SECUENCIAL REGISTRADO` | El SRI recuerda los secuenciales ya autorizados para ese RUC, establecimiento y punto aunque la base local se reinicie, que vuelve a emitir desde el 1. Sube el contador por encima del último autorizado: `update comprobantes.secuencial_comprobante set ultimo_secuencial = 10 where ruc='1722039953001' and tipo_comprobante='FACTURA';` (en `docker exec <postgres> psql -U postgres -d facturacion`). Si la fila no existe, emite una factura para que se cree y repite. |
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
| Notificación | `ENVIADA` a `e.du.ingcharles@hotmail.com` con RIDE y XML |
| RIDE | formato SRI con logo, código de barras y totales; el logo no tapa la razón social |

Hallazgos de la corrida:
- Con la base reiniciada, la primera factura salió `RECHAZADO_RECEPCION` por `ERROR SECUENCIAL REGISTRADO` (ver sección 7);
  tras subir `ultimo_secuencial` la siguiente se autorizó.
- Un `.p12` con otra contraseña responde `400 FIR-400-001` y la saga de una factura sin certificado queda en `FIRMA_FALLIDA`
  (`La empresa … no tiene un certificado activo`).
