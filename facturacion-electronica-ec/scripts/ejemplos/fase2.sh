#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"
: > "$RES"
DOC='"docModificadoTipo":"01","docModificadoNumero":"001-001-000000015","docModificadoFecha":"17/11/2026"'   # factura F01, autorizada
CLAVE_F01=1711202601172203995300110010010000000158408025015

det() { echo "{\"codigoPrincipal\":\"$1\",\"descripcion\":\"$2\",\"cantidad\":$3,\"precioUnitario\":$4,\"impuestos\":[$5]$6}"; }

# ---- Notas de crédito ----
nc() { echo "{\"empresa\":$EMP,$COMP,$DOC,\"motivo\":\"$1\",\"detalles\":[$2]$3}"; }
emitir N01 notas-credito "$(nc 'DEVOLUCION DE MERCADERIA' "$(det P-15 'Producto IVA 15%' 1 10.00 "$IVA15")")"
emitir N02 notas-credito "$(nc 'DESCUENTO POR VOLUMEN' "$(det P-0 'Producto IVA 0%' 2 10.00 "$IVA0")")"
emitir N03 notas-credito "$(nc 'AJUSTE DE PRECIO' "$(det P-5 'Producto IVA 5%' 4 10.00 '{"codigo":"2","codigoPorcentaje":"5","tarifa":5.00}')")"
emitir N04 notas-credito "$(nc 'DEVOLUCION PARCIAL' "$(det L-15 'Linea IVA 15' 2 10.00 "$IVA15" ',"descuento":2.00'),$(det L-EX 'Linea exenta' 1 7.50 '{"codigo":"2","codigoPorcentaje":"7","tarifa":0}')")"
emitir N05 notas-credito "$(nc 'DEVOLUCION CON DETALLES' "$(det P-DET 'Producto con detalles' 1 10.00 "$IVA15" ',"detallesAdicionales":[{"nombre":"Lote","valor":"L-2026"}]')" ',"campoAdicional":[{"nombre":"Observacion","valor":"Devolucion total"}]')"

# ---- Notas de débito ----
nd() { echo "{\"empresa\":$EMP,$COMP,$DOC,\"motivos\":[$1],\"impuestos\":[$2]$3}"; }
emitir D01 notas-debito "$(nd '{"razon":"Interes por mora","valor":15.00}' "$IVA15")"
emitir D02 notas-debito "$(nd '{"razon":"Gastos de cobranza","valor":10.00},{"razon":"Intereses","valor":5.00}' "$IVA0")"
emitir D03 notas-debito "$(nd '{"razon":"Recargo por flete","valor":20.00}' "$IVA15" ',"pagos":[{"formaPago":"01","total":23.00}]')"

# ---- Guías de remisión ----
HOY=$(date +%d/%m/%Y)   # el SRI toma fechaIniTransporte como fecha de emisión: debe ser la de hoy
GR_BASE="\"empresa\":$EMP,\"dirPartida\":\"LLANO CHICO, Quito\",\"transportistaRazonSocial\":\"Transportes de Prueba SA\",\"transportistaTipoIdentificacion\":\"04\",\"transportistaRuc\":\"1790000000001\",\"fechaIniTransporte\":\"$HOY\",\"fechaFinTransporte\":\"$HOY\",\"placa\":\"PBX1234\""
emitir G01 guias-remision "{$GR_BASE,\"destinatarios\":[{\"identificacion\":\"1710034065\",\"razonSocial\":\"Consumidor de Prueba\",\"direccion\":\"Av. Amazonas y Naciones Unidas, Quito\",\"motivoTraslado\":\"Venta de mercaderia\",\"detalles\":[{\"codigoPrincipal\":\"TAB-02\",\"descripcion\":\"Tablet 10 pulgadas\",\"cantidad\":1}]}]}"
emitir G02 guias-remision "{$GR_BASE,\"destinatarios\":[{\"identificacion\":\"1710034065\",\"razonSocial\":\"Consumidor de Prueba\",\"direccion\":\"Quito norte\",\"motivoTraslado\":\"Venta\",\"codDocSustento\":\"01\",\"numDocSustento\":\"001-001-000000015\",\"numAutDocSustento\":\"$CLAVE_F01\",\"fechaEmisionDocSustento\":\"17/11/2026\",\"detalles\":[{\"codigoPrincipal\":\"P-15\",\"descripcion\":\"Producto IVA 15%\",\"cantidad\":2}]},{\"identificacion\":\"1790016919001\",\"razonSocial\":\"EMPRESA COMPRADORA SA\",\"direccion\":\"Guayaquil\",\"motivoTraslado\":\"Traslado entre bodegas\",\"detalles\":[{\"codigoPrincipal\":\"P-0\",\"descripcion\":\"Producto IVA 0%\",\"cantidad\":5},{\"codigoPrincipal\":\"P-EX\",\"descripcion\":\"Producto exento\",\"cantidad\":3}]}]}"

