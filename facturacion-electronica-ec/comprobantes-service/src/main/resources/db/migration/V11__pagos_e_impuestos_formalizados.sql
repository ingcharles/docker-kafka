-- Formaliza pagos_json (comprobante) e impuestos_json (detalle_comprobante) en
-- tablas con columnas tipadas. Corre contra el esquema "comprobantes".
--
-- Antes: dos columnas de texto con un arreglo JSON, que no se podían filtrar,
-- sumar, indexar ni proteger con constraints (p. ej. "total de ventas por
-- forma de pago" obligaba a parsear JSON fila por fila).
-- Ahora: catálogo forma_pago (Tabla 24 del SRI) + comprobante_pago +
-- detalle_impuesto, con FK, tipos numéricos y checks. Los datos existentes se
-- migran desde el JSON antes de eliminar las columnas.

-- 1) Catálogo de formas de pago — Tabla 24 de la ficha técnica del SRI
create table forma_pago (
    codigo         varchar(2) primary key,
    descripcion    varchar(100) not null,
    vigente_desde  date not null,
    vigente_hasta  date
);

insert into forma_pago (codigo, descripcion, vigente_desde) values
    ('01', 'SIN UTILIZACION DEL SISTEMA FINANCIERO',          date '2013-01-01'),
    ('15', 'COMPENSACION DE DEUDAS',                           date '2013-01-01'),
    ('16', 'TARJETA DE DEBITO',                                date '2016-06-01'),
    ('17', 'DINERO ELECTRONICO',                               date '2016-06-01'),
    ('18', 'TARJETA PREPAGO',                                  date '2016-06-01'),
    ('19', 'TARJETA DE CREDITO',                               date '2016-06-01'),
    ('20', 'OTROS CON UTILIZACION DEL SISTEMA FINANCIERO',     date '2016-06-01'),
    ('21', 'ENDOSO DE TITULOS',                                date '2016-06-01');

-- 2) Pagos del comprobante (Factura, Nota de Débito, Liquidación de Compra)
create table comprobante_pago (
    id              uuid primary key,
    comprobante_id  uuid not null references comprobante (id) on delete cascade,
    orden           integer not null,
    forma_pago      varchar(2) not null,
    total           numeric(14, 2) not null check (total >= 0),
    -- plazo/unidad_tiempo solo aplican a pago a crédito (XSD: plazo decimal
    -- 14,2; unidadTiempo texto 1-10). Ambos van juntos o ninguno.
    plazo           numeric(14, 2) check (plazo >= 0),
    unidad_tiempo   varchar(10),
    constraint uq_comprobante_pago_orden unique (comprobante_id, orden)
);

-- 3) Impuestos de cada línea de detalle. En Comprobante de Retención cada
--    "impuesto" es una línea de retención: codigo_porcentaje queda null y se
--    llenan codigo_retencion y los doc_sustento_*.
create table detalle_impuesto (
    id                   uuid primary key,
    detalle_id           uuid not null references detalle_comprobante (id) on delete cascade,
    orden                integer not null,
    codigo               varchar(2) not null,
    codigo_porcentaje    varchar(10),
    tarifa               numeric(7, 2) not null,
    -- Sin redondear a 2 decimales a propósito: es cantidad*precio-descuento
    -- (hasta 8 decimales) y los totales de encabezado se suman sobre este
    -- valor sin redondear antes de formatear, igual que antes con el JSON.
    base_imponible       numeric(22, 8) not null,
    valor                numeric(14, 2) not null,
    codigo_retencion     varchar(10),
    doc_sustento_codigo  varchar(2),
    doc_sustento_numero  varchar(20),
    doc_sustento_fecha   date,
    constraint uq_detalle_impuesto_orden unique (detalle_id, orden)
);

-- 4) Migración de datos existentes desde el JSON
insert into comprobante_pago (id, comprobante_id, orden, forma_pago, total, plazo, unidad_tiempo)
select gen_random_uuid(),
       c.id,
       (p.ord - 1)::integer,
       p.pago ->> 'formaPago',
       (p.pago ->> 'total')::numeric,
       nullif(p.pago ->> 'plazo', '')::numeric,
       nullif(p.pago ->> 'unidadTiempo', '')
from comprobante c
cross join lateral jsonb_array_elements(c.pagos_json::jsonb) with ordinality as p (pago, ord)
where c.pagos_json is not null
  and jsonb_typeof(c.pagos_json::jsonb) = 'array';

insert into detalle_impuesto (id, detalle_id, orden, codigo, codigo_porcentaje, tarifa, base_imponible, valor,
                              codigo_retencion, doc_sustento_codigo, doc_sustento_numero, doc_sustento_fecha)
select gen_random_uuid(),
       d.id,
       (i.imp - 1)::integer,
       i.impuesto ->> 'codigo',
       i.impuesto ->> 'codigoPorcentaje',
       (i.impuesto ->> 'tarifa')::numeric,
       (i.impuesto ->> 'baseImponible')::numeric,
       (i.impuesto ->> 'valor')::numeric,
       i.impuesto ->> 'codigoRetencion',
       i.impuesto ->> 'docSustentoCodigo',
       i.impuesto ->> 'docSustentoNumero',
       to_date(i.impuesto ->> 'docSustentoFecha', 'DD/MM/YYYY')
from detalle_comprobante d
cross join lateral jsonb_array_elements(d.impuestos_json::jsonb) with ordinality as i (impuesto, imp)
where jsonb_typeof(d.impuestos_json::jsonb) = 'array';

-- 5) Constraints que podrían no cumplir datos históricos se agregan NOT VALID:
--    se exigen a toda fila nueva o modificada sin bloquear la migración por
--    filas viejas (el XSD del SRI aceptaba códigos 02-14 que no están en la
--    Tabla 24, y plazo/unidadTiempo eran independientes). Para validarlas
--    cuando se haya depurado el histórico: ALTER TABLE ... VALIDATE CONSTRAINT.
alter table comprobante_pago
    add constraint fk_comprobante_pago_forma_pago foreign key (forma_pago) references forma_pago (codigo) not valid;
alter table comprobante_pago
    add constraint chk_comprobante_pago_plazo_unidad check ((plazo is null) = (unidad_tiempo is null)) not valid;

-- (comprobante_id y detalle_id ya quedan indexados por el prefijo de los unique (.., orden)).
create index idx_comprobante_pago_forma_pago on comprobante_pago (forma_pago);

-- 6) Ya con todo migrado, se eliminan las columnas JSON
alter table comprobante drop column pagos_json;
alter table detalle_comprobante drop column impuestos_json;
