package com.facturacion.comprobantes.infrastructure.ticket;

/** Los rótulos fijos del ticket, en un solo lugar (el texto de cada línea de datos sale del comprobante). */
enum TextoTicket {

    RUC("RUC "),
    NUMERO("No. "),
    FECHA("Fecha: "),
    MODIFICA("Modifica: "),
    DOCUMENTO_GENERICO("DOCUMENTO"),
    CLIENTE("Cliente: "),
    CONSUMIDOR_FINAL("CONSUMIDOR FINAL"),
    IDENTIFICACION("CI/RUC: "),
    FORMA_DE_PAGO("FORMA DE PAGO"),
    AUTORIZADO("AUTORIZADO POR EL SRI"),
    FECHA_AUTORIZACION("Autorizacion: "),
    NUMERO_AUTORIZACION("No. de autorizacion:"),
    EN_PROCESO("EN PROCESO DE AUTORIZACION"),
    CLAVE_DE_ACCESO("Clave de acceso:"),
    NO_AUTORIZADO("NO AUTORIZADO"),
    SIN_VALOR("NO VALE COMO COMPROBANTE"),
    AMBIENTE_PRUEBAS("AMBIENTE DE PRUEBAS"),
    SIN_VALIDEZ_TRIBUTARIA("SIN VALIDEZ TRIBUTARIA"),
    AGRADECIMIENTO("GRACIAS POR SU COMPRA");

    private final String texto;

    TextoTicket(String texto) {
        this.texto = texto;
    }

    String texto() {
        return texto;
    }
}