# ---- Retenciones ----
SUJ='"sujetoRetenidoTipoIdentificacion":"04","sujetoRetenidoIdentificacion":"1790000000001","sujetoRetenidoRazonSocial":"Proveedor de Prueba SA","periodoFiscal":"11/2026","parteRelacionada":"NO"'
DS='"codSustento":"01","docSustentoCodigo":"01","docSustentoNumero":"001001000000123","docSustentoFecha":"10/11/2026","pagoLocExt":"01","totalSinImpuestos":300.00,"importeTotal":345.00,"impuestos":[{"codigo":"2","codigoPorcentaje":"4","baseImponible":300.00,"tarifa":15.00,"valor":45.00}]'
ret() { echo "{\"empresa\":$EMP,$SUJ,\"documentosSustento\":[{$DS,\"retenciones\":[$1],\"pagos\":[{\"formaPago\":\"01\",\"total\":345.00}]}]}"; }
emitir R01 retenciones "$(ret '{"codigo":"2","codigoRetencion":"9","baseImponible":300.00,"porcentajeRetener":10,"valorRetenido":30.00}')"
emitir R02 retenciones "$(ret '{"codigo":"2","codigoRetencion":"9","baseImponible":45.00,"porcentajeRetener":10,"valorRetenido":4.50}')"
emitir R03 retenciones "$(ret '{"codigo":"1","codigoRetencion":"312","baseImponible":300.00,"porcentajeRetener":2,"valorRetenido":6.00}')"
emitir R04 retenciones "$(ret '{"codigo":"1","codigoRetencion":"312","baseImponible":300.00,"porcentajeRetener":2,"valorRetenido":6.00},{"codigo":"2","codigoRetencion":"9","baseImponible":45.00,"porcentajeRetener":10,"valorRetenido":4.50}')"

# ---- Liquidaciones de compra ----
LQ='"proveedorTipoIdentificacion":"05","proveedorIdentificacion":"1710034065","proveedorRazonSocial":"Persona Natural Proveedora","moneda":"DOLAR"'
lq() { echo "{\"empresa\":$EMP,$LQ,\"detalles\":[$1]$2}"; }
emitir L01 liquidaciones-compra "$(lq "$(det SERV01 'Servicio de mantenimiento' 1 50.00 "$IVA15")")"
emitir L02 liquidaciones-compra "$(lq "$(det SERV02 'Compra de productos agricolas' 10 3.00 "$IVA0")")"
emitir L03 liquidaciones-compra "$(lq "$(det L-15 'Servicio gravado' 2 20.00 "$IVA15" ',"detallesAdicionales":[{"nombre":"Referencia","valor":"OC-77"}]'),$(det L-EX 'Compra exenta' 1 15.00 '{"codigo":"2","codigoPorcentaje":"7","tarifa":0}')")"
echo "emitidas: $(wc -l < "$RES")"
