# Ejemplos de cada tipo de comprobante contra el SRI (ambiente PRUEBAS)

Prueba del 2026-11-17 con la empresa `1722039953001` en el ambiente `PRUEBAS` del SRI. Se emitió un comprobante de cada tipo con cada clase de
detalle e impuesto, **sin correo del receptor**: la saga termina en `AUTORIZADO_SIN_NOTIFICACION` y no se envía ningún correo (se comprobó: ninguna de
las 50 emisiones tiene notificación, ni hay envíos en `notificacion-service`).

**Resultado: 44 de 50 autorizadas en la primera pasada.** Las 6 rechazadas (`NO_AUTORIZADO`) eran de dos causas, explicadas abajo. Las 3 con ICE se corrigieron en `ConstructorDetalles` y se volvieron a emitir: ahora las 5 emisiones con ICE (F09, F10, F11, F13 y F15) quedan autorizadas. Siguen rechazadas solo las 3 retenciones de renta con porcentaje distinto al del SRI.

## Cómo repetirlo
```bash
export APIKEY=<api-key de escritura de la empresa>          # POST /identidad/api/v1/api-keys; revócala al terminar
bash scripts/ejemplos/fase2.sh && bash -c 'source scripts/ejemplos/lib.sh; esperar 420; tabla'   # notas, guías, retenciones, liquidaciones
bash scripts/ejemplos/fase7.sh && bash -c 'source scripts/ejemplos/lib.sh; esperar 360; tabla'   # varias formas de pago a la vez
```
`fase2.sh` emite primero su propia factura base y espera a que se autorice; las notas y las guías la usan. Cada emisión consume un secuencial del SRI;
el ambiente `PRUEBAS` no tiene validez tributaria. Las tablas de abajo que citan `fase1`, `fase4`, `fase5` y `fase6` son el resultado de aquella prueba; esos
scripts ya no están en el repositorio (quedan en el historial de git).

## Facturas (28 emisiones; la de IVA 15% se emitió dos veces)

| Caso | Detalle | Resultado |
|---|---|---|
| F01 | IVA 15% | autorizada |
| F02 | IVA 0% | autorizada |
| F03 | IVA no objeto de impuesto (código 6) | autorizada |
| F04 | IVA exento (código 7) | autorizada |
| F05 | IVA 5% | autorizada |
| F06 | IVA 13% | autorizada |
| F07 | IVA diferenciado 8% (turismo) | autorizada |
| F08 | 4 líneas con IVA 15% (con descuento), 0%, exento y no objeto | autorizada |
| F09 | ICE 3031 (75%) + IVA 15% | rechazada por la base del IVA; **autorizada tras corregir el cálculo** |
| F10 | ICE 3660 (35%) + IVA 15% | rechazada por la base del IVA; **autorizada tras corregir el cálculo** |
| F11 | ICE 3041 (75%) + IVA 15% | autorizada |
| F12 | IRBPNR 5001 ($0,02 por botella) + IVA 15% | autorizada (el cálculo se corrigió: base = unidades, valor = unidades x 0,02) |
| F13 | ICE 3031 + IRBPNR + IVA 15% en la misma línea | autorizada |
| F14 | ítem de inventario con IVA 15% (el impuesto se deriva del ítem) | autorizada |
| F15 | ítem de inventario con ICE 3031 + IVA | rechazada por la base del IVA; **autorizada tras corregir el cálculo** |
| F16 | 3 detalles adicionales por línea | autorizada |
| F17 | combustible (código 0101) con placa y precio sin subsidio | autorizada |
| F18 | cantidad y precio con decimales | autorizada |
| F19 | dos pagos, uno a crédito a 30 días | autorizada |
| F20 | factura negociable (forma de pago 21, a plazo) | autorizada |
| F21 | información adicional (`campoAdicional`) | autorizada |
| F22 | emisor RIMPE | autorizada |
| F23 | emisor negocio popular RIMPE | autorizada |
| F24 | comprador consumidor final (07) | autorizada |
| F25 | comprador con RUC (04) | autorizada |
| F26 | comprador con pasaporte (06) | autorizada |
| F27 | comprador con identificación del exterior (08) | autorizada |

## Notas, guías, retenciones y liquidaciones (22)

