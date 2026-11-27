#!/usr/bin/env perl
# Configura WSO2 API Manager para facturación electrónica a partir de archivos del repositorio (configuración como código):
#
#   1. Políticas de throttling (límites por minuto de wso2/env/<ambiente>.env): aplicación, suscripción, tope total de las operaciones
#      sensibles y su cupo por aplicación (reglas personalizadas).
#   2. Una API por servicio (contrato wso2/openapi/<servicio>.yaml, SOLO rutas de negocio: /q/health y /q/metrics no se publican).
#      Seguridad del gateway = api-key de WSO2 (cabecera ApiKey). La cabecera Authorization NO se toca: viaja al servicio, que valida su
#      propia api-key o sesión (identidad-service). Se despliega en el gateway y se publica.
#   3. Una aplicación cliente suscrita a todas las APIs y su api-key de WSO2 (wso2/.local/api-key-<ambiente>.txt, fuera de git).
#   4. Pruebas de humo contra el gateway.
#
# Es idempotente: se puede repetir; crea lo que falta y actualiza lo que existe (endpoint, contrato, límites).
#
# Uso:  perl wso2/scripts/configurar-wso2.pl [ambiente] [--rotar]       ambiente: desarrollo-local (predeterminado), pruebas o produccion
#       --rotar    genera una api-key de WSO2 nueva para la aplicación cliente (la anterior sigue válida hasta que venza o se revoque)
#       WSO2_ADMIN_CLAVE=... perl wso2/scripts/configurar-wso2.pl pruebas   (la clave del administrador se pasa por entorno, no por archivo)
#       APLICACION_NOMBRE=otro-cliente perl wso2/scripts/configurar-wso2.pl   (crea otra aplicación con su propia api-key en
#                                      wso2/.local/api-key-<ambiente>-otro-cliente.txt)
use strict;
use warnings;
use utf8;
use File::Basename qw(dirname);
use Cwd qw(abs_path);
use JSON::PP;

binmode(STDOUT, ':encoding(UTF-8)');
binmode(STDERR, ':encoding(UTF-8)');
$| = 1;

my $aqui  = abs_path(dirname(__FILE__));
my $wso2  = abs_path("$aqui/..");
my $local = "$wso2/.local";
mkdir $local unless -d $local;

