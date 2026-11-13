-- El correo es el identificador de login y hasta ahora distinguía mayúsculas: "Ana@x.com" y "ana@x.com" eran dos
-- cuentas distintas. El servicio ahora guarda y busca el correo en minúsculas y sin espacios; esto alinea los datos
-- existentes y deja a la base como garantía final (si dos filas chocaran al normalizar, la migración falla a la vista).
update usuario set email = lower(trim(email));
create unique index uq_usuario_email_lower on usuario (lower(email));
