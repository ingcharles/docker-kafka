#!/bin/sh
# Arma la configuración de Alertmanager a partir de las variables de entorno y lo arranca.
# Alertmanager no lee variables de entorno en su YAML; por eso se genera aquí (en /tmp) y la clave del correo va a un archivo
# de solo lectura para el proceso, nunca dentro del YAML ni de una imagen.
#
#   ALERTAS_CORREO_HABILITADO=true   envía las alertas por correo (además de mostrarlas en http://localhost:9093 y Grafana)
#   ALERTAS_CORREO_DESTINATARIOS     uno o varios correos separados por coma
#   ALERTAS_CORREO_REMITENTE         quien aparece como remitente
#   ALERTAS_SMTP_HOST                servidor:puerto (p. ej. smtp.gmail.com:587)
#   ALERTAS_SMTP_USUARIO / ALERTAS_SMTP_PASSWORD   cuenta del servidor SMTP (vacíos si el servidor no pide autenticación)
#   ALERTAS_SMTP_REQUIERE_TLS        true (por defecto) | false solo para un servidor local de pruebas
set -eu

CONFIG=/tmp/alertmanager.yml
CLAVE=/tmp/alertmanager-smtp-clave

receptor_por_defecto=pantalla
if [ "${ALERTAS_CORREO_HABILITADO:-false}" = "true" ]; then
  : "${ALERTAS_CORREO_DESTINATARIOS:?ALERTAS_CORREO_DESTINATARIOS es obligatorio con ALERTAS_CORREO_HABILITADO=true}"
  : "${ALERTAS_CORREO_REMITENTE:?ALERTAS_CORREO_REMITENTE es obligatorio con ALERTAS_CORREO_HABILITADO=true}"
  : "${ALERTAS_SMTP_HOST:?ALERTAS_SMTP_HOST es obligatorio con ALERTAS_CORREO_HABILITADO=true}"
  receptor_por_defecto=correo
fi

{
  echo "# Generado por entrypoint.sh en cada arranque; no se edita a mano."
  if [ "$receptor_por_defecto" = "correo" ]; then
    echo "global:"
    echo "  smtp_smarthost: '${ALERTAS_SMTP_HOST}'"
    echo "  smtp_from: '${ALERTAS_CORREO_REMITENTE}'"
    echo "  smtp_require_tls: ${ALERTAS_SMTP_REQUIERE_TLS:-true}"
    if [ -n "${ALERTAS_SMTP_USUARIO:-}" ]; then
      printf '%s' "${ALERTAS_SMTP_PASSWORD:?ALERTAS_SMTP_PASSWORD es obligatorio si hay ALERTAS_SMTP_USUARIO}" > "$CLAVE"
      chmod 600 "$CLAVE"
      echo "  smtp_auth_username: '${ALERTAS_SMTP_USUARIO}'"
      echo "  smtp_auth_password_file: ${CLAVE}"
    fi
  fi
  cat <<YAML
route:
  receiver: ${receptor_por_defecto}
  group_by: [alertname, servicio]
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  routes:
    # Críticas: actuar ya; se repiten cada hora hasta que se resuelvan.
    - matchers: [severidad="critica"]
      receiver: ${receptor_por_defecto}
      repeat_interval: 1h

receivers:
  - name: pantalla
YAML
  if [ "$receptor_por_defecto" = "correo" ]; then
    cat <<YAML
  - name: correo
    email_configs:
      - to: '${ALERTAS_CORREO_DESTINATARIOS}'
        send_resolved: true
YAML
  fi
  cat <<YAML

inhibit_rules:
  # Si un servicio está caído, no avisar además de sus errores 5xx ni de su latencia.
  - source_matchers: [alertname="ServicioCaido"]
    target_matchers: [alertname=~"ErroresHttp5xx|LatenciaAlta|MemoriaJvmAlta"]
    equal: [servicio]
YAML
} > "$CONFIG"

exec /bin/alertmanager --config.file="$CONFIG" --storage.path=/alertmanager "$@"
