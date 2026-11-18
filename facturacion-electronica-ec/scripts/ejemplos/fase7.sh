#!/usr/bin/env bash
# Varias formas de pago a la vez (n pagos) en factura, nota de débito y liquidación de compra. Cada pago lleva un código
# de la Tabla 24 (01 sin sistema financiero, 15 compensación de deudas, 16 tarjeta de débito, 17 dinero electrónico,
# 18 tarjeta prepago, 19 tarjeta de crédito, 20 otros con sistema financiero, 21 endoso de títulos); la suma de los
# pagos debe ser igual al importe total del comprobante.
source "$(dirname "$0")/lib.sh"
: > "$RES"
DOC='"docModificadoTipo":"01","docModificadoNumero":"001-001-000000015","docModificadoFecha":"17/11/2026"'   # factura autorizada

det() { echo "{\"codigoPrincipal\":\"$1\",\"descripcion\":\"$2\",\"cantidad\":$3,\"precioUnitario\":$4,\"impuestos\":[$5]$6}"; }
fac() { echo "{\"empresa\":$EMP,$COMP,\"detalles\":[$1]$2}"; }
pago() { # pago FORMA TOTAL [PLAZO]
  if [ -n "$3" ]; then echo "{\"formaPago\":\"$1\",\"total\":$2,\"plazo\":\"$3\",\"unidadTiempo\":\"dias\"}"; else echo "{\"formaPago\":\"$1\",\"total\":$2}"; fi
}
pagos() { local IFS=,; echo ",\"pagos\":[$*]"; }

# Factura de 100,00 + IVA 15 % = 115,00 con 2, 3, 4 y 6 pagos
emitir P01 facturas "$(fac "$(det P-1 'Producto' 1 100.00 "$IVA15")" "$(pagos "$(pago 01 50.00)" "$(pago 16 65.00)")")"
emitir P02 facturas "$(fac "$(det P-2 'Producto' 1 100.00 "$IVA15")" "$(pagos "$(pago 01 15.00)" "$(pago 19 40.00 30)" "$(pago 20 60.00)")")"
emitir P03 facturas "$(fac "$(det P-3 'Producto' 1 100.00 "$IVA15")" "$(pagos "$(pago 01 15.00)" "$(pago 16 40.00)" "$(pago 17 25.00)" "$(pago 18 35.00)")")"
emitir P06 facturas "$(fac "$(det P-6 'Producto' 1 100.00 "$IVA15")" "$(pagos "$(pago 01 10.00)" "$(pago 15 20.00)" "$(pago 16 20.00)" "$(pago 17 20.00)" "$(pago 18 20.00)" "$(pago 20 25.00)")")"
# Factura de 200,00 + IVA 15 % = 230,00 con 5 pagos (uno a crédito a 60 días)
emitir P04 facturas "$(fac "$(det P-4 'Producto' 2 100.00 "$IVA15")" "$(pagos "$(pago 01 30.00)" "$(pago 16 50.00)" "$(pago 17 40.00)" "$(pago 18 60.00)" "$(pago 19 50.00 60)")")"
# Líneas con IVA 15 % y 0 % (total 165,00) con 3 pagos, uno a crédito y uno por endoso de títulos
emitir P05 facturas "$(fac "$(det L-15 'Linea IVA 15' 1 100.00 "$IVA15"),$(det L-0 'Linea IVA 0' 1 50.00 "$IVA0")" "$(pagos "$(pago 01 100.00)" "$(pago 19 40.00 15)" "$(pago 21 25.00 30)")")"
# Factura con ICE (total 24,15) con 3 pagos
emitir P07 facturas "$(fac "$(det ICE-3041 'Cerveza ICE' 6 2.00 '{"codigo":"3","codigoPorcentaje":"3041"},{"codigo":"2","codigoPorcentaje":"4"}')" "$(pagos "$(pago 01 10.00)" "$(pago 16 8.00)" "$(pago 20 6.15)")")"
# Nota de débito de 100,00 + IVA 15 % = 115,00 con 3 pagos
emitir P08 notas-debito "{\"empresa\":$EMP,$COMP,$DOC,\"motivos\":[{\"razon\":\"Interes por mora\",\"valor\":60.00},{\"razon\":\"Gastos de cobranza\",\"valor\":40.00}],\"impuestos\":[$IVA15]$(pagos "$(pago 01 40.00)" "$(pago 16 40.00)" "$(pago 20 35.00)")}"
# Liquidación de compra de 100,00 + IVA 15 % = 115,00 con 4 pagos
emitir P09 liquidaciones-compra "{\"empresa\":$EMP,\"proveedorTipoIdentificacion\":\"05\",\"proveedorIdentificacion\":\"1710034065\",\"proveedorRazonSocial\":\"Persona Natural Proveedora\",\"moneda\":\"DOLAR\",\"detalles\":[$(det SERV 'Servicio' 1 100.00 "$IVA15")]$(pagos "$(pago 01 15.00)" "$(pago 16 30.00)" "$(pago 20 30.00)" "$(pago 19 40.00 30)")}"
# Pagos que no suman el total: se rechazan localmente con 400
emitir P10 facturas "$(fac "$(det P-10 'Producto' 1 100.00 "$IVA15")" "$(pagos "$(pago 01 50.00)" "$(pago 16 50.00)")")"
echo "emitidas: $(wc -l < "$RES")"
