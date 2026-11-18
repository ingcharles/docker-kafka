#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"
: > "$RES"
sfx=$(date +%H%M%S)

# --- Ítems de inventario (para probar la derivación del impuesto desde el ítem: IVA 15% e ICE) ---
crear_item() {
  curl -s -X POST -H "$A" -H 'Content-Type: application/json' "$INV/items" --data-binary "$1" | perl -ne 'my @m = /"id":"([0-9a-f-]{36})"/g; print $m[-1] if @m'
}
ITEM_IVA=$(crear_item '{"ruc":"1722039953001","tipo":"PRODUCTO","codigoPrincipal":"INV-IVA-'$sfx'","descripcion":"Producto de inventario con IVA 15","precioUnitario":12.00,"unidadMedida":"UN","cantidadInicial":50,"costoInicial":8.00,"stockMinimo":1,"codigoPorcentajeIva":"4"}')
ITEM_ICE=$(crear_item '{"ruc":"1722039953001","tipo":"PRODUCTO","codigoPrincipal":"INV-ICE-'$sfx'","descripcion":"Producto de inventario con ICE","precioUnitario":20.00,"unidadMedida":"UN","cantidadInicial":50,"costoInicial":10.00,"stockMinimo":1,"codigoPorcentajeIva":"4","codigoIce":"3031"}')
echo "items: iva=$ITEM_IVA ice=$ITEM_ICE"
sleep 6   # los ítems llegan a la copia local de comprobantes por Kafka

det() { # det CODIGO DESCRIPCION CANTIDAD PRECIO 'impuestos-json' [extra]
  echo "{\"codigoPrincipal\":\"$1\",\"descripcion\":\"$2\",\"cantidad\":$3,\"precioUnitario\":$4,\"impuestos\":[$5]$6}"
}
fac() { echo "{\"empresa\":${EMPRESA:-$EMP},$COMP,\"detalles\":[$1]$2}"; }

