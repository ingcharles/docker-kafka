-- Logo de la empresa para el RIDE (PNG o JPEG, hasta 200 KB). Vive en la propia tabla empresa; al subirlo o quitarlo
-- se publica como ajuste "ride.logo" (base64) por el mismo tópico de configuración que ya consume comprobantes-service.
alter table empresa add column logo bytea;
alter table empresa add column logo_tipo varchar(20);
