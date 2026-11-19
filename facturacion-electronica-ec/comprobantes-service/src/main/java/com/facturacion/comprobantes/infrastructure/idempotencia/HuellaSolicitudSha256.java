package com.facturacion.comprobantes.infrastructure.idempotencia;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.exception.ErrorInternoException;
import com.facturacion.comprobantes.domain.port.CalculadorHuella;

/**
 * Huella de una solicitud: SHA-256 de su JSON. El JSON de un objeto sale siempre con los campos en el mismo orden (el de
 * la clase), así que el mismo cuerpo da la misma huella.
 */
@ApplicationScoped
public class HuellaSolicitudSha256 implements CalculadorHuella {

    private static final String ALGORITMO = "SHA-256";

    private final ObjectMapper objectMapper;

    @Inject
    public HuellaSolicitudSha256(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String calcularHuella(Object solicitud) {
        try {
            byte[] json = objectMapper.writeValueAsString(solicitud).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance(ALGORITMO).digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new ErrorInternoException(e, ErrorComun.ERROR_INTERNO);
        }
    }
}