my ($ambiente, $rotar) = ('desarrollo-local', 0);
for my $a (@ARGV) {
    if    ($a eq '--rotar')       { $rotar = 1 }
    elsif ($a =~ /^(-h|--help)$/) { open(my $f, '<', __FILE__); print grep { /^#/ } <$f>; exit 0 }
    elsif ($a !~ /^-/)            { $ambiente = $a }
    else                          { die "Argumento desconocido: $a\n" }
}

# ----------------------------------------------------------------------------------------------------------------- configuración
my %E;
my $archivo_env = "$wso2/env/$ambiente.env";
open(my $fh, '<:encoding(UTF-8)', $archivo_env) or die "No existe $archivo_env (ambientes: desarrollo-local, pruebas, produccion)\n";
while (<$fh>) { s/\r?\n$//; next if /^\s*(#|$)/; $E{$1} = $2 if /^([A-Z0-9_]+)=(.*)$/; }
close $fh;
my $aplicacion_del_ambiente = $E{APLICACION_NOMBRE};   # antes de que el entorno la cambie
$E{$_} = $ENV{$_} for grep { defined $ENV{$_} } keys %E, 'WSO2_ADMIN_CLAVE';
for my $k (qw(WSO2_URL_ADMIN WSO2_URL_GATEWAY WSO2_ADMIN_USUARIO BACKEND_BASE)) {
    die "$k sin definir en $archivo_env\n" unless length($E{$k} // '');
    die "$k todavía tiene el marcador CAMBIAR-... en $archivo_env\n" if $E{$k} =~ /CAMBIAR-/;
}
die "Falta la clave del administrador de WSO2: define WSO2_ADMIN_CLAVE en el entorno\n" unless length($E{WSO2_ADMIN_CLAVE} // '');
my %puerto = map { split /=/, $_, 2 } split ' ', ($E{BACKEND_PUERTOS} // '');

my $j = JSON::PP->new->utf8->canonical;
my $n_tmp = 0;
chdir $local or die "No puedo entrar a $local\n";   # los archivos temporales y @archivo de curl van relativos (curl de Windows no entiende /tmp)

# ----------------------------------------------------------------------------------------------------------------------------- HTTP
# http(METODO, URL, token => ..., basico => "u:p", json => {...}, form => [...]) -> ($codigo, $datos|undef, $crudo)
sub http {
    my ($metodo, $url, %o) = @_;
    my @a = ('-sk', '-m', '90', '-X', $metodo, '-w', "\n%{http_code}");
    push @a, '-H', "Authorization: Bearer $o{token}" if $o{token};
    push @a, '-u', $o{basico} if $o{basico};
    push @a, '-H', $_ for @{ $o{headers} // [] };
    push @a, '--data-urlencode', "$_->[0]=$_->[1]" for @{ $o{urlenc} // [] };
    my @tmp;
    if (defined $o{json}) {
        my $f = "tmp-" . $$ . "-" . ++$n_tmp . ".json"; push @tmp, $f;
        open(my $o, '>', $f) or die; print $o $j->encode($o{json}); close $o;
        push @a, '-H', 'Content-Type: application/json', '--data-binary', "\@$f";
    }
    for my $campo (@{ $o{form} // [] }) {
        my ($k, $v) = @$campo;
        if (ref $v) {   # { json => {...} } valor JSON como campo de formulario; { archivo => ruta }
            if ($v->{json}) {
                my $f = "tmp-" . $$ . "-" . ++$n_tmp . ".json"; push @tmp, $f;
                open(my $o, '>', $f) or die; print $o $j->encode($v->{json}); close $o;
                push @a, '-F', "$k=<$f";
            } else { push @a, '-F', "$k=\@$v->{archivo}"; }
        } else { push @a, '-F', "$k=$v"; }
    }
    push @a, $url;
    open(my $h, '-|', 'curl', @a) or die "No pude ejecutar curl\n";
    my $s = do { local $/; <$h> }; close $h;
    unlink @tmp;
    my ($cuerpo, $cod) = ($s // '') =~ /\A(.*)\n(\d{3})\z/s;
    $cod //= 0; $cuerpo //= '';
    my $datos = eval { $j->decode($cuerpo) } if length $cuerpo;
    return ($cod, $datos, $cuerpo);
}
sub exigir {   # exigir("lo que se hacía", [codigos ok], http(...)) -> datos
    my ($que, $ok, $cod, $datos, $crudo) = @_;
    return $datos if grep { $_ == $cod } @$ok;
    my $msg = ref $datos eq 'HASH' ? join(' | ', grep { defined } $datos->{message}, $datos->{description}, map { $_->{description} } @{ $datos->{error} // [] }) : substr($crudo, 0, 300);
    die "FALLÓ $que (HTTP $cod): $msg\n";
}
sub paso { print "== @_\n" }
sub ok   { print "   @_\n" }

# -------------------------------------------------------------------------------------------------------------------------- token
my $ADMIN = $E{WSO2_URL_ADMIN};
my @ambitos = qw(apim:api_create apim:api_publish apim:api_view apim:api_manage apim:api_delete apim:admin apim:tier_manage apim:admin_tier_view
                 apim:policy_manage apim:subscribe apim:app_manage apim:sub_manage apim:api_key apim:subscription_view apim:subscription_manage);
paso "WSO2 $ADMIN: cliente OAuth y token de administración";
my $basico = "$E{WSO2_ADMIN_USUARIO}:$E{WSO2_ADMIN_CLAVE}";
my $nombre_cliente = "facturacion-configurador";
my ($c, $d, $crudo) = http('POST', "$ADMIN/client-registration/v0.17/register", basico => $basico,
        json => { clientName => $nombre_cliente, owner => $E{WSO2_ADMIN_USUARIO}, grantType => 'password refresh_token', saasApp => JSON::PP::true });
exigir('registrar el cliente OAuth (¿clave del administrador incorrecta?)', [200, 201], $c, $d, $crudo);   # WSO2 devuelve el mismo cliente si el nombre ya existe
my ($cid, $csec) = ($d->{clientId}, $d->{clientSecret});
($c, $d, $crudo) = http('POST', "$ADMIN/oauth2/token", basico => "$cid:$csec",
    urlenc => [[grant_type => 'password'], [username => $E{WSO2_ADMIN_USUARIO}], [password => $E{WSO2_ADMIN_CLAVE}], [scope => join(' ', @ambitos)]]);
my $token = exigir('pedir el token', [200], $c, $d, $crudo)->{access_token};
ok "token de " . length($token) . " caracteres";
my %t = (token => $token);

# ---------------------------------------------------------------------------------------------------------------------- políticas
my $PUB = "$ADMIN/api/am/publisher/v4";
my $ADM = "$ADMIN/api/am/admin/v4/throttling/policies";
my $DEV = "$ADMIN/api/am/devportal/v3";
my %POL = (aplicacion => 'FacturacionAplicacion', suscripcion => 'FacturacionSuscripcion', sensible => 'FacturacionSensible', emision => 'FacturacionEmision');

sub limite { my ($v) = @_; return { type => 'REQUESTCOUNTLIMIT', requestCount => { timeUnit => 'min', unitTime => 1, requestCount => $v + 0 } } }
sub asegurar_politica {
    my ($tipo, $nombre, $descripcion, $por_min) = @_;
    die "Límite inválido para $nombre: '$por_min'\n" unless defined $por_min && $por_min =~ /^\d+$/ && $por_min > 0;
    my %cuerpo = (policyName => $nombre, displayName => $nombre, description => $descripcion, defaultLimit => limite($por_min));
    $cuerpo{conditionalGroups} = [] if $tipo eq 'advanced';
    @cuerpo{qw(stopOnQuotaReach billingPlan subscriberCount rateLimitCount rateLimitTimeUnit)} = (JSON::PP::true, 'FREE', 0, 0, 'sec') if $tipo eq 'subscription';
    my ($c, $d, $crudo) = http('GET', "$ADM/$tipo", %t);
    my ($existente) = grep { $_->{policyName} eq $nombre } @{ exigir("listar políticas $tipo", [200], $c, $d, $crudo)->{list} };
    if ($existente) {
        $cuerpo{policyId} = $existente->{policyId};
        ($c, $d, $crudo) = http('PUT', "$ADM/$tipo/$existente->{policyId}", %t, json => \%cuerpo);
        exigir("actualizar la política $nombre", [200], $c, $d, $crudo);
        ok "$nombre: $por_min/min (actualizada)";
    } else {
        ($c, $d, $crudo) = http('POST', "$ADM/$tipo", %t, json => \%cuerpo);
        exigir("crear la política $nombre", [200, 201], $c, $d, $crudo);
        ok "$nombre: $por_min/min (creada)";
    }
}

# Límite por cliente de una operación marcada: WSO2 cuenta las políticas de recurso en un solo contador para todos los clientes, así que el
# cupo de cada aplicación se hace con una regla personalizada (solo del super tenant) cuya clave incluye la aplicación y la operación.
# La política de recurso queda como tope total (la marca que la regla usa para elegir las operaciones).
sub asegurar_regla_por_cliente {
    my ($politica, $prefijo, $descripcion, $por_min) = @_;
    my $nombre = "${politica}PorCliente";
    die "Límite inválido para $nombre: '$por_min'\n" unless defined $por_min && $por_min =~ /^\d+$/ && $por_min > 0;
    my $consulta = join("\n",
        'FROM RequestStream',
        "SELECT messageID, (apiTenant == 'carbon.super' and resourceTier == '$politica') AS isEligible,",
        "       str:concat('$prefijo:', appId, ':', resourceKey) AS throttleKey",
        'INSERT INTO EligibilityStream;',
        '',
        'FROM EligibilityStream[isEligible==true]#throttler:timeBatch(1 min)',
        "SELECT throttleKey, (count(messageID) >= $por_min) AS isThrottled, expiryTimeStamp GROUP BY throttleKey",
        'INSERT ALL EVENTS INTO ResultStream;');
    my %cuerpo = (policyName => $nombre, displayName => $nombre, description => $descripcion,
                  keyTemplate => "$prefijo:\$appId:\$resourceKey", siddhiQuery => $consulta);
    my ($c, $d, $crudo) = http('GET', "$ADM/custom", %t);
    my ($existente) = grep { $_->{policyName} eq $nombre } @{ exigir('listar reglas personalizadas (requiere el super administrador)', [200], $c, $d, $crudo)->{list} };
    if ($existente) {
        $cuerpo{policyId} = $existente->{policyId};
        ($c, $d, $crudo) = http('PUT', "$ADM/custom/$existente->{policyId}", %t, json => \%cuerpo);
        exigir("actualizar la regla $nombre", [200], $c, $d, $crudo);
        ok "$nombre: $por_min/min por aplicación (actualizada)";
    } else {
        ($c, $d, $crudo) = http('POST', "$ADM/custom", %t, json => \%cuerpo);
        exigir("crear la regla $nombre", [200, 201], $c, $d, $crudo);
        ok "$nombre: $por_min/min por aplicación (creada)";
    }
}
paso "políticas de throttling";
asegurar_politica('application',  $POL{aplicacion},  'Facturación: tope de la aplicación cliente',        $E{LIMITE_APLICACION_POR_MIN});
asegurar_politica('subscription', $POL{suscripcion}, 'Facturación: tope por API suscrita',              $E{LIMITE_SUSCRIPCION_POR_MIN});
asegurar_politica('advanced',     $POL{sensible},    'Facturación: tope total de login, alta de usuarios, claves y api-keys', $E{LIMITE_SENSIBLE_TOTAL_POR_MIN});
asegurar_politica('advanced',     $POL{emision},     'Facturación: tope total de emisión de comprobantes', $E{LIMITE_EMISION_TOTAL_POR_MIN});
asegurar_regla_por_cliente($POL{sensible}, 'sensible', 'Facturación: login, alta de usuarios, claves y api-keys por aplicación', $E{LIMITE_SENSIBLE_POR_MIN});
asegurar_regla_por_cliente($POL{emision},  'emision',  'Facturación: emisión de comprobantes por aplicación',                    $E{LIMITE_EMISION_POR_MIN});

# Operaciones con límite propio (wso2/politicas.conf)
my @reglas;
open($fh, '<:encoding(UTF-8)', "$wso2/politicas.conf") or die "Falta wso2/politicas.conf\n";
while (<$fh>) {
    s/\r?\n$//; next if /^\s*(#|$)/;
    my ($svc, $verbo, $ruta, $pol) = split ' ';
    die "politicas.conf: línea inválida: $_\n" unless $pol && $POL{$pol};
    push @reglas, { svc => $svc, verbo => uc $verbo, ruta => $ruta, politica => $POL{$pol} };
}
close $fh;

# ------------------------------------------------------------------------------------------------------------------------- APIs
paso "entorno de despliegue del gateway";
($c, $d, $crudo) = http('GET', "$PUB/settings", %t);
my $entorno = (exigir('leer los entornos del gateway', [200], $c, $d, $crudo)->{environment} // [])->[0] or die "WSO2 no tiene ningún entorno de gateway\n";
my $vhost = $entorno->{vhosts}[0]{host} // 'localhost';
ok "entorno $entorno->{name}, vhost $vhost";

my @servicios;
open($fh, '<:encoding(UTF-8)', "$wso2/openapi/servicios.tsv") or die "Falta wso2/openapi/servicios.tsv: corre generar-contrato.pl\n";
while (<$fh>) { s/\r?\n$//; next unless length; my ($svc, $ctx) = split /\t/; push @servicios, [$svc, $ctx]; }
close $fh;

my %id_api;
sub endpoint_de { my ($svc, $ctx) = @_; my $p = $puerto{$svc}; return $E{BACKEND_BASE} . (defined $p ? ":$p" : '') . $ctx }

sub asegurar_api {
    my ($svc, $ctx) = @_;
    my ($contexto_base) = $ctx =~ m{^(.*)/v1$} or die "La ruta base de $svc no termina en /v1: $ctx\n";
    my $url_destino = endpoint_de($svc, $ctx);
    my $config_destino = { endpoint_type => 'http', production_endpoints => { url => $url_destino }, sandbox_endpoints => { url => $url_destino } };
    my ($c, $d, $crudo) = http('GET', "$PUB/apis?limit=100&query=" . "name:$svc", %t);
    my ($api) = grep { $_->{name} eq $svc && $_->{version} eq 'v1' } @{ exigir("buscar la API $svc", [200], $c, $d, $crudo)->{list} // [] };
    my $id;
    if ($api) {
        $id = $api->{id};
        ($c, $d, $crudo) = http('PUT', "$PUB/apis/$id/swagger", %t, form => [[apiDefinition => { archivo => "../openapi/$svc.yaml" }]]);
        exigir("actualizar el contrato de $svc", [200], $c, $d, $crudo);
    } else {
        my $props = { name => $svc, version => 'v1', context => $contexto_base, policies => [$POL{suscripcion}], securityScheme => ['api_key'], endpointConfig => $config_destino };
        ($c, $d, $crudo) = http('POST', "$PUB/apis/import-openapi", %t, form => [[file => { archivo => "../openapi/$svc.yaml" }], [additionalProperties => { json => $props }]]);
        $id = exigir("crear la API $svc", [200, 201], $c, $d, $crudo)->{id};
    }
    # Configuración: destino, seguridad (solo api-key de WSO2), suscripción y límite propio de las operaciones sensibles.
    ($c, $d, $crudo) = http('GET', "$PUB/apis/$id", %t);
    my $api_obj = exigir("leer la API $svc", [200], $c, $d, $crudo);
    $api_obj->{endpointConfig}  = $config_destino;
    $api_obj->{securityScheme}  = ['api_key', 'oauth_basic_auth_api_key_mandatory'];
    $api_obj->{policies}        = [$POL{suscripcion}];
    my ($especiales, $total) = (0, 0);
    for my $op (@{ $api_obj->{operations} }) {
        my $pol = 'Unlimited';
        for my $r (@reglas) {
            next unless $r->{svc} eq $svc && $r->{verbo} eq uc $op->{verb} && ($r->{ruta} eq '*' || $r->{ruta} eq $op->{target});
            $pol = $r->{politica}; $especiales++; last;
        }
        $op->{throttlingPolicy} = $pol; $total++;
    }
    ($c, $d, $crudo) = http('PUT', "$PUB/apis/$id", %t, json => $api_obj);
    my $actualizada = exigir("configurar la API $svc", [200], $c, $d, $crudo);

    # Revisión y despliegue en el gateway (WSO2 admite 5 revisiones: se borran las más viejas sin desplegar).
    ($c, $d, $crudo) = http('GET', "$PUB/apis/$id/revisions", %t);
    my @revs = sort { $a->{createdTime} <=> $b->{createdTime} } @{ exigir("listar revisiones de $svc", [200], $c, $d, $crudo)->{list} // [] };
    my @sin_desplegar = grep { !@{ $_->{deploymentInfo} // [] } } @revs;
    while (@revs >= 5 && @sin_desplegar) {
        my $v = shift @sin_desplegar; @revs = grep { $_->{id} ne $v->{id} } @revs;
        ($c, $d, $crudo) = http('DELETE', "$PUB/apis/$id/revisions/$v->{id}", %t); exigir("borrar una revisión vieja de $svc", [200, 204], $c, $d, $crudo);
    }
    ($c, $d, $crudo) = http('POST', "$PUB/apis/$id/revisions", %t, json => { description => "configurar-wso2.pl $ambiente" });
    my $rev = exigir("crear la revisión de $svc", [200, 201], $c, $d, $crudo)->{id};
    ($c, $d, $crudo) = http('POST', "$PUB/apis/$id/deploy-revision?revisionId=$rev", %t, json => [{ name => $entorno->{name}, vhost => $vhost, displayOnDevportal => JSON::PP::true }]);
    exigir("desplegar $svc en el gateway", [200, 201], $c, $d, $crudo);
    if (($actualizada->{lifeCycleStatus} // '') ne 'PUBLISHED') {
        ($c, $d, $crudo) = http('POST', "$PUB/apis/change-lifecycle?apiId=$id&action=Publish", %t);
        exigir("publicar $svc", [200], $c, $d, $crudo);
    }
    ok sprintf("%-34s %-22s %2d operaciones (%d con límite propio)  -> %s  [%s]", $svc, $ctx, $total, $especiales, $url_destino, $api ? 'actualizada' : 'creada');
    return $id;
}
paso "APIs (una por servicio)";
$id_api{$_->[0]} = asegurar_api(@$_) for @servicios;

# --------------------------------------------------------------------------------------------------------------------- aplicación
paso "aplicación cliente '$E{APLICACION_NOMBRE}' y suscripciones";
($c, $d, $crudo) = http('GET', "$DEV/applications?limit=100&query=$E{APLICACION_NOMBRE}", %t);
my ($app) = grep { $_->{name} eq $E{APLICACION_NOMBRE} } @{ exigir('listar aplicaciones', [200], $c, $d, $crudo)->{list} // [] };
my $cuerpo_app = { name => $E{APLICACION_NOMBRE}, throttlingPolicy => $POL{aplicacion}, tokenType => 'JWT', description => 'Cliente de facturación electrónica (api-key de WSO2)' };
if ($app) {
    ($c, $d, $crudo) = http('PUT', "$DEV/applications/$app->{applicationId}", %t, json => $cuerpo_app);
    exigir('actualizar la aplicación', [200], $c, $d, $crudo); ok "aplicación existente: política $POL{aplicacion}";
} else {
    ($c, $d, $crudo) = http('POST', "$DEV/applications", %t, json => $cuerpo_app);
    $app = exigir('crear la aplicación', [200, 201], $c, $d, $crudo); ok "aplicación creada";
}
my $app_id = $app->{applicationId};
($c, $d, $crudo) = http('GET', "$DEV/subscriptions?applicationId=$app_id&limit=100", %t);
my %suscrita = map { ($_->{apiId} => 1) } @{ exigir('listar suscripciones', [200], $c, $d, $crudo)->{list} // [] };
my $nuevas = 0;
for my $svc (sort keys %id_api) {
    next if $suscrita{ $id_api{$svc} };
    ($c, $d, $crudo) = http('POST', "$DEV/subscriptions", %t, json => { applicationId => $app_id, apiId => $id_api{$svc}, throttlingPolicy => $POL{suscripcion} });
    exigir("suscribir la aplicación a $svc", [200, 201], $c, $d, $crudo); $nuevas++;
}
ok scalar(keys %id_api) . " APIs suscritas ($nuevas nuevas)";

# ------------------------------------------------------------------------------------------------------------------------- api-key
# Un archivo por aplicación: la del ambiente es api-key-<ambiente>.txt; otra (APLICACION_NOMBRE=...) agrega su nombre.
my $sufijo = $E{APLICACION_NOMBRE} eq ($aplicacion_del_ambiente // '') ? '' : '-' . ($E{APLICACION_NOMBRE} =~ s/[^A-Za-z0-9._-]+/_/gr);
my $archivo_clave = "$local/api-key-$ambiente$sufijo.txt";
paso "api-key de WSO2 para la aplicación";
if (-s $archivo_clave && !$rotar) {
    ok "ya existe (" . (-s $archivo_clave) . " bytes en $archivo_clave); --rotar genera otra";
} else {
    ($c, $d, $crudo) = http('POST', "$DEV/applications/$app_id/api-keys/PRODUCTION/generate", %t,
        json => { validityPeriod => $E{API_KEY_VIGENCIA_SEG} + 0, additionalProperties => { permittedIP => '', permittedReferer => '' } });
    my $clave = exigir('generar la api-key', [200], $c, $d, $crudo)->{apikey};
    open(my $o, '>', $archivo_clave) or die "No puedo escribir $archivo_clave\n"; print $o $clave; close $o;
    chmod 0600, $archivo_clave;
    ok "generada (" . length($clave) . " caracteres, vigencia $E{API_KEY_VIGENCIA_SEG} s) en $archivo_clave";
}
open($fh, '<', $archivo_clave) or die; my $api_key = do { local $/; <$fh> }; close $fh;

# ------------------------------------------------------------------------------------------------------------------ pruebas de humo
paso "pruebas de humo en el gateway $E{WSO2_URL_GATEWAY}";
sleep 6;   # el gateway tarda unos segundos en cargar las APIs recién desplegadas
my ($svc0, $ctx0) = @{ (grep { $_->[0] eq 'identidad-service' } @servicios)[0] // $servicios[0] };
my ($cod1) = http('GET', "$E{WSO2_URL_GATEWAY}$ctx0/usuarios/me");
my $fallos = 0;
sub prueba { my ($desc, $esp, $obt) = @_; my $bien = $esp == $obt; $fallos++ unless $bien; printf "   %s %s (esperado %d, obtenido %d)\n", $bien ? 'OK  ' : 'FALLA', $desc, $esp, $obt; }
prueba("sin api-key de WSO2 el gateway rechaza", 401, $cod1);
# /q/health con api-key: no está en el contrato, así que WSO2 responde 404 (no llega al servicio)
my ($cod3) = http('GET', "$E{WSO2_URL_GATEWAY}$ctx0/q/health", headers => ["ApiKey: $api_key"]);
prueba("/q/health con api-key válida no se publica", 404, $cod3);
print "\n", $fallos ? "TERMINÓ CON $fallos PRUEBA(S) FALLIDA(S)\n" : "Listo. Prueba una llamada real:\n   curl -k -H \"ApiKey: \$(cat $archivo_clave)\" -H \"Content-Type: application/json\" -X POST $E{WSO2_URL_GATEWAY}$ctx0/usuarios/login -d '{\"email\":\"...\",\"password\":\"...\"}'\n";
exit($fallos ? 1 : 0);
