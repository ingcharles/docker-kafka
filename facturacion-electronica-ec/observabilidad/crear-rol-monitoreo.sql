-- Rol de solo lectura para el exportador de Postgres (observabilidad). Se corre una vez, como administrador, en la base
-- "facturacion":
--
--   psql -U postgres -d facturacion -v clave="'LA-CLAVE'" -f observabilidad/crear-rol-monitoreo.sql
--
-- La clave es OBSERVABILIDAD_DB_PASSWORD (env/<ambiente>.secretos.env); no la escribas en este archivo.
--
-- pg_monitor da acceso a las vistas de estadísticas de Postgres (conexiones, bloqueos, tamaño) sin leer datos de las
-- tablas. Las métricas de negocio (observabilidad/postgres-exporter/queries.yaml) solo cuentan filas, así que el
-- permiso de SELECT se concede por COLUMNA y únicamente sobre las que se usan: el rol no puede leer datos personales.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'observabilidad') THEN
        CREATE ROLE observabilidad LOGIN;
    END IF;
END
$$;

ALTER ROLE observabilidad PASSWORD :clave;
ALTER ROLE observabilidad CONNECTION LIMIT 5;
ALTER ROLE observabilidad SET statement_timeout = '10s';
GRANT pg_monitor TO observabilidad;

GRANT USAGE ON SCHEMA orquestacion, comprobantes, firma_electronica, inventario, notificacion, sri TO observabilidad;
GRANT SELECT (state) ON orquestacion.saga_instance TO observabilidad;
GRANT SELECT (estado, fecha_expiracion_certificado) ON firma_electronica.certificado_digital TO observabilidad;
GRANT SELECT (published_at) ON orquestacion.outbox_event TO observabilidad;
GRANT SELECT (published_at) ON comprobantes.outbox_event TO observabilidad;
GRANT SELECT (published_at) ON firma_electronica.outbox_event TO observabilidad;
GRANT SELECT (published_at) ON inventario.outbox_event TO observabilidad;
GRANT SELECT (published_at) ON notificacion.outbox_event TO observabilidad;
GRANT SELECT (published_at) ON sri.outbox_event TO observabilidad;
