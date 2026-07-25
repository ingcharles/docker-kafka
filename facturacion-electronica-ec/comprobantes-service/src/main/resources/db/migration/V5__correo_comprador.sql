-- notificacion-service necesita a quién mandarle el comprobante autorizado
-- por correo — hasta ahora nada capturaba el email del receptor. Nullable:
-- si no se da, notificacion-service simplemente no manda nada para ese
-- comprobante (no es un error, es una decisión válida del llamador).
alter table comprobante add column comprador_correo varchar(300);