| Tipo | Casos | Resultado |
|---|---|---|
| Nota de crédito | IVA 15%, IVA 0%, IVA 5%, varias líneas con descuento y exenta, con detalles adicionales (N01 a N05) | las 5 autorizadas |
| Nota de débito | IVA 15%, IVA 0% con dos motivos, con pagos (D01 a D03) | las 3 autorizadas |
| Guía de remisión | un destinatario; dos destinatarios con documento de sustento (G01, G02) | las 2 autorizadas |
| Retención de IVA | código 9 (10%), sobre el valor del IVA y sobre la base (R01, R02) | las 2 autorizadas |
| Retención de renta | primera pasada: 303 al 10%, 310 al 1%, 312 al 2%, 332 al 0% autorizadas; 312 al 1,75% (R03, R04) y 307 al 2% rechazadas | ver la tabla del punto 3 (porcentajes 2026) |
| Liquidación de compra | IVA 15%, IVA 0%, con detalle adicional y línea exenta (L01 a L03) | las 3 autorizadas |

## Rechazos de la primera pasada y su causa

**1. ICE sumado a la base del IVA (F09, F10, F15) — corregido.** La ficha técnica del SRI dice: *"El valor del ICE formará parte de la base imponible del IVA"* (Anexo del ICE, art. 58 de la LRTI). `ConstructorDetalles` calculaba el IVA solo sobre el subtotal de la línea: en F09 la base del IVA salió 20,00 y el IVA 3,00, cuando con el ICE (15,00) es 35,00 y 5,25. Ahora la base del IVA de una línea es subtotal + ICE (también para ítems de inventario con ICE); el IRBPNR no entra en esa base. Reemisión contra el SRI: F09 (total 40,25), F10 (77,63) y F15 (80,50) autorizadas, y F11 y F13 siguen autorizadas.

Limitación conocida: el ICE se calcula como porcentaje del subtotal de la línea. Las tarifas específicas del ICE (valor fijo por unidad, litro o 100 g de azúcar) y la base por PVP del fabricante (`PVP / ((1+IVA)*(1+ICE))`, art. 76 LRTI) no se calculan: quien emite manda en `precioUnitario` la base imponible ya determinada.

**2. IRBPNR (F12, F13) — corregido.** Es una tarifa específica por unidad (USD 0,02 por botella): en el ejemplo oficial de la ficha la base son las unidades (12000.00), la tarifa 0.02 y el valor 240.00. `ConstructorDetalles` lo calculaba como porcentaje del precio (valor 0,00). Ahora base = cantidad y valor = cantidad × tarifa, y no entra en la base del IVA. El RIDE muestra la fila IRBPNR y el "SUBTOTAL IVA" sin el ICE (Tabla 21). Reemisión: F12 (total 14,04) y F13 (24,27) autorizadas.

**3. Porcentaje de retención de renta.** El SRI valida el par código + porcentaje. Desde el 1/3/2026 rige la Resolución NAC-DGERCGC26-00000009, que cambió porcentajes (p. ej. 1,75% pasó a 2%). Se emitió una retención por código contra el SRI:

| Código | % | Resultado |
|---|---|---|
| 303 | 10 | autorizada |
| 304 | 10 | autorizada |
| 304 | 8 | **rechazada** (porcentaje anterior) |
| 307 | 3 | autorizada |
| 307 | 2 | **rechazada** (porcentaje anterior) |
| 308 | 2 | **rechazada** (no se probó otro porcentaje) |
| 309 | 3 | autorizada |
| 310 | 1 | autorizada |
| 312 | 2 | autorizada |
| 312 | 1,75 | **rechazada** (porcentaje anterior) |
| 320 | 10 | autorizada |
| 322 | 2 | autorizada |
| 332 | 0 | autorizada |
| 343 | 3 | **rechazada** (no se probó otro porcentaje) |

Nuestro servicio no detecta estos errores porque el catálogo de retenciones de renta está vacío (guía de pruebas 9.3) y solo valida el formato; el SRI los rechaza. La tabla código por código del ATS no está en `documentacion/`, y los pares de arriba son solo lo que se verificó emitiendo: cargar un catálogo parcial haría estricta la validación de todos los códigos de renta y rechazaría los no listados.

## ICE específico, base indicada, ítems solo con códigos y ticket

