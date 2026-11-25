#!/usr/bin/env bash
# Abre un port-forward por servicio del clúster k3s de facturacion-despliegue/pruebas-locales hacia tu máquina, para que el WSO2 de
# Docker los alcance con host.docker.internal. Los puertos son los de BACKEND_PUERTOS en wso2/env/desarrollo-local.env.
#
#   wso2/scripts/exponer-servicios-k3s.sh            abre los 7 (queda en primer plano; Ctrl+C los cierra)
#   NAMESPACE=otro wso2/scripts/exponer-servicios-k3s.sh
set -euo pipefail
AQUI="$(cd "$(dirname "$0")" && pwd)"
ENV="$AQUI/../env/desarrollo-local.env"
KUBECONFIG_LOCAL="${KUBECONFIG_LOCAL:-$AQUI/../../../facturacion-despliegue/pruebas-locales/.local/kubeconfig}"
NAMESPACE="${NAMESPACE:-facturacion-desarrollo}"
[ -s "$KUBECONFIG_LOCAL" ] || { echo "No hay clúster k3s: falta $KUBECONFIG_LOCAL. Corre antes facturacion-despliegue/pruebas-locales/flujo-local.sh subir" >&2; exit 1; }
export KUBECONFIG="$KUBECONFIG_LOCAL"

pids=()
trap 'kill "${pids[@]}" 2>/dev/null || true' EXIT INT TERM
for par in $(grep '^BACKEND_PUERTOS=' "$ENV" | cut -d= -f2- | tr -d '\r'); do
  servicio="${par%%=*}"; puerto="${par##*=}"
  kubectl -n "$NAMESPACE" port-forward "svc/$servicio" "$puerto:8080" >/dev/null &
  pids+=($!)
  echo "http://localhost:$puerto  ->  $servicio"
done
echo "(Ctrl+C para cerrar)"
wait
