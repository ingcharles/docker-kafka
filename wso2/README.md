# WSO2 API Manager: gateway de facturación electrónica

Los 7 servicios no se publican: solo el gateway (WSO2) llega al Ingress del clúster. Aquí está **la configuración de WSO2 como código**
(contratos, límites, aplicación cliente) y un WSO2 de desarrollo en Docker para probarla.

```
cliente ──► WSO2 (8243)  ──► Ingress del clúster ──► servicio
            · exige la api-key de WSO2 (cabecera ApiKey)
            · límites por minuto (por aplicación; en login y emisión, además por operación)
            · TLS / WAF / tamaño de cuerpo (plataforma)
            · publica SOLO las rutas de negocio (sin /q/health, /q/metrics ni swagger)
```

## Dos credenciales, cada una en su cabecera

| Cabecera | La emite | La valida | Para qué |
|---|---|---|---|
| `ApiKey: <api-key de WSO2>` | WSO2 (aplicación cliente) | el gateway | quién es el cliente, cuotas y límites |
| `Authorization: Bearer <api-key o sesión del servicio>` | `identidad-service` | cada servicio (JWT Ed25519, llave pública) | qué empresa y rol tiene (autorización de negocio) |

WSO2 **no toca `Authorization`**: viaja intacta al servicio. Por eso las APIs usan solo el esquema `api_key` (no OAuth2). `Idempotency-Key`
también pasa sin cambios. Sin `ApiKey` el gateway responde `401` aunque la sesión sea válida; con `ApiKey` pero sin `Authorization`, es el
servicio el que responde `401`.

## Qué hay aquí

