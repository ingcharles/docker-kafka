package com.facturacion.firma.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP propios de firma-electronica-service (prefijo {@code FIR}); los genéricos (404, 401, 403)
 * son {@code ErrorComun}. El texto de cada uno está en {@code mensajes.properties}.
 */
public enum FirmaError implements ErrorCatalogo {

    CERTIFICADO_INVALIDO("FIR-400-001", 400, "certificado.invalido"),
    LOGO_VACIO("FIR-400-002", 400, "logo.vacio"),
    EMPRESA_NO_ENCONTRADA("FIR-404-001", 404, "empresa.no_encontrada"),
    ESTABLECIMIENTO_NO_ENCONTRADO("FIR-404-002", 404, "establecimiento.no_encontrado"),
    PUNTO_EMISION_NO_ENCONTRADO("FIR-404-003", 404, "punto_emision.no_encontrado"),
    CERTIFICADO_ACTIVO_NO_ENCONTRADO("FIR-404-004", 404, "certificado.activo_no_encontrado"),
    CONFIGURACION_NO_ENCONTRADA("FIR-404-005", 404, "configuracion.no_encontrada"),
    LOGO_NO_ENCONTRADO("FIR-404-006", 404, "logo.no_encontrado"),
    ESTABLECIMIENTO_DUPLICADO("FIR-409-001", 409, "establecimiento.duplicado"),
    RUC_DUPLICADO("FIR-409-002", 409, "empresa.ruc_duplicado"),
    ARCHIVO_LOGO_ILEGIBLE("FIR-400-003", 400, "logo.archivo_ilegible"),
    ARCHIVO_CERTIFICADO_ILEGIBLE("FIR-400-004", 400, "certificado.archivo_ilegible"),
    CIFRADO_FALLIDO("FIR-500-001", 500, "cifrado.fallido"),
    DESCIFRADO_FALLIDO("FIR-500-002", 500, "cifrado.descifrado_fallido"),
    CLAVE_CIFRADO_NO_DERIVABLE("FIR-500-003", 500, "cifrado.clave_no_derivable"),
    COMANDO_FIRMA_INVALIDO("FIR-400-005", 400, "comando.firma_invalido"),
    CERTIFICADO_SIN_ENTRADAS("FIR-400-006", 400, "certificado.sin_entradas"),
    CERTIFICADO_SIN_X509("FIR-400-007", 400, "certificado.sin_x509"),
    CERTIFICADO_VENCIDO("FIR-400-008", 400, "certificado.vencido"),
    CERTIFICADO_AUN_NO_VIGENTE("FIR-400-009", 400, "certificado.aun_no_vigente"),
    CERTIFICADO_EMPRESA_VENCIDO("FIR-400-010", 400, "certificado.empresa_vencido"),
    EMPRESA_SIN_REGISTRO_PARA_FIRMAR("FIR-400-011", 400, "firma.empresa_sin_registro"),
    EMPRESA_SIN_CERTIFICADO_PARA_FIRMAR("FIR-400-012", 400, "firma.empresa_sin_certificado"),
    FIRMA_NO_REALIZADA("FIR-400-013", 400, "firma.no_realizada"),
    LOGO_MUY_GRANDE("FIR-400-014", 400, "logo.muy_grande"),
    LOGO_TIPO_NO_ADMITIDO("FIR-400-015", 400, "logo.tipo_no_admitido"),
    PUNTO_EMISION_DUPLICADO("FIR-409-003", 409, "punto_emision.duplicado"),
    CONFIGURACION_DUPLICADA("FIR-409-004", 409, "configuracion.duplicada");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    FirmaError(String codigo, int estadoHttp, String claveMensaje) {
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
