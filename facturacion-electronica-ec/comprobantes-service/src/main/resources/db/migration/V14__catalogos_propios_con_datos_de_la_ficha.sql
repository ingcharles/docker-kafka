-- Los catálogos del SRI dejan de ser réplicas (V12, alimentadas por un servicio aparte) y pasan a ser tablas PROPIAS de
-- este servicio: se siembran aquí con los datos de la Ficha Técnica v2.34 y se mantienen con la API /catalogos.
-- Corre contra el esquema "comprobantes". Idempotente: si la tabla ya trae filas (de la época de las réplicas),
-- los INSERT no las pisan (on conflict do nothing) y las restricciones nuevas solo se agregan.
--
-- Convención de vigencia: vigente_desde NOT NULL con 1900-01-01 = "la ficha no publica fecha de inicio";
-- vigente_hasta NULL = sin fecha de fin. Un registro aplica en la fecha F si está activo y
-- vigente_desde <= F y (vigente_hasta es null o vigente_hasta >= F).

-- Tabla 16: códigos de impuestos
insert into impuesto (codigo, descripcion) values
    ('2', 'IVA'),
    ('3', 'ICE'),
    ('5', 'IRBPNR')
on conflict do nothing;

-- Tabla 17: tarifa del IVA. La ficha no publica fechas de vigencia de estas tarifas; se cargan sin fecha y se acotan
-- con PUT /catalogos/tarifa_impuesto cuando el SRI cambie una. No incluye el ICE (Tabla 18): su tabla en la ficha no
-- es utilizable y sus tarifas siguen viajando por ítem y por request.
insert into tarifa_impuesto (impuesto_codigo, codigo_porcentaje, descripcion, tarifa) values
    ('2', '0',  'IVA 0%',                                  0),
    ('2', '2',  'IVA 12%',                                 12),
    ('2', '3',  'IVA 14%',                                 14),
    ('2', '4',  'IVA 15%',                                 15),
    ('2', '5',  'IVA 5%',                                  5),
    ('2', '6',  'No objeto de impuesto',                   0),
    ('2', '7',  'Exento de IVA',                           0),
    ('2', '8',  'IVA diferenciado (sector turístico, 8%)', 8),
    ('2', '10', 'IVA 13%',                                 13)
on conflict do nothing;

-- Tabla 6: tipo de identificación. solo_factura: la ficha indica que "venta a consumidor final" (07) no está
-- habilitada en liquidaciones de compra y que notas de crédito, notas de débito y retenciones deben identificar al
-- receptor con RUC, cédula, pasaporte o identificación del exterior.
insert into tipo_identificacion (codigo, descripcion, solo_factura) values
    ('04', 'RUC',                         false),
    ('05', 'CEDULA',                      false),
    ('06', 'PASAPORTE',                   false),
    ('07', 'VENTA A CONSUMIDOR FINAL',    true),
    ('08', 'IDENTIFICACION DEL EXTERIOR', false)
on conflict do nothing;

-- Tabla 19: impuesto a retener
insert into impuesto_retencion (codigo, descripcion) values
    ('1', 'RENTA'),
    ('2', 'IVA'),
    ('6', 'ISD')
on conflict do nothing;

-- Tabla 20: códigos de retención. Solo los que la ficha publica completos: IVA e ISD. Los códigos de RENTA (303,
-- 304, ...) viven en el Catálogo del Anexo Transaccional Simplificado (ATS), que no está en la documentación del
-- proyecto: se cargan con PUT /catalogos/retencion desde el catálogo oficial del SRI. Mientras un impuesto no tenga
-- filas, solo se valida el formato del código de ese impuesto.
insert into retencion (impuesto_codigo, codigo, descripcion, porcentaje) values
    ('2', '9',  'Retención IVA 10%',                                    10),
    ('2', '10', 'Retención IVA 20%',                                    20),
    ('2', '1',  'Retención IVA 30%',                                    30),
    ('2', '11', 'Retención IVA 50%',                                    50),
    ('2', '2',  'Retención IVA 70%',                                    70),
    ('2', '3',  'Retención IVA 100%',                                   100),
    ('2', '7',  'Retención en cero (Res. NAC-DGERCGC15-00000284)',      0),
    ('2', '8',  'No procede retención',                                 0)
on conflict do nothing;

-- ISD: el código 4580 cambió de porcentaje varias veces; cada período es una fila. Fechas tal como las publica la ficha.
insert into retencion (impuesto_codigo, codigo, vigente_desde, vigente_hasta, descripcion, porcentaje) values
    ('6', '4580', date '1900-01-01', date '2021-12-31', 'Retención ISD 5%',    5.00),
    ('6', '4580', date '2022-01-01', date '2022-03-31', 'Retención ISD 4.75%', 4.75),
    ('6', '4580', date '2022-04-01', date '2022-06-30', 'Retención ISD 4.50%', 4.50),
    ('6', '4580', date '2022-07-01', date '2022-09-30', 'Retención ISD 4.25%', 4.25),
    ('6', '4580', date '2022-10-01', date '2022-12-31', 'Retención ISD 4.00%', 4.00),
    ('6', '4580', date '2023-02-01', date '2023-06-30', 'Retención ISD 3.75%', 3.75),
    ('6', '4580', date '2023-07-01', date '2024-03-31', 'Retención ISD 3.50%', 3.50)
on conflict do nothing;
insert into retencion (impuesto_codigo, codigo, vigente_desde, descripcion, porcentaje) values
    ('6', '4580', date '2024-04-01', 'Retención ISD 5% (Ley Orgánica para Enfrentar el Conflicto Armado Interno, RO 516)', 5.00),
    ('6', '4586', date '2025-05-01', 'Retención ISD 2.5% (Decreto Ejecutivo 589)', 2.50)
on conflict do nothing;

-- sustento_tributario (codSustento de la Retención v2.0.0): es del Catálogo ATS (tabla 5), que no está en la
-- documentación del proyecto. Queda vacía y se carga con PUT /catalogos/sustento_tributario; mientras esté vacía solo
-- se valida el formato (2 dígitos).

-- Restricciones que tenían las tablas del servicio de catálogos que ya no existe: al ser ahora las tablas dueñas,
-- la base debe garantizar la integridad que antes garantizaba el dueño.
alter table tarifa_impuesto add constraint fk_tarifa_impuesto_impuesto
    foreign key (impuesto_codigo) references impuesto (codigo);
alter table tarifa_impuesto add constraint chk_tarifa_impuesto_tarifa check (tarifa >= 0);
alter table tarifa_impuesto add constraint chk_tarifa_impuesto_vigencia
    check (vigente_hasta is null or vigente_hasta >= vigente_desde);

alter table retencion add constraint fk_retencion_impuesto_retencion
    foreign key (impuesto_codigo) references impuesto_retencion (codigo);
alter table retencion add constraint chk_retencion_porcentaje check (porcentaje >= 0 and porcentaje <= 100);
alter table retencion add constraint chk_retencion_vigencia
    check (vigente_hasta is null or vigente_hasta >= vigente_desde);

alter table forma_pago add constraint chk_forma_pago_vigencia
    check (vigente_hasta is null or vigente_hasta >= vigente_desde);
