package com.facturacion.firma.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.facturacion.commons.error.Mensajes;
import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.firma.shared.exception.CertificadoInvalidoException;
import com.facturacion.firma.shared.exception.RucDuplicadoException;

/** Mantiene alineados el enum del catálogo y mensajes.properties, y los estados HTTP de sus excepciones. */
class FirmaErrorTest {

    @Test
    void testDeberiaCadaCodigoTieneSuMensajeYNoSobranClaves() {
        for (FirmaError error : FirmaError.values()) {
            assertTrue(Mensajes.existe(error.claveMensaje()), error + " no tiene clave en mensajes.properties");
            assertFalse(Mensajes.obtenerTexto(error, "x").isBlank(), error + " tiene mensaje vacío");
        }
        Set<String> sobrantes = new HashSet<>(Mensajes.listarClaves());
        sobrantes.removeAll(Arrays.stream(FirmaError.values()).map(FirmaError::claveMensaje).collect(Collectors.toSet()));
        Arrays.stream(CodigoLog.values()).forEach(log -> sobrantes.remove(log.claveMensaje()));
        assertTrue(sobrantes.isEmpty(), "Claves sin código de error: " + sobrantes);
    }

    @Test
    void testDeberiaCodigosUnicosConFormatoYEstadoCoherente() {
        Set<String> vistos = new HashSet<>();
        for (FirmaError error : FirmaError.values()) {
            assertTrue(error.codigo().matches("^FIR-" + error.estadoHttp() + "-\\d{3}$"), error.codigo());
            assertTrue(vistos.add(error.codigo()), "Código repetido: " + error.codigo());
        }
    }

    @Test
    void testDeberiaLasExcepcionesConservanSuTextoYSuEstado() {
        RucDuplicadoException ruc = new RucDuplicadoException("1722039953001");
        assertEquals("Ya existe una empresa registrada con el RUC 1722039953001", ruc.getMessage());
        assertEquals(409, ruc.getError().estadoHttp());

        ConflictoException codigo = new ConflictoException(FirmaError.ESTABLECIMIENTO_DUPLICADO, "001");
        assertEquals("Ya existe un establecimiento activo con el código 001 para esta empresa", codigo.getMessage());
        assertEquals(409, codigo.getError().estadoHttp());

        CertificadoInvalidoException cert = new CertificadoInvalidoException(FirmaError.CERTIFICADO_INVALIDO, new RuntimeException());
        assertEquals("El archivo no es un .p12 válido o la contraseña dada no lo abre", cert.getMessage());
        assertEquals(400, cert.getError().estadoHttp());
    }
}
