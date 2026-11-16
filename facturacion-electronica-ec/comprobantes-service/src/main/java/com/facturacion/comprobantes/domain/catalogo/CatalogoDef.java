package com.facturacion.comprobantes.domain.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.facturacion.commons.error.Mensajes;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.domain.model.enums.Catalogo;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Descripción de un {@link Catalogo}: columnas tipadas y columnas clave (las de las tablas de
 * {@code V1__init.sql}; el nombre de la tabla es {@code catalogo.nombre()}). Todos los catálogos tienen la
 * misma forma de operación (listar, guardar por clave, publicar por Kafka), así que en vez de una entidad +
 * recurso REST por catálogo se describen una vez aquí y {@code CatalogoService} los trata de forma genérica.
 */
public record CatalogoDef(Catalogo catalogo, List<Columna> columnas) {

    public enum Tipo { TEXTO, DECIMAL, FECHA, BOOLEANO }

    /** Nombres de las columnas con significado propio en las reglas. */
    public static final String COL_ACTIVO = "activo";
    public static final String COL_VIGENTE_DESDE = "vigente_desde";
    public static final String COL_VIGENTE_HASTA = "vigente_hasta";
    public static final String COL_TARIFA = "tarifa";
    public static final String COL_PORCENTAJE = "porcentaje";

    /** "La ficha no publica fecha de inicio" — mismo valor por defecto que las tablas. */
    public static final LocalDate SIN_FECHA_INICIO = LocalDate.of(1900, 1, 1);

    private static final BigDecimal MAXIMO_PORCENTAJE = new BigDecimal("100");
    private static final BigDecimal MAXIMO_TARIFA = new BigDecimal("1000");

    /**
     * @param clave      forma parte de la clave primaria
     * @param requerido  debe venir en el body al guardar (si no tiene {@code valorPorDefecto})
     * @param maxLong    longitud máxima para TEXTO (la de la columna)
     */
    public record Columna(String nombre, Tipo tipo, boolean clave, boolean requerido, int maxLong,
            String valorPorDefecto) {

        static Columna texto(String nombre, int maxLong, boolean clave) {
            return new Columna(nombre, Tipo.TEXTO, clave, true, maxLong, null);
        }

        static Columna decimal(String nombre) {
            return new Columna(nombre, Tipo.DECIMAL, false, true, 0, null);
        }

        static Columna booleano(String nombre, boolean porDefecto) {
            return new Columna(nombre, Tipo.BOOLEANO, false, false, 0, String.valueOf(porDefecto));
        }

        static Columna activo() {
            return booleano(COL_ACTIVO, true);
        }

        static Columna fechaDesde(boolean clave) {
            return new Columna(COL_VIGENTE_DESDE, Tipo.FECHA, clave, false, 0, SIN_FECHA_INICIO.toString());
        }

        static Columna fechaHasta() {
            return new Columna(COL_VIGENTE_HASTA, Tipo.FECHA, false, false, 0, null);
        }
    }

    public static final List<CatalogoDef> TODOS = List.of(
        new CatalogoDef(Catalogo.IMPUESTO, List.of(
            Columna.texto("codigo", 2, true),
            Columna.texto("descripcion", 100, false),
            Columna.activo())),
        new CatalogoDef(Catalogo.TARIFA_IMPUESTO, List.of(
            Columna.texto("impuesto_codigo", 2, true),
            Columna.texto("codigo_porcentaje", 4, true),
            Columna.texto("descripcion", 100, false),
            Columna.decimal(COL_TARIFA),
            Columna.fechaDesde(false),
            Columna.fechaHasta(),
            Columna.activo())),
        new CatalogoDef(Catalogo.FORMA_PAGO, List.of(
            Columna.texto("codigo", 2, true),
            Columna.texto("descripcion", 100, false),
            Columna.fechaDesde(false),
            Columna.fechaHasta(),
            Columna.activo())),
        new CatalogoDef(Catalogo.TIPO_IDENTIFICACION, List.of(
            Columna.texto("codigo", 2, true),
            Columna.texto("descripcion", 100, false),
            Columna.booleano("solo_factura", false),
            Columna.activo())),
        new CatalogoDef(Catalogo.IMPUESTO_RETENCION, List.of(
            Columna.texto("codigo", 2, true),
            Columna.texto("descripcion", 100, false),
            Columna.activo())),
        new CatalogoDef(Catalogo.RETENCION, List.of(
            Columna.texto("impuesto_codigo", 2, true),
            Columna.texto("codigo", 5, true),
            Columna.fechaDesde(true),
            Columna.texto("descripcion", 150, false),
            Columna.decimal(COL_PORCENTAJE),
            Columna.fechaHasta(),
            Columna.activo())),
        new CatalogoDef(Catalogo.SUSTENTO_TRIBUTARIO, List.of(
            Columna.texto("codigo", 2, true),
            Columna.texto("descripcion", 300, false),
            Columna.activo())));

    public static Optional<CatalogoDef> buscar(String nombre) {
        return Catalogo.buscarPorNombre(nombre).flatMap(c -> TODOS.stream().filter(d -> d.catalogo == c).findFirst());
    }

