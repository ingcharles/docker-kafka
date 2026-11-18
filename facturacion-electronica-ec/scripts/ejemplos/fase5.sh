#!/usr/bin/env bash
# IRBPNR (impuesto 5001: base = unidades, valor = unidades x 0.02) y retenciones de renta con los porcentajes vigentes
# desde el 1/3/2026 (Resolución NAC-DGERCGC26-00000009). Cada retención va sola para aislar los rechazos.
source "$(dirname "$0")/lib.sh"
: > "$RES"

det() { echo "{\"codigoPrincipal\":\"$1\",\"descripcion\":\"$2\",\"cantidad\":$3,\"precioUnitario\":$4,\"impuestos\":[$5]$6}"; }
fac() { echo "{\"empresa\":$EMP,$COMP,\"detalles\":[$1]$2}"; }

# ---- IRBPNR ----
emitir F12 facturas "$(fac "$(det IRB-1 'Bebida en botella plastica' 12 1.00 '{"codigo":"5","codigoPorcentaje":"5001","tarifa":0.02},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"
emitir F13 facturas "$(fac "$(det ICE-IRB 'Bebida con ICE e IRBPNR' 6 2.00 '{"codigo":"3","codigoPorcentaje":"3031","tarifa":75.00},{"codigo":"5","codigoPorcentaje":"5001","tarifa":0.02},{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}')")"

# ---- Retenciones de renta ----
SUJ='"sujetoRetenidoTipoIdentificacion":"04","sujetoRetenidoIdentificacion":"1790000000001","sujetoRetenidoRazonSocial":"Proveedor de Prueba SA","periodoFiscal":"11/2026","parteRelacionada":"NO"'
DS='"codSustento":"01","docSustentoCodigo":"01","docSustentoNumero":"001001000000123","docSustentoFecha":"10/11/2026","pagoLocExt":"01","totalSinImpuestos":300.00,"importeTotal":345.00,"impuestos":[{"codigo":"2","codigoPorcentaje":"4","baseImponible":300.00,"tarifa":15.00,"valor":45.00}]'
ret() { echo "{\"empresa\":$EMP,$SUJ,\"documentosSustento\":[{$DS,\"retenciones\":[$1],\"pagos\":[{\"formaPago\":\"01\",\"total\":345.00}]}]}"; }
renta() { # renta NOMBRE CODIGO PORCENTAJE  (base 300.00)
  local valor; valor=$(perl -e "printf '%.2f', 300 * $3 / 100")
  emitir "$1" retenciones "$(ret "{\"codigo\":\"1\",\"codigoRetencion\":\"$2\",\"baseImponible\":300.00,\"porcentajeRetener\":$3,\"valorRetenido\":$valor}")"
}
renta R303-10 303 10
renta R304-10 304 10
renta R307-3  307 3
renta R309-3  309 3
renta R310-1  310 1
renta R312-2  312 2
renta R320-10 320 10
renta R322-2  322 2
renta R332-0  332 0
echo "emitidas: $(wc -l < "$RES")"
