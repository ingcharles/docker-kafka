#!/usr/bin/env perl
# Genera, a partir de las clases *Resource de cada servicio, lo que el gateway y el Ingress deben publicar:
#
#   wso2/openapi/<servicio>.yaml     contrato OpenAPI 3 con SOLO las rutas de negocio (sin /q/health, /q/metrics ni swagger).
#                                    WSO2 lo importa: lo que no está aquí, el gateway lo rechaza con 404.
#   --ingress <archivo>...           reemplaza, entre los marcadores "# >>> rutas generadas" y "# <<< rutas generadas", las rutas
#                                    del Ingress por los primeros segmentos de negocio de cada servicio (sin /q).
#
# Uso:  perl wso2/scripts/generar-contrato.pl [--ingress ruta/ingress.yaml ...]
# Se regenera cada vez que se agrega o quita un endpoint.
use strict;
use warnings;
use utf8;
use File::Basename qw(dirname);
use Cwd qw(abs_path);

my $aqui   = abs_path(dirname(__FILE__));
my $wso2   = abs_path("$aqui/..");
my $raiz   = abs_path("$wso2/../facturacion-electronica-ec");
my @ingress;
while (@ARGV) {
    my $a = shift @ARGV;
    if ($a eq '--ingress') { push @ingress, shift @ARGV while @ARGV && $ARGV[0] !~ /^--/; }
    else { die "Argumento desconocido: $a\n"; }
}

sub leer { my ($f) = @_; open(my $h, '<:encoding(UTF-8)', $f) or die "No puedo leer $f: $!\n"; local $/; my $c = <$h>; close $h; return $c; }

opendir(my $dh, $raiz) or die "No existe $raiz\n";
my @servicios = sort grep { -f "$raiz/$_/src/main/resources/application.properties" && /-service$/ } readdir $dh;
closedir $dh;

mkdir "$wso2/openapi" unless -d "$wso2/openapi";
my %segmentos;     # servicio => { contexto, segmentos[] }
my $total = 0;

