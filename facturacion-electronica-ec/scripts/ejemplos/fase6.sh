#!/usr/bin/env bash
# ICE específico (USD por unidad), ICE con base indicada por el emisor, ítem con ICE + IRBPNR (solo códigos: la tarifa
# sale del catálogo) y los tickets. Cada caso va solo para aislar los rechazos.
source "$(dirname "$0")/lib.sh"
: > "$RES"
sfx=$(date +%H%M%S)

crear_item() {
  curl -s -X POST -H "$A" -H 'Content-Type: application/json' "$INV/items" --data-binary "$1" | perl -ne 'my @m = /"id":"([0-9a-f-]{36})"/g; print $m[-1] if @m'
}
ITEM_ICE_IRB=$(crear_item '{"ruc":"1722039953001","tipo":"PRODUCTO","codigoPrincipal":"INV-ICEIRB-'$sfx'","descripcion":"Bebida con ICE e IRBPNR (item)","precioUnitario":2.00,"unidadMedida":"UN","cantidadInicial":50,"costoInicial":1.00,"stockMinimo":1,"codigoPorcentajeIva":"4","codigoIce":"3031","codigoIrbpnr":"5001"}')
ITEM_CIG=$(crear_item '{"ruc":"1722039953001","tipo":"PRODUCTO","codigoPrincipal":"INV-CIG-'$sfx'","descripcion":"Cigarrillos (item, ICE especifico)","precioUnitario":5.00,"unidadMedida":"UN","cantidadInicial":50,"costoInicial":2.00,"stockMinimo":1,"codigoPorcentajeIva":"4","codigoIce":"3011"}')
echo "items: ice_irbpnr=$ITEM_ICE_IRB cigarrillos=$ITEM_CIG"
sleep 6

det() { echo "{\"codigoPrincipal\":\"$1\",\"descripcion\":\"$2\",\"cantidad\":$3,\"precioUnitario\":$4,\"impuestos\":[$5]$6}"; }
fac() { echo "{\"empresa\":$EMP,$COMP,\"detalles\":[$1]$2}"; }

# ICE específico 3011: USD 0,16 por unidad (base = cantidad); IVA 15% sobre subtotal + ICE
emitir C01 facturas "$(fac "$(det CIG-1 'Cigarrillos rubios' 10 5.00 '{"codigo":"3","codigoPorcentaje":"3011"},{"codigo":"2","codigoPorcentaje":"4"}')")"
# ICE 3610 perfumes, base indicada por el emisor (base por PVP) y tarifa tomada del catálogo (20 %)
emitir C02 facturas "$(fac "$(det PERF-1 'Perfume (base por PVP)' 1 100.00 '{"codigo":"3","codigoPorcentaje":"3610","baseImponible":60.00},{"codigo":"2","codigoPorcentaje":"4"}')")"
# 3031 en su modalidad específica (USD 10,00 por litro de alcohol puro): base = litros indicados
emitir C03 facturas "$(fac "$(det ALC-1 'Alcohol (ICE especifico)' 1 30.00 '{"codigo":"3","codigoPorcentaje":"3031","tipoTarifa":"ESPECIFICA","baseImponible":3},{"codigo":"2","codigoPorcentaje":"4"}')")"
# Ítems de inventario: solo códigos; tarifa y tipo salen del catálogo
emitir C04 facturas "$(fac "{\"itemId\":\"$ITEM_ICE_IRB\",\"codigoPrincipal\":\"INV-ICEIRB-$sfx\",\"descripcion\":\"Bebida con ICE e IRBPNR (item)\",\"cantidad\":6,\"precioUnitario\":2.00}")"
emitir C05 facturas "$(fac "{\"itemId\":\"$ITEM_CIG\",\"codigoPrincipal\":\"INV-CIG-$sfx\",\"descripcion\":\"Cigarrillos (item, ICE especifico)\",\"cantidad\":10,\"precioUnitario\":5.00}")"
# Un código ICE que no está en la Tabla 18 se rechaza antes de llegar al SRI (400)
emitir C06 facturas "$(fac "$(det X-1 'ICE inexistente' 1 10.00 '{"codigo":"3","codigoPorcentaje":"9999"},{"codigo":"2","codigoPorcentaje":"4"}')")"
echo "emitidas: $(wc -l < "$RES")"
