#!/usr/bin/env perl
# La observabilidad se define UNA sola vez, en esta carpeta. Este script escribe en el repositorio de despliegue lo que Kubernetes necesita
# a partir de esos archivos: las alertas (PrometheusRule), los tableros de Grafana, la configuración de Loki y de Alloy y las consultas
# del exportador de Postgres. En el repositorio de despliegue esos archivos son GENERADOS: no se editan a mano.
#
# Uso:  perl generar-monitoreo-k8s.pl <ruta-repositorio-de-despliegue>               escribe los archivos
#       perl generar-monitoreo-k8s.pl <ruta-repositorio-de-despliegue> --verificar   no escribe; sale con 1 si algo no coincide (CI)
use strict;
use warnings;
use File::Basename qw(dirname basename);
use File::Path qw(make_path);
use Cwd qw(abs_path);

my $verificar = grep { $_ eq '--verificar' } @ARGV;
my ($destino) = grep { !/^--/ } @ARGV;
die "Uso: perl generar-monitoreo-k8s.pl <ruta-repositorio-de-despliegue> [--verificar]\n" unless defined $destino;
my $origen = abs_path(dirname(__FILE__));
my $monitoreo = "$destino/monitoreo/recursos";
die "No existe $monitoreo: ¿es el repositorio de despliegue?\n" unless -d $monitoreo;

# Se comparan y escriben bytes (sin traducir finales de línea): los archivos generados quedan con LF.
sub leer {
    my ($f) = @_;
    open(my $h, '<:raw', $f) or return undef;
    local $/;
    my $c = <$h>;
    close $h;
    $c =~ s/\r\n/\n/g;
    return $c;
}

my %salida;   # ruta relativa a monitoreo/recursos => contenido

my $reglas = leer("$origen/prometheus/reglas/alertas.yml") // die "Falta prometheus/reglas/alertas.yml\n";
$reglas =~ s/\A(?:#[^\n]*\n)+//;   # la cabecera de comentarios del original se reemplaza por la de abajo
die "alertas.yml debe empezar por 'groups:'\n" unless $reglas =~ /\Agroups:\n/;
$reglas =~ s/^(?=.)/  /mg;         # dentro de spec:
$salida{'prometheusrule-facturacion.yaml'} = <<"YAML" . $reglas;
# GENERADO desde el repositorio de código: facturacion-electronica-ec/observabilidad/prometheus/reglas/alertas.yml
# (observabilidad/generar-monitoreo-k8s.pl). No editar aquí: cambia el original y vuelve a generar.
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: facturacion
  namespace: monitoring
  labels:
    app.kubernetes.io/part-of: facturacion
spec:
YAML

my %copias = (
    'config/loki.yml'    => 'loki/loki.yml',
    'config/config.alloy' => 'alloy/config-k8s.alloy',
    'config/queries.yaml' => 'postgres-exporter/queries.yaml',
);
opendir(my $d, "$origen/grafana/dashboards") or die "Falta grafana/dashboards\n";
$copias{"tableros/$_"} = "grafana/dashboards/$_" for grep { /\.json$/ } readdir $d;
closedir $d;
for my $a (keys %copias) {
    $salida{$a} = leer("$origen/$copias{$a}") // die "Falta $copias{$a}\n";
}

my @distintos;
for my $rel (sort keys %salida) {
    my $f = "$monitoreo/$rel";
    my $actual = leer($f);
    next if defined $actual && $actual eq $salida{$rel};
    push @distintos, $rel;
    next if $verificar;
    make_path(dirname($f));
    open(my $h, '>:raw', $f) or die "No puedo escribir $f: $!\n";
    print $h $salida{$rel};
    close $h;
}
# Un tablero borrado en el original no debe quedar en el despliegue.
for my $f (glob("$monitoreo/tableros/*.json")) {
    my $rel = 'tableros/' . basename($f);
    next if exists $salida{$rel};
    push @distintos, "$rel (sobra)";
    unlink $f unless $verificar;
}

if ($verificar) {
    if (@distintos) {
        print STDERR "El monitoreo del repositorio de despliegue NO coincide con el del código:\n";
        print STDERR "  monitoreo/recursos/$_\n" for @distintos;
        print STDERR "Corre: perl facturacion-electronica-ec/observabilidad/generar-monitoreo-k8s.pl <despliegue> y commitea el resultado.\n";
        exit 1;
    }
    print "Monitoreo de Kubernetes al día (", scalar(keys %salida), " archivos).\n";
} else {
    print @distintos ? "Actualizados: @distintos\n" : "Sin cambios.\n";
}
