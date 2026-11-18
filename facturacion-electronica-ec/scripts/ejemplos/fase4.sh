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

# ---- ICE (impuesto 3) ----
emitir F09 facturas "$(fac "$(det ICE-3031 'Bebida alcoholica ICE 75%' 2 10.00 '{"codigo":"3","codigoPorcentaje":"3031","tarifa":75.00},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
emitir F10 facturas "$(fac "$(det ICE-3660 'Perfume ICE 35%' 1 50.00 '{"codigo":"3","codigoPorcentaje":"3660","tarifa":35.00},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
emitir F11 facturas "$(fac "$(det ICE-3041 'Cerveza ICE' 6 2.00 '{"codigo":"3","codigoPorcentaje":"3041","tarifa":75.00},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
emitir F13 facturas "$(fac "$(det ICE-IRB 'Bebida con ICE e IRBPNR' 6 2.00 '{"codigo":"3","codigoPorcentaje":"3031","tarifa":75.00},{"codigo":"5","codigoPorcentaje":"5001","tarifa":0.02},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
emitir F15 facturas "$(fac "{\"itemId\":\"$ITEM_ICE\",\"codigoPrincipal\":\"INV-ICE-$sfx\",\"descripcion\":\"Producto de inventario con ICE\",\"cantidad\":2,\"precioUnitario\":20.00}")"
echo "emitidas: $(wc -l < "$RES")"