for my $svc (@servicios) {
    my $props = leer("$raiz/$svc/src/main/resources/application.properties");
    my ($contexto) = $props =~ /^quarkus\.http\.root-path=(\S+)/m or next;
    my @recursos = sort glob("$raiz/$svc/src/main/java/com/facturacion/*/infrastructure/rest/*Resource.java");
    my %ops;   # "ruta\0verbo" => {...}
    for my $f (@recursos) {
        my $c = leer($f);
        $c =~ s{/\*.*?\*/}{}gs;                       # comentarios de bloque y javadoc
        $c =~ s{(?<!:)//[^\n]*}{}g;                   # comentarios de línea
        my ($cabecera) = $c =~ /(.*?)\bpublic\s+(?:final\s+)?class\b/s or next;
        my $base = '';
        $base = $1 if $cabecera =~ /\@Path\(\s*"([^"]*)"\s*\)\s*$/s || $cabecera =~ /.*\@Path\(\s*"([^"]*)"\s*\)/s;
        my $cuerpo = substr($c, length($cabecera));
        my $anot = qr/\@\w+(?:\((?:[^()"]|"(?:[^"\\]|\\.)*"|\((?:[^()"]|"(?:[^"\\]|\\.)*")*\))*\))?/;
        while ($cuerpo =~ /((?:\s*$anot)+)\s*public\s+[\w<>?,.\s\[\]]+?\s+(\w+)\s*\(/g) {
            my ($bloque, $metodo) = ($1, $2);
            my ($verbo) = $bloque =~ /\@(GET|POST|PUT|PATCH|DELETE)\b/ or next;
            my ($sub) = $bloque =~ /\@Path\(\s*"([^"]*)"\s*\)/;
            $sub = '' unless defined $sub;
            my ($resumen) = $bloque =~ /summary\s*=\s*"((?:[^"\\]|\\.)*)"/;
            $resumen = $metodo unless defined $resumen;
            my $multipart = $bloque =~ /MULTIPART_FORM_DATA/ ? 1 : 0;
            my $ruta = "$base/$sub"; $ruta =~ s{/+}{/}g; $ruta =~ s{/$}{} if length($ruta) > 1;
            $ruta = "/$ruta" unless $ruta =~ m{^/};
            $ruta =~ s/\{(\w+)\s*:[^}]*\}/{$1}/g;     # {ruc: \d{13}} -> {ruc}
            $ops{"$ruta\0$verbo"} = { ruta => $ruta, verbo => lc $verbo, resumen => $resumen, multipart => $multipart, id => $metodo };
            $total++;
        }
    }
    my %por_ruta;
    push @{ $por_ruta{ $_->{ruta} } }, $_ for sort { $a->{ruta} cmp $b->{ruta} || $a->{verbo} cmp $b->{verbo} } values %ops;

    my @l = ("openapi: 3.0.3", "info:", "  title: $svc", "  description: \"Contrato generado de las clases Resource (wso2/scripts/generar-contrato.pl). No editar a mano.\"", "  version: v1", "paths:");
    for my $ruta (sort keys %por_ruta) {
        push @l, "  \"$ruta\":";
        my @params = $ruta =~ /\{(\w+)\}/g;
        for my $o (@{ $por_ruta{$ruta} }) {
            my $r = $o->{resumen}; $r =~ s/\\"/'/g; $r =~ s/"/'/g;
            my $oid = lc("$o->{verbo}_$ruta"); $oid =~ s/[^a-z0-9]+/_/g; $oid =~ s/_+$//;
            push @l, "    $o->{verbo}:", "      operationId: $oid", "      summary: \"$r\"";
            if (@params) {
                push @l, "      parameters:";
                push @l, "        - { name: $_, in: path, required: true, schema: { type: string } }" for @params;
            }
            if ($o->{verbo} =~ /^(post|put|patch)$/) {
                my $mime = $o->{multipart} ? 'multipart/form-data' : 'application/json';
                push @l, "      requestBody:", "        content:", "          $mime:", "            schema: { type: object }";
            }
            push @l, "      responses:", "        \"200\": { description: OK }";
        }
    }
    my $nombre = "$wso2/openapi/$svc.yaml";
    open(my $o, '>:encoding(UTF-8)', $nombre) or die "No puedo escribir $nombre\n";
    print $o join("\n", @l), "\n"; close $o;

    my %seg;
    for my $ruta (keys %por_ruta) { my ($s) = $ruta =~ m#^/([^/{]+)#; $seg{$s} = 1 if defined $s; }
    $segmentos{$svc} = { contexto => $contexto, segmentos => [ sort keys %seg ], operaciones => scalar(keys %ops) };
    printf "%-36s %-24s %3d operaciones   primeros segmentos: %s\n", $svc, $contexto, scalar(keys %ops), join(', ', sort keys %seg);
}
print "Total: $total operaciones en ", scalar(keys %segmentos), " servicios.\n";

# servicio <TAB> ruta base: lo lee wso2/scripts/configurar-wso2.pl para crear una API por servicio.
open(my $tsv, '>:encoding(UTF-8)', "$wso2/openapi/servicios.tsv") or die "No puedo escribir servicios.tsv\n";
print $tsv "$_\t$segmentos{$_}{contexto}\n" for sort keys %segmentos;
close $tsv;

# Recursos que cuelgan de la raíz con un parámetro como primer segmento ("/{id}") o que usan "/": un prefijo por segmento no los cubre y
# publicar toda la ruta base dejaría /q/health y /q/metrics alcanzables. Esos servicios van en un segundo Ingress con regex que excluye /q.
my (%literal, %regex);
for my $svc (sort keys %segmentos) {
    my $s = $segmentos{$svc};
    open(my $h, '<:encoding(UTF-8)', "$wso2/openapi/$svc.yaml") or die; my $y = do { local $/; <$h> }; close $h;
    my $raiz_parametrica = $y =~ m{^  "/(?:\{[^"]*)?":}m;   # "/" o "/{...}"
    if ($raiz_parametrica) { $regex{$svc} = $s } else { $literal{$svc} = $s }
}

for my $d (@ingress) {
    my $f = "$d/ingress.yaml";
    my $c = leer($f);
    my ($host)  = $c =~ /^    - host: (\S+)/m or die "Sin host en $f\n";
    my ($lista) = $c =~ /whitelist-source-range: "([^"]*)"/ or die "Sin whitelist en $f\n";
    my $comunes = <<"ANOT";
    nginx.ingress.kubernetes.io/whitelist-source-range: "$lista"
    nginx.ingress.kubernetes.io/proxy-body-size: 12m
    nginx.ingress.kubernetes.io/proxy-read-timeout: "120"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "5"
ANOT
    my $tls = "  tls:\n    - hosts:\n        - $host\n      secretName: facturacion-tls\n";
    my $p_literal = '';
    for my $svc (sort keys %literal) {
        my $s = $literal{$svc};
        $p_literal .= "          - path: $s->{contexto}/$_\n            pathType: Prefix\n            backend:\n              service:\n                name: $svc\n                port:\n                  number: 8080\n" for @{ $s->{segmentos} };
    }
    my $p_regex = '';
    for my $svc (sort keys %regex) {
        my $s = $regex{$svc};
        $p_regex .= "          - path: $s->{contexto}(?:\$|/(?!q(?:/|\$)).*)\n            pathType: ImplementationSpecific\n            backend:\n              service:\n                name: $svc\n                port:\n                  number: 8080\n";
    }
    my $salida = <<"YAML";
# GENERADO por wso2/scripts/generar-contrato.pl (repositorio de código): no editar las rutas a mano. Solo se cambian el host, el TLS y
# whitelist-source-range (que van a las IP del gateway WSO2).
#
# Entrada por el ingress controller de RKE2 (ingress-nginx). Los servicios no se exponen directamente: el gateway (WSO2) es quien llega
# aquí. Solo se publican las rutas de negocio de cada servicio: /q/health, /q/metrics y swagger no están en ninguna regla, así que el
# Ingress responde 404 aunque alguien alcance la IP del Ingress.
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: facturacion
  labels:
    app.kubernetes.io/part-of: facturacion
  annotations:
${comunes}spec:
  ingressClassName: nginx
$tls  rules:
    - host: $host
      http:
        paths:
$p_literal---
# Servicios con recursos en la raíz de su ruta base (p. ej. /empresas/api/v1/{id}): regex que deja fuera /q.
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: facturacion-raiz
  labels:
    app.kubernetes.io/part-of: facturacion
  annotations:
    nginx.ingress.kubernetes.io/use-regex: "true"
${comunes}spec:
  ingressClassName: nginx
$tls  rules:
    - host: $host
      http:
        paths:
$p_regex
YAML
    open(my $o, '>:encoding(UTF-8)', $f) or die "No puedo escribir $f\n";
    print $o $salida; close $o;
    print "Ingress generado: $f (host $host)\n";
}
