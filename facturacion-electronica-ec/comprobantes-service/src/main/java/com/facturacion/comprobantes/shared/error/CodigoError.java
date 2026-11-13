package com.facturacion.comprobantes.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Errores de negocio de la emisión de comprobantes contra los catálogos y reglas del SRI. Cada uno lleva el
 * {@code codigo} estable que recibe el cliente, el {@code estadoHttp} y la {@code claveMensaje} del texto en
 * {@code mensajes.properties} (con {@code {0}}, {@code {1}}... para los parámetros). El texto nunca se escribe
 * en el código.
 */
public enum CodigoError implements ErrorCatalogo {

    // Formas de pago (Tabla 24) y reglas de <pagos>
    PAGO_FORMA_NO_VIGENTE("COMP-400-101", 400, "pago.forma_no_vigente"),
    PAGO_RETENCION_SIN_PLAZO("COMP-400-102", 400, "pago.retencion_sin_plazo"),
    PAGO_PLAZO_SIN_UNIDAD("COMP-400-103", 400, "pago.plazo_sin_unidad"),
    PAGO_UNIDAD_SIN_PLAZO("COMP-400-104", 400, "pago.unidad_sin_plazo"),
    PAGO_PLAZO_NO_NUMERICO("COMP-400-105", 400, "pago.plazo_no_numerico"),
    PAGO_PLAZO_FUERA_DE_RANGO("COMP-400-106", 400, "pago.plazo_fuera_de_rango"),
    PAGO_UNIDAD_TIEMPO_LARGA("COMP-400-107", 400, "pago.unidad_tiempo_larga"),
    PAGOS_NO_CUADRAN("COMP-400-108", 400, "pago.no_cuadran"),
    NEGOCIABLE_SIN_PLAZO("COMP-400-109", 400, "pago.negociable_sin_plazo"),
    NEGOCIABLE_SIN_DIRECCION("COMP-400-110", 400, "pago.negociable_sin_direccion"),

    // Impuestos y tarifas (Tablas 16 y 17)
    IMPUESTO_NO_EXISTE("COMP-400-201", 400, "impuesto.no_existe"),
    PORCENTAJE_IMPUESTO_NO_VIGENTE("COMP-400-202", 400, "impuesto.porcentaje_no_vigente"),
    TARIFA_NO_COINCIDE("COMP-400-203", 400, "impuesto.tarifa_no_coincide"),
    TARIFA_NO_DISPONIBLE("COMP-400-204", 400, "impuesto.tarifa_no_disponible"),

    // Identificación del receptor (Tabla 6)
    TIPO_IDENTIFICACION_NO_EXISTE("COMP-400-301", 400, "identificacion.tipo_no_existe"),
    CONSUMIDOR_FINAL_SOLO_FACTURA("COMP-400-302", 400, "identificacion.consumidor_final_solo_factura"),
    IDENTIFICACION_FIJA_INCORRECTA("COMP-400-303", 400, "identificacion.fija_incorrecta"),
    IDENTIFICACION_FIJA_CON_OTRO_TIPO("COMP-400-304", 400, "identificacion.fija_con_otro_tipo"),
    IDENTIFICACION_LONGITUD_INVALIDA("COMP-400-305", 400, "identificacion.longitud_invalida"),
    CONSUMIDOR_FINAL_EXCEDE_MONTO("COMP-400-306", 400, "identificacion.consumidor_final_excede_monto"),

    // Retenciones (Tablas 19 y 20) y documento sustento
    IMPUESTO_RETENER_NO_EXISTE("COMP-400-401", 400, "retencion.impuesto_no_existe"),
    CODIGO_RETENCION_NO_EXISTE("COMP-400-402", 400, "retencion.codigo_no_existe"),
    CODIGO_RETENCION_NO_VIGENTE("COMP-400-403", 400, "retencion.codigo_no_vigente"),
    PORCENTAJE_RETENER_NO_COINCIDE("COMP-400-404", 400, "retencion.porcentaje_no_coincide"),
    VALOR_RETENIDO_INCORRECTO("COMP-400-405", 400, "retencion.valor_retenido_incorrecto"),
    SUSTENTO_NO_EXISTE("COMP-400-406", 400, "retencion.sustento_no_existe"),
    FECHA_DOC_SUSTENTO_INVALIDA("COMP-400-407", 400, "retencion.fecha_doc_sustento_invalida"),
    FECHA_DOCUMENTO_INVALIDA("COMP-400-408", 400, "documento.fecha_invalida"),

    // Ítems
    ITEM_DADO_DE_BAJA("COMP-400-501", 400, "item.dado_de_baja"),
    LINEA_SIN_IMPUESTOS_NI_ITEM("COMP-400-502", 400, "item.linea_sin_impuestos_ni_item"),
    ITEM_NO_ENCONTRADO_EN_CATALOGO_LOCAL("COMP-400-503", 400, "item.no_encontrado_en_catalogo_local"),

    // Empresa, establecimiento y punto de emisión
    ESTABLECIMIENTO_NO_REGISTRADO("COMP-400-601", 400, "empresa.establecimiento_no_registrado"),
    PUNTO_EMISION_NO_REGISTRADO("COMP-400-602", 400, "empresa.punto_emision_no_registrado"),
    AMBIENTE_INVALIDO("COMP-400-603", 400, "empresa.ambiente_invalido"),

    // Tipo de comprobante
    TIPO_COMPROBANTE_NO_SOPORTADO("COMP-400-801", 400, "documento.tipo_no_soportado"),

    // Administración de catálogos (API /catalogos)
    CATALOGO_NO_ENCONTRADO("COMP-404-701", 404, "catalogo.no_encontrado"),
    ESCRITURA_DESHABILITADA("COMP-403-701", 403, "catalogo.escritura_deshabilitada"),
    TOKEN_ADMIN_INVALIDO("COMP-403-702", 403, "catalogo.token_admin_invalido"),
    FECHA_INVALIDA("COMP-400-701", 400, "catalogo.fecha_invalida"),
    CUERPO_OBLIGATORIO("COMP-400-702", 400, "catalogo.cuerpo_obligatorio"),
    CAMPO_DESCONOCIDO("COMP-400-703", 400, "catalogo.campo_desconocido"),
    CAMPOS_INVALIDOS("COMP-400-704", 400, "catalogo.campos_invalidos"),
    DATO_RECHAZADO_POR_BASE_DE_DATOS("COMP-400-705", 400, "catalogo.dato_rechazado_por_bd"),
    // Detalles individuales de un CAMPOS_INVALIDOS
    CAMPO_OBLIGATORIO("COMP-400-711", 400, "campo.obligatorio"),
    CAMPO_LONGITUD("COMP-400-712", 400, "campo.longitud"),
    CAMPO_NUMERICO("COMP-400-713", 400, "campo.numerico"),
    CAMPO_FECHA("COMP-400-714", 400, "campo.fecha"),
    CAMPO_BOOLEANO("COMP-400-715", 400, "campo.booleano"),
    RANGO_FUERA_DE_LIMITES("COMP-400-716", 400, "campo.rango_fuera_de_limites"),
    VIGENCIA_INVERTIDA("COMP-400-717", 400, "campo.vigencia_invertida");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    CodigoError(String codigo, int estadoHttp, String claveMensaje) {
        this.codigo = codigo;
        this.estadoHttp = estadoHttp;
        this.claveMensaje = claveMensaje;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public int estadoHttp() {
        return estadoHttp;
    }

    @Override
    public String claveMensaje() {
        return claveMensaje;
    }
}