| Ruta | Qué es |
|---|---|
| `docker-compose.yml` | WSO2 API Manager 4.3.0 de desarrollo (H2 interna; en producción es el de la plataforma) |
| `openapi/*.yaml` | **Generado** de las clases `*Resource` (83 operaciones): lo que el gateway acepta. No se edita a mano |
| `openapi/servicios.tsv` | servicio → ruta base (`/identidad/api/v1`, …) |
| `politicas.conf` | operaciones con límite propio (login, alta de usuarios, cambio de clave, api-keys → `sensible`; POST de comprobantes → `emision`). Ver [Límites](#límites) |
| `env/<ambiente>.env` | URL de WSO2, destino de las APIs y **límites por minuto** de `desarrollo-local`, `pruebas` y `produccion` |
| `scripts/generar-contrato.pl` | regenera `openapi/` y el `ingress.yaml` de los overlays del repositorio de despliegue |
| `scripts/configurar-wso2.pl` | crea/actualiza en WSO2 las políticas, las 7 APIs, la aplicación cliente y su api-key (idempotente) |
| `scripts/exponer-servicios-k3s.sh` | solo local: abre un `port-forward` por servicio del k3s para que WSO2 los alcance |

## Límites

Peticiones por minuto, en `env/<ambiente>.env`. La aplicación cliente es la dueña de la `ApiKey`: todos sus usuarios comparten sus cupos.

| Variable | Qué cuenta | Cómo se hace en WSO2 |
|---|---|---|
| `LIMITE_APLICACION_POR_MIN` | todo lo que hace **una aplicación**, sumando las APIs | política de aplicación |
| `LIMITE_SUSCRIPCION_POR_MIN` | **una aplicación** en cada API | política de suscripción |
| `LIMITE_SENSIBLE_POR_MIN`, `LIMITE_EMISION_POR_MIN` | **una aplicación** en cada operación marcada en `politicas.conf` | regla personalizada `...PorCliente` (clave: aplicación + operación) |
| `LIMITE_SENSIBLE_TOTAL_POR_MIN`, `LIMITE_EMISION_TOTAL_POR_MIN` | cada operación marcada, **sumando a todos los clientes** (protege al servicio) | política de recurso |

- Una aplicación que agota su cupo recibe `429` sin afectar a las demás. Para separar a dos consumidores, dale una aplicación a cada uno.
- El conteo es asíncrono: pueden pasar una o dos peticiones por encima del límite.
- Las reglas personalizadas las crea solo el **super administrador** de WSO2; en un WSO2 compartido hay que pedirlas a la plataforma.
- No hay límite por **IP** ni por **cuenta** (correo). WSO2 toma la IP de `X-Forwarded-For`, que el cliente puede inventar si el balanceador no la reescribe.

> **Tras reiniciar el contenedor de desarrollo (`docker restart`, `stop`/`start` o reinicio del equipo) WSO2 4.3.0 deja de aplicar todos los
> límites, sin errores visibles.** Recréalo (`down`, `up -d`) y vuelve a configurarlo con `--rotar`. En un ambiente real, comprueba los
> límites después de cada reinicio: más intentos de login que el cupo en un minuto deben terminar en `429`.

## Probarlo en tu máquina (WSO2 en Docker + servicios en el k3s de `facturacion-despliegue/pruebas-locales`)

```bash
docker compose -f wso2/docker-compose.yml up -d            # ~2 min hasta "healthy"; consola https://localhost:9443 (admin / admin)
bash wso2/scripts/exponer-servicios-k3s.sh &               # puertos 18091..18097 hacia los 7 servicios (mantén la ventana abierta)
WSO2_ADMIN_CLAVE=admin perl wso2/scripts/configurar-wso2.pl desarrollo-local
```

Termina con pruebas de humo y deja la api-key de WSO2 en `wso2/.local/api-key-desarrollo-local.txt` (fuera de git). Después:

```bash
G=https://localhost:8243/identidad/api/v1 ; K=$(cat wso2/.local/api-key-desarrollo-local.txt)
curl -k -H "ApiKey: $K" -H "Content-Type: application/json" -X POST $G/usuarios/login -d '{"email":"...","password":"..."}'   # -> token de sesión
curl -k -H "ApiKey: $K" -H "Authorization: Bearer <token de sesión>" $G/usuarios/me
```

El gateway escucha en **8243** (https) y **8280** (http). Publisher: `https://localhost:9443/publisher`; Developer Portal: `/devportal`.
Otro cliente: `APLICACION_NOMBRE=otro-cliente` crea su aplicación y guarda su api-key en `wso2/.local/api-key-<ambiente>-otro-cliente.txt`. `--rotar` genera otra api-key de WSO2. El WSO2 de desarrollo no guarda nada entre `down` y `up`: vuelve a correr `configurar-wso2.pl --rotar` (la api-key anterior es de la instalación borrada).

## En un ambiente real (pruebas, producción)

1. Completa `env/pruebas.env` o `env/produccion.env` (los `CAMBIAR-...`: URL de WSO2, del gateway y usuario administrador). La clave del
   administrador **no** va en el archivo: `WSO2_ADMIN_CLAVE=... perl wso2/scripts/configurar-wso2.pl pruebas`.
2. `BACKEND_BASE` es el host del Ingress (`facturacion-pruebas.<dominio>`), sin puertos por servicio.
3. En el Ingress de ese ambiente (`facturacion-despliegue/overlays/<ambiente>/ingress.yaml`), `whitelist-source-range` debe ser **solo las IP
   del gateway**; hoy tiene todas las redes privadas.
4. Los límites son un punto de partida: ajústalos con tráfico real en `env/<ambiente>.env` y vuelve a ejecutar el script.

## Mantenimiento

- **Cambió un endpoint** (se agregó, quitó o renombró): `perl wso2/scripts/generar-contrato.pl --ingress ../facturacion-despliegue/overlays/desarrollo ../facturacion-despliegue/overlays/pruebas ../facturacion-despliegue/overlays/produccion`,
  commitea `wso2/openapi` **y** los tres `ingress.yaml`, y vuelve a correr `configurar-wso2.pl`.
- **`/q/health`, `/q/metrics` y swagger** cuelgan de la ruta base de cada servicio. No se publican en el gateway (no están en el contrato) ni en el
  Ingress (las rutas son por recurso; los dos servicios con recursos en la raíz, `empresas` y `sagas`, usan una regla regex que incluye la ruta base y excluye `/q`).
  Las sondas de Kubernetes y Prometheus siguen funcionando porque van directo al pod.
- **Rotar la api-key de un cliente**: `--rotar` entrega una nueva; la anterior vale hasta que venza (`API_KEY_VIGENCIA_SEG`) o se revoque en el Developer Portal.

## Lo que NO está probado

- Un WSO2 de producción real (clúster, base propia, TLS verdadero, Vault): se probó la imagen de desarrollo 4.3.0.
- La causa de que los límites dejen de aplicarse al reiniciar el contenedor de desarrollo.
- Límites por IP: no están configurados. Los límites por usuario y por empresa no van aquí: los aplica cada microservicio, configurados en su base (`facturacion-electronica-ec/ARQUITECTURA.md` 10.6).
