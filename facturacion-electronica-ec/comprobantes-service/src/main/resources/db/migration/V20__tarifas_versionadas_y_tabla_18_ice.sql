-- Las tarifas de impuestos pasan a poder versionarse y a cubrir el ICE (Tabla 18) y el IRBPNR.
--
-- 1) La clave primaria era (impuesto, código): una sola fila por código, así que una tarifa que cambia de valor con
--    el mismo código (el ICE de los cigarrillos 3011 pasó de 0,17 a 0,16) obligaba a pisar la fila y perdía la
--    historia. Ahora la clave incluye vigente_desde: el cambio de normativa es una fila nueva (PUT /catalogos/tarifa_impuesto)
--    y la anterior se cierra con vigente_hasta; el comprobante usa la fila vigente a su fecha de emisión.
-- 2) tipo_tarifa distingue las dos columnas de la Tabla 18: PORCENTAJE (ad valorem, % sobre la base) y ESPECIFICA
--    (USD por unidad). Un código puede tener las dos (3031: 75 % y USD 10,00), una fila por tipo.
alter table tarifa_impuesto alter column descripcion type varchar(200);
alter table tarifa_impuesto add column tipo_tarifa varchar(10) not null default 'PORCENTAJE';
alter table tarifa_impuesto add constraint chk_tarifa_impuesto_tipo check (tipo_tarifa in ('PORCENTAJE', 'ESPECIFICA'));
alter table tarifa_impuesto drop constraint tarifa_impuesto_pkey;
alter table tarifa_impuesto add primary key (impuesto_codigo, codigo_porcentaje, tipo_tarifa, vigente_desde);

comment on column tarifa_impuesto.tipo_tarifa is 'PORCENTAJE = ad valorem (valor = base x tarifa / 100); ESPECIFICA = USD por unidad (valor = unidades x tarifa).';
comment on column tarifa_impuesto.tarifa is 'Valor de la tarifa: porcentaje (cero o mayor) si tipo_tarifa = PORCENTAJE, dólares por unidad si ESPECIFICA.';

-- Tabla 16/17: IRBPNR (botellas plásticas no retornables): USD 0,02 por botella (ejemplo oficial de la ficha:
-- 12000 botellas, tarifa 0.02, valor 240.00).
insert into tarifa_impuesto (impuesto_codigo, codigo_porcentaje, descripcion, tarifa, tipo_tarifa) values
    ('5', '5001', 'IRBPNR - botella plástica no retornable (USD por botella)', 0.02, 'ESPECIFICA')
on conflict do nothing;

