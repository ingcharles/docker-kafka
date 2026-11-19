package com.facturacion.notificacion.domain.model.enums;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Variables que una plantilla de correo puede usar, escritas {@code {{nombre}}}. La lista es cerrada: guardar una
 * plantilla con otra variable se rechaza. {@link #LOGO} es la única que produce HTML (la imagen del logo, o el nombre de la
 * empresa si no tiene logo); el valor de las demás se escapa antes de ponerlo en el HTML.
 */
public enum VariableCorreo {

    TIPO_COMPROBANTE("tipoComprobante", "Nombre del tipo de comprobante: Factura, Nota de crédito..."),
    NUMERO("numero", "Número del comprobante, por ejemplo 001-001-000000123"),
    FECHA_EMISION("fechaEmision", "Fecha de emisión (dd/MM/aaaa)"),
    TOTAL("total", "Importe total con dos decimales, por ejemplo 11.50 (vacío si no se conoce)"),
    CLAVE_ACCESO("claveAcceso", "Clave de acceso de 49 dígitos"),
    RUC_EMISOR("rucEmisor", "RUC de la empresa emisora"),
    NOMBRE_EMISOR("nombreEmisor",
        "Nombre de la empresa: el ajuste notificacion.remitente_nombre, o el nombre comercial, o la razón social"),
    NOMBRE_RECEPTOR("nombreReceptor", "Razón social del receptor; \"cliente\" si no se conoce"),
    LOGO("logo", "El logo de la empresa; si no tiene logo, el nombre de la empresa. Solo en el cuerpo del correo");

    private static final String APERTURA = "{{";
    private static final String CIERRE = "}}";
    private static final String SEPARADOR = ", ";

    private final String nombre;
    private final String descripcion;

    VariableCorreo(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Cómo se escribe en una plantilla: {@code {{numero}}}. */
    public String marcador() {
        return APERTURA + nombre + CIERRE;
    }

    public static Optional<VariableCorreo> obtenerPorNombre(String nombre) {
        return Arrays.stream(values()).filter(variable -> variable.nombre.equals(nombre)).findFirst();
    }

    /** Los marcadores permitidos, para los mensajes de error. */
    public static String listarMarcadores() {
        return Arrays.stream(values()).map(VariableCorreo::marcador).collect(Collectors.joining(SEPARADOR));
    }
}
