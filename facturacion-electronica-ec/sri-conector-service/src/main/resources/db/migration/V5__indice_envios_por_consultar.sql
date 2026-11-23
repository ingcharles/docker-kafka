-- El poller de autorización reclama los envíos RECIBIDO que no se consultan desde hace un rato, los más antiguos primero
-- (ver EnvioSriPanacheRepository.findRecibidosParaConsultar): este índice parcial cubre justo esa consulta y solo pesa por los
-- envíos que esperan al SRI.
create index idx_envio_sri_por_consultar on envio_sri (ultima_consulta_en nulls first, creado_en) where estado = 'RECIBIDO';
