-- Una api-key puede acotarse opcionalmente a un establecimiento (y,
-- opcionalmente también, a un punto de emisión dentro de él) — para
-- empresas con varias sucursales/cajas que no quieren que una key de un
-- punto de venta pueda emitir con el de otro. Nullable: la mayoría de las
-- api-keys seguirán sin acotar (sin restricción, comportamiento actual
-- sin cambios) — ver ApiKeyService.crear.
alter table api_key add column establecimiento varchar(3);
alter table api_key add column punto_emision varchar(3);