    /** Nombre de cable del catálogo (también el nombre de su tabla). */
    public String nombre() {
        return catalogo.nombre();
    }

    public List<Columna> listarClaves() {
        return columnas.stream().filter(Columna::clave).toList();
    }

    public boolean tieneVigencia() {
        return columnas.stream().anyMatch(c -> c.nombre.equals(COL_VIGENTE_HASTA));
    }

    /**
     * Valida el body de un PUT y devuelve los valores normalizados por columna: TEXTO como String, DECIMAL como
     * String plano (sin notación científica), FECHA como String ISO (yyyy-MM-dd), BOOLEANO como Boolean. Los
     * problemas por campo se agrupan en un solo {@link CodigoError#CAMPOS_INVALIDOS} con su lista de detalles.
     */
    public Map<String, Object> validar(Map<String, Object> body) {
        if (body == null) {
            throw new ErrorNegocioException(CodigoError.CUERPO_OBLIGATORIO);
        }
        for (String campo : body.keySet()) {
            if (columnas.stream().noneMatch(c -> c.nombre.equals(campo))) {
                throw new ErrorNegocioException(CodigoError.CAMPO_DESCONOCIDO, campo, nombre(),
                    columnas.stream().map(Columna::nombre).toList());
            }
        }
        Map<String, Object> valores = new LinkedHashMap<>();
        List<String> errores = new ArrayList<>();
        for (Columna columna : columnas) {
            Object crudo = body.get(columna.nombre);
            boolean ausente = crudo == null || (crudo instanceof String s && s.isBlank());
            if (ausente) {
                if (columna.valorPorDefecto != null) {
                    crudo = columna.tipo == Tipo.BOOLEANO
                        ? Boolean.valueOf(columna.valorPorDefecto) : columna.valorPorDefecto;
                } else if (columna.requerido || columna.clave) {
                    errores.add(Mensajes.obtenerTexto(CodigoError.CAMPO_OBLIGATORIO, columna.nombre));
                    continue;
                } else {
                    valores.put(columna.nombre, null);
                    continue;
                }
            }
            try {
                valores.put(columna.nombre, normalizar(columna, crudo));
            } catch (ErrorNegocioException e) {
                errores.add(e.getMessage());
            }
        }
        if (!errores.isEmpty()) {
            throw ErrorNegocioException.conDetalles(CodigoError.CAMPOS_INVALIDOS, errores, nombre());
        }
        validarRangos(valores);
        return valores;
    }

    private static Object normalizar(Columna c, Object crudo) {
        switch (c.tipo) {
            case TEXTO -> {
                String texto = crudo.toString().trim();
                if (texto.isEmpty() || texto.length() > c.maxLong) {
                    throw new ErrorNegocioException(CodigoError.CAMPO_LONGITUD, c.nombre, c.maxLong);
                }
                return texto;
            }
            case DECIMAL -> {
                try {
                    return new BigDecimal(crudo.toString().trim()).toPlainString();
                } catch (NumberFormatException e) {
                    throw new ErrorNegocioException(CodigoError.CAMPO_NUMERICO, c.nombre);
                }
            }
            case FECHA -> {
                try {
                    return LocalDate.parse(crudo.toString().trim()).toString();
                } catch (DateTimeParseException e) {
                    throw new ErrorNegocioException(CodigoError.CAMPO_FECHA, c.nombre);
                }
            }
            case BOOLEANO -> {
                if (crudo instanceof Boolean b) {
                    return b;
                }
                String texto = crudo.toString().trim().toLowerCase();
                if (texto.equals("true") || texto.equals("false")) {
                    return Boolean.valueOf(texto);
                }
                throw new ErrorNegocioException(CodigoError.CAMPO_BOOLEANO, c.nombre);
            }
            default -> throw new IllegalStateException("Tipo de columna no soportado: " + c.tipo);
        }
    }

    private void validarRangos(Map<String, Object> valores) {
        validarRango(valores, COL_TARIFA, MAXIMO_TARIFA);
        validarRango(valores, COL_PORCENTAJE, MAXIMO_PORCENTAJE);
        if (valores.get(COL_VIGENTE_HASTA) != null) {
            LocalDate desde = LocalDate.parse((String) valores.get(COL_VIGENTE_DESDE));
            LocalDate hasta = LocalDate.parse((String) valores.get(COL_VIGENTE_HASTA));
            if (hasta.isBefore(desde)) {
                throw ErrorNegocioException.conDetalles(CodigoError.CAMPOS_INVALIDOS,
                    List.of(Mensajes.obtenerTexto(CodigoError.VIGENCIA_INVERTIDA)), nombre());
            }
        }
    }

    private void validarRango(Map<String, Object> valores, String campo, BigDecimal maximo) {
        if (valores.get(campo) != null) {
            BigDecimal valor = new BigDecimal((String) valores.get(campo));
            if (valor.signum() < 0 || valor.compareTo(maximo) > 0) {
                throw ErrorNegocioException.conDetalles(CodigoError.CAMPOS_INVALIDOS,
                    List.of(Mensajes.obtenerTexto(CodigoError.RANGO_FUERA_DE_LIMITES, campo, maximo.toPlainString())),
                    nombre());
            }
        }
    }
}
