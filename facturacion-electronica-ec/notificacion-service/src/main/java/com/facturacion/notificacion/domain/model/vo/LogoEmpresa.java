package com.facturacion.notificacion.domain.model.vo;

import java.util.Base64;
import java.util.Optional;

/**
 * El logo de la empresa (ajuste {@code ride.logo}, que viaja en base64): una imagen PNG o JPEG. El tipo se decide por los
 * primeros bytes, no por ningún nombre.
 */
public record LogoEmpresa(byte[] datos, String tipoContenido) {

    /** Identificador con que el logo viaja incrustado en el correo: {@code <img src="cid:logo@facturacion">}. */
    public static final String CID = "logo@facturacion";

    private static final String TIPO_PNG = "image/png";
    private static final String TIPO_JPEG = "image/jpeg";
    private static final int[] FIRMA_PNG = {0x89, 0x50, 0x4E, 0x47};
    private static final int[] FIRMA_JPEG = {0xFF, 0xD8, 0xFF};

    /**
     * Lee el logo desde su base64. Devuelve vacío si no hay valor y falla con {@link IllegalArgumentException} si no es
     * base64 o no es una imagen PNG/JPEG (el llamador decide: el correo sale sin logo).
     */
    public static Optional<LogoEmpresa> desdeBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            return Optional.empty();
        }
        byte[] datos = Base64.getDecoder().decode(base64.trim());
        if (empiezaCon(datos, FIRMA_PNG)) {
            return Optional.of(new LogoEmpresa(datos, TIPO_PNG));
        }
        if (empiezaCon(datos, FIRMA_JPEG)) {
            return Optional.of(new LogoEmpresa(datos, TIPO_JPEG));
        }
        throw new IllegalArgumentException("El logo no es una imagen PNG ni JPEG");
    }

    private static boolean empiezaCon(byte[] datos, int[] firma) {
        if (datos.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((datos[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