Después de cargar la Tabla 18 y el IRBPNR en el catálogo (V20) y de simplificar el ítem a solo códigos (inventario V8, comprobantes V21):

| Caso | Detalle | Resultado |
|---|---|---|
| C01 | ICE 3011 (cigarrillos, USD 0,16 por unidad): 10 unidades; IVA sobre subtotal + ICE; total 59,34 | autorizada |
| C02 | ICE 3610 con `baseImponible` 60,00 (base por PVP) y la tarifa 20 % tomada del catálogo; total 128,80 | autorizada |
| C03 | ICE 3031 en modalidad `ESPECIFICA` (3 litros de alcohol puro a USD 10,00); total 69,00 | autorizada |
| C04 | ítem de inventario con `codigoIce` 3031 y `codigoIrbpnr` 5001 (solo códigos); total 24,27 | autorizada |
| C05 | ítem de inventario con `codigoIce` 3011 (específico); total 59,34 | autorizada |
| C06 | ICE con código 9999 que no está en la Tabla 18 | rechazada localmente con `400` (`COMP-400-202`), sin llegar al SRI |

El ticket de C04 (`GET .../ticket`, 80 mm) sale con "SUBTOTAL 15%" 12,00, ICE 9,00, IRBPNR 0,12, IVA 15% 3,15 y VALOR TOTAL 24,27.

## Varias formas de pago a la vez (fase7.sh)

Un comprobante admite n pagos; la suma debe ser igual al importe total y los pagos a crédito (19) y el endoso de títulos (21) llevan plazo y unidad de tiempo. Se probaron las formas de pago 01, 15, 16, 17, 18, 19, 20 y 21 combinadas:

| Caso | Comprobante | Pagos | Resultado |
|---|---|---|---|
| P01 | factura 115,00 | 2: 01 y 16 | autorizada |
| P02 | factura 115,00 | 3: 01, 19 a 30 días y 20 | autorizada |
| P03 | factura 115,00 | 4: 01, 16, 17 y 18 | autorizada |
| P04 | factura 230,00 | 5: 01, 16, 17, 18 y 19 a 60 días | autorizada |
| P05 | factura 165,00 con líneas IVA 15 % y 0 % | 3: 01, 19 a 15 días y 21 a 30 días | autorizada |
| P06 | factura 115,00 | 6: 01, 15, 16, 17, 18 y 20 | autorizada |
| P07 | factura con ICE 24,15 | 3: 01, 16 y 20 | autorizada |
| P08 | nota de débito 115,00 | 3: 01, 16 y 20 | autorizada |
| P09 | liquidación de compra 115,00 | 4: 01, 16, 20 y 19 a 30 días | autorizada |
| P10 | pagos que suman 100,00 para un total de 115,00 | 2 | rechazada localmente con `400` |

El pago 21 sin plazo se rechaza localmente con `400` (Anexo 11). El ticket de P04 lista las 5 formas de pago.

## Prueba completa del 2026-11-18

Se volvieron a emitir todos los tipos contra el SRI de `PRUEBAS` con el código final (fase1, fase2, fase4, fase5, fase6 y fase7), sin correo del receptor: **74 comprobantes, todos autorizados** — 27 facturas de la fase 1, 5 notas de crédito, 3 notas de débito, 2 guías de remisión, 4 retenciones de IVA y renta, 3 liquidaciones de compra, y las de ICE, IRBPNR, renta, ítems por códigos y n pagos. Las notas de crédito y las liquidaciones de compra también quedaron autorizadas en la primera prueba (N01 a N05 y L01 a L03) y en esta.

Dos rechazos locales esperados (`400`, no llegan al SRI): C06 (código ICE inexistente) y P10 (pagos que no suman el total). Dos fallos que se corrigieron en esta prueba:
- **Guías de remisión:** el SRI toma `fechaIniTransporte` como la fecha de emisión y la compara con la de la clave de acceso; con la fecha fija del 17/11 las rechazó el 18/11 (`ERROR EN LA ESTRUCTURA DE LA CLAVE DE ACCESO`). El script usa la fecha de hoy y el servicio lo valida antes de emitir (`COMP-400-841`).
- **P05:** el script mandó el pago 21 sin plazo; el servicio lo rechazó con `400` y se corrigió el script.
