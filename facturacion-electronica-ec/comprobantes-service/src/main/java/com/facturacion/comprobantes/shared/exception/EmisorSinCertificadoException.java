package com.facturacion.comprobantes.shared.exception;

import java.util.UUID;

public class EmisorSinCertificadoException extends RuntimeException {

    public EmisorSinCertificadoException(UUID emisorId) {
        super("El emisor " + emisorId + " no existe o no tiene datos sincronizados todavía "
            + "(esperar a que firma-electronica-service publique su evento, o verificar que el emisor exista)");
    }
}