# ---- IVA, una tarifa por factura ----
emitir F01b facturas "$(fac "$(det P-15 'Producto IVA 15%' 2 10.00 "$IVA15")")"
emitir F02 facturas "$(fac "$(det P-0 'Producto IVA 0%' 2 10.00 "$IVA0")")"
emitir F03 facturas "$(fac "$(det P-NOOBJ 'No objeto de impuesto' 1 10.00 '{"codigo":"2","codigoPorcentaje":"6","tarifa":0}')")"
emitir F04 facturas "$(fac "$(det P-EXENTO 'Exento de IVA' 1 10.00 '{"codigo":"2","codigoPorcentaje":"7","tarifa":0}')")"
emitir F05 facturas "$(fac "$(det P-5 'Producto IVA 5%' 4 10.00 '{"codigo":"2","codigoPorcentaje":"5","tarifa":5.00}')")"
emitir F06 facturas "$(fac "$(det P-13 'Producto IVA 13%' 2 10.00 '{"codigo":"2","codigoPorcentaje":"10","tarifa":13.00}')")"
emitir F07 facturas "$(fac "$(det P-8 'Servicio turistico IVA 8%' 2 10.00 '{"codigo":"2","codigoPorcentaje":"8","tarifa":8.00}')")"
# ---- Varias líneas con distinto IVA y descuento ----
emitir F08 facturas "$(fac "$(det L-15 'Linea IVA 15' 2 10.00 "$IVA15" ',"descuento":2.00'),$(det L-0 'Linea IVA 0' 3 5.00 "$IVA0"),$(det L-EX 'Linea exenta' 1 7.50 '{"codigo":"2","codigoPorcentaje":"7","tarifa":0}'),$(det L-NO 'Linea no objeto' 1 4.00 '{"codigo":"2","codigoPorcentaje":"6","tarifa":0}')")"
# ---- ICE (impuesto 3) ----
emitir F09 facturas "$(fac "$(det ICE-3031 'Bebida alcoholica ICE 75%' 2 10.00 '{"codigo":"3","codigoPorcentaje":"3031","tarifa":75.00},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
emitir F10 facturas "$(fac "$(det ICE-3660 'Perfume ICE 35%' 1 50.00 '{"codigo":"3","codigoPorcentaje":"3660","tarifa":35.00},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
emitir F11 facturas "$(fac "$(det ICE-3041 'Cerveza ICE' 6 2.00 '{"codigo":"3","codigoPorcentaje":"3041","tarifa":75.00},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
# ---- IRBPNR (impuesto 5): $0.02 por botella ----
emitir F12 facturas "$(fac "$(det IRB-1 'Bebida en botella plastica' 12 1.00 '{"codigo":"5","codigoPorcentaje":"5001","tarifa":0.02},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
emitir F13 facturas "$(fac "$(det ICE-IRB 'Bebida con ICE e IRBPNR' 6 2.00 '{"codigo":"3","codigoPorcentaje":"3031","tarifa":75.00},{"codigo":"5","codigoPorcentaje":"5001","tarifa":0.02},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
# ---- Ítems de inventario: el impuesto se deriva del ítem ----
emitir F14 facturas "$(fac "{\"itemId\":\"$ITEM_IVA\",\"codigoPrincipal\":\"INV-IVA-$sfx\",\"descripcion\":\"Producto de inventario con IVA 15\",\"cantidad\":3,\"precioUnitario\":12.00}")"
emitir F15 facturas "$(fac "{\"itemId\":\"$ITEM_ICE\",\"codigoPrincipal\":\"INV-ICE-$sfx\",\"descripcion\":\"Producto de inventario con ICE\",\"cantidad\":2,\"precioUnitario\":20.00}")"
# ---- Otros detalles ----
emitir F16 facturas "$(fac "$(det P-DET 'Producto con detalles adicionales' 1 10.00 "$IVA15" ',"detallesAdicionales":[{"nombre":"Marca","valor":"ACME"},{"nombre":"Modelo","valor":"X-100"},{"nombre":"Color","valor":"Azul"}]')")"
emitir F17 facturas "$(fac "$(det 0101 'GASOLINA EXTRA' 10 2.50 "$IVA15" ',"precioSinSubsidio":3.00')" ',"placa":"ABC1234"')"
emitir F18 facturas "$(fac "$(det P-DEC 'Cantidad y precio con decimales' 2.5 3.333333 "$IVA15")")"
emitir F19 facturas "$(fac "$(det P-PAGOS 'Factura con pagos mixtos' 2 10.00 "$IVA15")" ',"pagos":[{"formaPago":"01","total":3.00},{"formaPago":"19","total":20.00,"plazo":"30","unidadTiempo":"dias"}]')"
emitir F20 facturas "$(fac "$(det P-NEG 'Factura negociable' 2 10.00 "$IVA15")" ',"pagos":[{"formaPago":"21","total":23.00,"plazo":"30","unidadTiempo":"dias"}]')"
emitir F21 facturas "$(fac "$(det P-INFO 'Con informacion adicional' 1 10.00 "$IVA15")" ',"campoAdicional":[{"nombre":"Observacion","valor":"Entrega en oficina"},{"nombre":"Vendedor","valor":"Pedro"}]')"
# ---- Régimen RIMPE ----
EMPRESA=$EMP_RIMPE emitir F22 facturas "$(EMPRESA=$EMP_RIMPE fac "$(det P-RIMPE 'Emisor RIMPE' 2 10.00 "$IVA15")")"
EMPRESA=$EMP_POP emitir F23 facturas "$(EMPRESA=$EMP_POP fac "$(det P-POP 'Emisor negocio popular' 2 10.00 "$IVA0")")"
# ---- Tipos de comprador ----
emitir F24 facturas "{\"empresa\":$EMP,\"compradorTipoIdentificacion\":\"07\",\"compradorIdentificacion\":\"9999999999999\",\"compradorRazonSocial\":\"CONSUMIDOR FINAL\",\"moneda\":\"DOLAR\",\"detalles\":[$(det P-CF 'Consumidor final' 1 5.00 "$IVA15")]}"
emitir F25 facturas "{\"empresa\":$EMP,\"compradorTipoIdentificacion\":\"04\",\"compradorIdentificacion\":\"1790016919001\",\"compradorRazonSocial\":\"EMPRESA COMPRADORA SA\",\"compradorDireccion\":\"Quito\",\"moneda\":\"DOLAR\",\"detalles\":[$(det P-RUC 'Venta a RUC' 1 10.00 "$IVA15")]}"
emitir F26 facturas "{\"empresa\":$EMP,\"compradorTipoIdentificacion\":\"06\",\"compradorIdentificacion\":\"AB123456\",\"compradorRazonSocial\":\"TURISTA PASAPORTE\",\"compradorDireccion\":\"Quito\",\"moneda\":\"DOLAR\",\"detalles\":[$(det P-PAS 'Venta a pasaporte' 1 10.00 "$IVA15")]}"
emitir F27 facturas "{\"empresa\":$EMP,\"compradorTipoIdentificacion\":\"08\",\"compradorIdentificacion\":\"EXT987654\",\"compradorRazonSocial\":\"CLIENTE DEL EXTERIOR\",\"compradorDireccion\":\"Bogota\",\"moneda\":\"DOLAR\",\"detalles\":[$(det P-EXT 'Venta identificacion exterior' 1 10.00 "$IVA15")]}"
echo "emitidas: $(wc -l < "$RES")"
