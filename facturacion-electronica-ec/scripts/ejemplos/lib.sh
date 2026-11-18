#!/usr/bin/env bash
# Utilidades para emitir casos de prueba contra comprobantes-service y esperar el resultado del SRI.
S="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COM=http://localhost:8093/comprobantes/api/v1
INV=http://localhost:8092/inventario/api/v1
SAG=http://localhost:8096/sagas/api/v1
A="Authorization: Bearer ${APIKEY:?define APIKEY con una api-key de escritura de la empresa}"
RES="${RES:-${TMPDIR:-/tmp}/resultados-ejemplos.txt}"

EMP='{"ruc":"1722039953001","razonSocial":"ANCHUNDIA VALENCIA CARLOS EDUARDO","direccionMatriz":"LLANO CHICO","ambiente":"PRUEBAS","establecimiento":"001","puntoEmision":"001"}'
EMP_RIMPE='{"ruc":"1722039953001","razonSocial":"ANCHUNDIA VALENCIA CARLOS EDUARDO","direccionMatriz":"LLANO CHICO","ambiente":"PRUEBAS","establecimiento":"001","puntoEmision":"001","regimenRimpe":"RIMPE"}'
EMP_POP='{"ruc":"1722039953001","razonSocial":"ANCHUNDIA VALENCIA CARLOS EDUARDO","direccionMatriz":"LLANO CHICO","ambiente":"PRUEBAS","establecimiento":"001","puntoEmision":"001","regimenRimpe":"NEGOCIO_POPULAR"}'
# Comprador por defecto: cédula, SIN correo (la saga termina en AUTORIZADO_SIN_NOTIFICACION: no se envía ningún correo).
COMP='"compradorTipoIdentificacion":"05","compradorIdentificacion":"1710034065","compradorRazonSocial":"Consumidor de Prueba","compradorDireccion":"Quito","moneda":"DOLAR"'
IVA15='{"codigo":"2","codigoPorcentaje":"4","tarifa":15.00}'
IVA0='{"codigo":"2","codigoPorcentaje":"0","tarifa":0}'

# emitir NOMBRE ruta 'json'  -> registra "NOMBRE|id|http|secuencial|clave" en resultados.txt
emitir() {
  local nombre="$1" ruta="$2" json="$3" out http id sec clave
  out=$(curl -s -w '\n%{http_code}' -X POST -H "$A" -H 'Content-Type: application/json' --data-binary "$json" "$COM/$ruta")
  http=$(echo "$out" | tail -1)
  id=$(echo "$out" | head -1 | perl -ne 'print $1 if /"id":"([0-9a-f-]{36})","(?:importeTotal|moneda|motivo|numero|periodoFiscal|puntoEmision|razon)/ or /,"id":"([0-9a-f-]{36})"[^{}]*"(?:estado)"/' )
  [ -z "$id" ] && id=$(echo "$out" | head -1 | perl -ne 'my @m = /"id":"([0-9a-f-]{36})"/g; print $m[-1] if @m')
  sec=$(echo "$out" | head -1 | perl -ne 'print $1 if /"secuencial":"(\d+)"/')
  clave=$(echo "$out" | head -1 | perl -ne 'print $1 if /"claveAcceso":"(\d+)"/')
  if [ "$http" != "201" ]; then
    echo "$nombre|-|$http|-|-|$(echo "$out" | head -1 | perl -ne 'print $1 if /"message":"([^"]*)"/; print " ".join(";", /"details":\[([^\]]*)\]/g)' | cut -c1-200)" >> "$RES"
  else
    echo "$nombre|$id|$http|$sec|$clave|" >> "$RES"
  fi
}

# esperar: consulta cada comprobante emitido hasta estado terminal (o 5 min) y muestra una tabla
esperar() {
  local limite=${1:-300} t0=$(date +%s) pend
  while :; do
    pend=0
    while IFS='|' read -r nombre id http sec clave nota; do
      [ "$http" = "201" ] || continue
      est=$(curl -s -H "$A" "$COM/comprobantes/$id" | perl -ne 'print $1 if /"estado":"(\w+)"/')
      case "$est" in AUTORIZADO|NOTIFICADO|NO_AUTORIZADO|RECHAZADO*|FIRMA_FALLIDA|STOCK_INSUFICIENTE|EXPIRADO) ;; *) pend=$((pend+1));; esac
    done < "$RES"
    [ "$pend" = 0 ] && break
    [ $(( $(date +%s) - t0 )) -ge "$limite" ] && break
    sleep 10
  done
}

tabla() {
  printf '%-6s %-4s %-14s %-27s %s\n' CASO HTTP COMPROBANTE SAGA MOTIVO
  while IFS='|' read -r nombre id http sec clave nota; do
    if [ "$http" = "201" ]; then
      j=$(curl -s -H "$A" "$COM/comprobantes/$id")
      est=$(echo "$j" | perl -ne 'print $1 if /"estado":"(\w+)"/')
      mot=$(echo "$j" | perl -ne 'print $1 if /"motivoRechazo":"([^"]*)"/')
      tot=$(echo "$j" | perl -ne 'print $1 if /"importeTotal":([\d.]+)/')
      sg=$(curl -s -H "$A" "$SAG/$id" | perl -ne 'print $1 if /"state":"(\w+)"/')
      printf '%-6s %-4s %-14s %-27s %s\n' "$nombre" "$http" "$est" "$sg" "total=$tot ${mot:0:150}"
    else
      printf '%-6s %-4s %-14s %-27s %s\n' "$nombre" "$http" "(no creado)" "-" "$nota"
    fi
  done < "$RES"
}
