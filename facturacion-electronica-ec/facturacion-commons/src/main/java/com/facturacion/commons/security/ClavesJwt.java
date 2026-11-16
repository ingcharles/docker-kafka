package com.facturacion.commons.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Lee las llaves con las que identidad-service firma los JWT (la privada) y con las que el resto los verifica (la
 * pública). Acepta Ed25519 (recomendada) y RSA, en PEM —con o sin las líneas {@code BEGIN/END}— o solo el base64 del
 * DER. Una variable de entorno suele traer el PEM en una línea: los {@code \n} literales y los espacios se ignoran.
 * <p>
 * Formatos: pública X.509 (SubjectPublicKeyInfo, {@code BEGIN PUBLIC KEY}) y privada PKCS#8 ({@code BEGIN PRIVATE
 * KEY}), que es lo que generan {@code openssl genpkey} y {@code openssl pkey -pubout}.
 */
public final class ClavesJwt {

    private static final String[] ALGORITMOS = {"Ed25519", "RSA"};

    private ClavesJwt() {
    }

    public static PublicKey publica(String texto) {
        byte[] der = decodificar(texto, "pública");
        for (String algoritmo : ALGORITMOS) {
            try {
                return KeyFactory.getInstance(algoritmo).generatePublic(new X509EncodedKeySpec(der));
            } catch (InvalidKeySpecException e) {
                continue;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("El JDK no soporta " + algoritmo, e);
            }
        }
        throw new IllegalArgumentException("La clave pública JWT no es una llave Ed25519 ni RSA válida (X.509/SPKI)");
    }

    public static PrivateKey privada(String texto) {
        byte[] der = decodificar(texto, "privada");
        for (String algoritmo : ALGORITMOS) {
            try {
                return KeyFactory.getInstance(algoritmo).generatePrivate(new PKCS8EncodedKeySpec(der));
            } catch (InvalidKeySpecException e) {
                continue;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("El JDK no soporta " + algoritmo, e);
            }
        }
        throw new IllegalArgumentException("La clave privada JWT no es una llave Ed25519 ni RSA válida (PKCS#8)");
    }

    private static byte[] decodificar(String texto, String cual) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Falta la clave " + cual + " JWT");
        }
        String cuerpo = texto.replace("\\n", "\n")
            .replaceAll("-----[A-Z ]+-----", "")
            .replaceAll("\\s+", "");
        try {
            return Base64.getDecoder().decode(cuerpo.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La clave " + cual + " JWT no es un PEM/base64 válido", e);
        }
    }
}