-- Tabla 18: tarifa del ICE, transcrita de la ficha técnica. La ficha publica dos columnas de fechas: enero de 2023 y
-- febrero a diciembre de 2023; cuando difieren hay dos filas (hasta 2023-01-31 y desde 2023-02-01 sin fecha de fin), y
-- cuando son iguales una sola fila sin fechas. La ficha no publica valores posteriores a 2023: las tarifas específicas
-- se ajustan cada año, así que hay que actualizarlas con PUT /catalogos/tarifa_impuesto cuando el SRI las publique.
-- Códigos que la ficha deja en blanco (3542 y 3543, cigarrillos CAE) no se cargan.
insert into tarifa_impuesto (impuesto_codigo, codigo_porcentaje, descripcion, tarifa, tipo_tarifa, vigente_desde, vigente_hasta) values
    ('3', '3011', 'ICE Cigarrillos Rubios', 0.17, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3011', 'ICE Cigarrillos Rubios', 0.16, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3021', 'ICE Cigarrillos Negros', 0.17, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3021', 'ICE Cigarrillos Negros', 0.16, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3023', 'ICE Productos del Tabaco y Sucedáneos del Tabaco excepto Cigarrillos', 150, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3031', 'ICE Bebidas Alcohólicas', 75, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3031', 'ICE Bebidas Alcohólicas', 10.36, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3031', 'ICE Bebidas Alcohólicas', 10.00, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3041', 'ICE Cerveza Industrial (Gran, Mediana y Pequeña Escala)', 75, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3073', 'ICE Vehículos Motorizados cuyo PVP sea hasta de 20000 USD', 5, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3075', 'ICE Vehículos Motorizados PVP entre 30000 y 40000', 15, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3077', 'ICE Vehículos Motorizados cuyo PVP superior USD 40.000 hasta 50.000', 20, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3078', 'ICE Vehículos Motorizados cuyo PVP superior USD 50.000 hasta 60.000', 25, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3079', 'ICE Vehículos Motorizados cuyo PVP superior USD 60.000 hasta 70.000', 30, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3080', 'ICE Vehículos Motorizados cuyo PVP superior USD 70.000', 35, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3081', 'ICE Aviones, Tricares, yates, Barcos de Recreo', 15, 'PORCENTAJE', date '1900-01-01', date '2023-01-31'),
    ('3', '3081', 'ICE Aviones, Tricares, yates, Barcos de Recreo', 10, 'PORCENTAJE', date '2023-02-01', null),
    ('3', '3092', 'ICE Servicios de Televisión Prepagada', 0, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3610', 'ICE Perfumes y Aguas de Tocador', 20, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3620', 'ICE Videojuegos', 0, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3630', 'ICE Armas de Fuego, Armas deportivas y Municiones', 300, 'PORCENTAJE', date '1900-01-01', date '2023-01-31'),
    ('3', '3630', 'ICE Armas de Fuego, Armas deportivas y Municiones', 30, 'PORCENTAJE', date '2023-02-01', null),
    ('3', '3640', 'ICE Focos Incandescentes', 100, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3660', 'ICE Cuotas Membresías Afiliaciones Acciones', 35, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3093', 'ICE Servicios Telefonía Sociedades', 15, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3101', 'ICE Bebidas Energizantes', 10, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3053', 'ICE Bebidas Gaseosas con Alto Contenido de Azúcar (tarifa por 100 g de azúcar)', 0.19, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3053', 'ICE Bebidas Gaseosas con Alto Contenido de Azúcar (tarifa por 100 g de azúcar)', 0.18, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3054', 'ICE Bebidas Gaseosas con Bajo Contenido de Azúcar', 10, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3111', 'ICE Bebidas No Alcohólicas (tarifa por 100 g de azúcar)', 0.19, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3111', 'ICE Bebidas No Alcohólicas (tarifa por 100 g de azúcar)', 0.18, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3043', 'ICE Cerveza Artesanal', 1.55, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3043', 'ICE Cerveza Artesanal', 1.50, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3033', 'ICE Alcohol', 75, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3033', 'ICE Alcohol', 10.36, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3033', 'ICE Alcohol', 10.00, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3671', 'ICE calefones y sistemas de calentamiento de agua a gas SRI', 100, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3684', 'ICE vehículos motorizados camionetas y de rescate cuyo PVP sea hasta DE 30.000 USD', 5, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3686', 'ICE vehículos motorizados excepto camionetas y de rescate cuyo PVP sea superior USD 20.000 hasta DE 30.000', 10, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3688', 'ICE vehículos híbridos cuyo PVP sea de hasta USD 35.000', 0, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3691', 'ICE vehículos híbridos cuyo PVP superior USD 35.000 hasta 40.000', 8, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3692', 'ICE vehículos híbridos cuyo PVP superior USD 40.000 hasta 50.000', 14, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3695', 'ICE vehículos híbridos cuyo PVP superior USD 50.000 hasta 60.000', 20, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3696', 'ICE vehículos híbridos cuyo PVP superior USD 60.000 hasta 70.000', 26, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3698', 'ICE vehículos híbridos cuyo PVP superior a USD 70.000', 32, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3682', 'ICE consumibles tabaco calentado y líquidos con nicotina SRI', 150, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3681', 'ICE servicios de telefonía móvil personas naturales', 0, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3680', 'ICE fundas plásticas', 0.10, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3680', 'ICE fundas plásticas', 0.08, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3533', 'ICE Import. Bebidas Alcohólicas', 75, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3541', 'ICE Cerveza Industrial Gran, Mediana y Pequeña Escala CAE', 75, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3544', 'ICE Productos del Tabaco y Sucedáneos Excepto Cigarrillos CAE', 150, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3581', 'ICE Aeronaves CAE', 15, 'PORCENTAJE', date '1900-01-01', date '2023-01-31'),
    ('3', '3581', 'ICE Aeronaves CAE', 10, 'PORCENTAJE', date '2023-02-01', null),
    ('3', '3582', 'ICE Aviones, Avionetas y Helicópteros Exct. Aquellos destinados Al Trans. CAE', 15, 'PORCENTAJE', date '1900-01-01', date '2023-01-31'),
    ('3', '3582', 'ICE Aviones, Avionetas y Helicópteros Exct. Aquellos destinados Al Trans. CAE', 10, 'PORCENTAJE', date '2023-02-01', null),
    ('3', '3710', 'ICE Perfumes y Aguas de Tocador CAE', 20, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3720', 'ICE Video Juegos CAE', 35, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3730', 'ICE Importaciones Armas de Fuego, Armas deportivas y Municiones CAE', 300, 'PORCENTAJE', date '1900-01-01', date '2023-01-31'),
    ('3', '3730', 'ICE Importaciones Armas de Fuego, Armas deportivas y Municiones CAE', 30, 'PORCENTAJE', date '2023-02-01', null),
    ('3', '3740', 'ICE Focos Incandescentes CAE', 100, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3871', 'ICE vehículos motorizados cuyo PVP sea hasta de 20000 USD SENAE', 5, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3873', 'ICE vehículos motorizados PVP entre 30000 y 40000 SENAE', 15, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3874', 'ICE vehículos motorizados cuyo PVP superior USD 40.000 hasta 50.000 SENAE', 20, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3875', 'ICE vehículos motorizados cuyo PVP superior USD 50.000 hasta 60.000 SENAE', 25, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3876', 'ICE vehículos motorizados cuyo PVP superior USD 60.000 hasta 70.000 SENAE', 30, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3877', 'ICE vehículos motorizados cuyo PVP superior USD 70.000 SENAE', 35, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3878', 'ICE Aviones, Tricars, Yates, Barcos de Recreo SENAE', 15, 'PORCENTAJE', date '1900-01-01', date '2023-01-31'),
    ('3', '3878', 'ICE Aviones, Tricars, Yates, Barcos de Recreo SENAE', 10, 'PORCENTAJE', date '2023-02-01', null),
    ('3', '3601', 'ICE Bebidas Energizantes SENAE', 10, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3552', 'ICE bebidas gaseosas con alto contenido de azúcar SENAE (tarifa por 100 g de azúcar)', 0.19, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3552', 'ICE bebidas gaseosas con alto contenido de azúcar SENAE (tarifa por 100 g de azúcar)', 0.18, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3553', 'ICE bebidas gaseosas con bajo contenido de azúcar SENAE', 10, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3602', 'ICE bebidas no alcohólicas SENAE (tarifa por 100 g de azúcar)', 0.19, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3602', 'ICE bebidas no alcohólicas SENAE (tarifa por 100 g de azúcar)', 0.18, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3545', 'ICE cerveza artesanal SENAE', 75, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3545', 'ICE cerveza artesanal SENAE', 1.55, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3545', 'ICE cerveza artesanal SENAE', 1.50, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3532', 'ICE Import. alcohol SENAE', 75, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3532', 'ICE Import. alcohol SENAE', 10.36, 'ESPECIFICA', date '1900-01-01', date '2023-01-31'),
    ('3', '3532', 'ICE Import. alcohol SENAE', 10.00, 'ESPECIFICA', date '2023-02-01', null),
    ('3', '3771', 'ICE calefones y sistemas de calentamiento de agua a gas SENAE', 100, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3685', 'ICE vehículos motorizados camionetas y de rescate PVP sea hasta DE 30.000 USD SENAE', 5, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3687', 'ICE vehículos motorizados excepto camionetas y de rescate PVP sea superior USD 20.000 hasta DE 30.000 SENAE', 10, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3689', 'ICE vehículos híbridos cuyo PVP sea de hasta USD 35.000 SENAE', 0, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3690', 'ICE vehículos híbridos cuyo PVP superior USD 35.000 hasta 40.000 SENAE', 8, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3693', 'ICE vehículos híbridos cuyo PVP superior USD 40.000 hasta 50.000 SENAE', 14, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3694', 'ICE vehículos híbridos cuyo PVP superior USD 50.000 hasta 60.000 SENAE', 20, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3697', 'ICE vehículos híbridos cuyo PVP superior USD 60.000 hasta 70.000 SENAE', 26, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3699', 'ICE vehículos híbridos cuyo PVP superior a USD 70.000 SENAE', 32, 'PORCENTAJE', date '1900-01-01', null),
    ('3', '3683', 'ICE consumibles tabaco calentado y líquidos con nicotina SENAE', 150, 'PORCENTAJE', date '1900-01-01', date '2023-01-31'),
    ('3', '3683', 'ICE consumibles tabaco calentado y líquidos con nicotina SENAE', 50, 'PORCENTAJE', date '2023-02-01', null)
on conflict do nothing;
