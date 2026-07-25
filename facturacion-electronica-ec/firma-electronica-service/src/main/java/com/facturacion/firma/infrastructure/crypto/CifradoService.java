package com.facturacion.firma.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Cifrado en reposo (AES-256-GCM) para el contenido del .p12 y su
 * contraseña antes de guardarlos en {@code certificado_digital} — nada de
 * material criptográfico del emisor se persiste jamás en claro.
 * <p>
 * La clave maestra configurada ({@code firma.cifrado.clave-maestra}) puede
 * tener cualquier longitud (se generó con {@code openssl rand -base64 24}
 * al armar el stack) — se deriva de ella una clave AES-256 aplicando
 * SHA-256, en vez de exigir que el operador provea exactamente 32 bytes.
 * Como el secreto de entrada ya es aleatorio de alta entropía (no una
 * contraseña elegida por un humano), un hash simple es suficiente aquí; no
 * hace falta un KDF con costo computacional como PBKDF2/Argon2.
 */
@ApplicationScoped
public class CifradoService {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANO_IV_BYTES = 12;
    private static final int TAMANO_TAG_BITS = 128;

    private final SecretKeySpec claveAes;
    private final SecureRandom random = new SecureRandom();

    public CifradoService(@ConfigProperty(name = "firma.cifrado.clave-maestra") String claveMaestra) {
        this.claveAes = new SecretKeySpec(derivarClave(claveMaestra), "AES");
    }

    public byte[] cifrar(byte[] datosClaros) {
        try {
            byte[] iv = new byte[TAMANO_IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, claveAes, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] textoCifrado = cipher.doFinal(datosClaros);
            byte[] resultado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, resultado, iv.length, textoCifrado.length);
            return resultado;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el material criptográfico", e);
        }
    }

    public byte[] descifrar(byte[] datosCifrados) {
        try {
            byte[] iv = Arrays.copyOfRange(datosCifrados, 0, TAMANO_IV_BYTES);
            byte[] textoCifrado = Arrays.copyOfRange(datosCifrados, TAMANO_IV_BYTES, datosCifrados.length);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, claveAes, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            return cipher.doFinal(textoCifrado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el material criptográfico", e);
        }
    }

    private static byte[] derivarClave(String claveMaestra) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(claveMaestra.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo derivar la clave de cifrado", e);
        }
    }
}
