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
import com.facturacion.firma.shared.exception.CertificadoInvalidoException;
import com.facturacion.firma.shared.exception.CodigoDuplicadoException;
import com.facturacion.firma.shared.exception.RucDuplicadoException;

/** Mantiene alineados el enum del catálogo y mensajes.properties, y los estados HTTP de sus excepciones. */
class FirmaErrorTest {

    @Test
    void cadaCodigoTieneSuMensajeYNoSobranClaves() {
        for (FirmaError error : FirmaError.values()) {
            assertTrue(Mensajes.existe(error.claveMensaje()), error + " no tiene clave en mensajes.properties");
            assertFalse(Mensajes.obtenerTexto(error, "x").isBlank(), error + " tiene mensaje vacío");
        }
        Set<String> sobrantes = new HashSet<>(Mensajes.listarClaves());
        sobrantes.removeAll(Arrays.stream(FirmaError.values()).map(FirmaError::claveMensaje).collect(Collectors.toSet()));
        assertTrue(sobrantes.isEmpty(), "Claves sin código de error: " + sobrantes);
    }

    @Test
    void codigosUnicosConFormatoYEstadoCoherente() {
        Set<String> vistos = new HashSet<>();
        for (FirmaError error : FirmaError.values()) {
            assertTrue(error.codigo().matches("^FIR-" + error.estadoHttp() + "-\\d{3}$"), error.codigo());
            assertTrue(vistos.add(error.codigo()), "Código repetido: " + error.codigo());
        }
    }

    @Test
    void lasExcepcionesConservanSuTextoYSuEstado() {
        RucDuplicadoException ruc = new RucDuplicadoException("1722039953001");
        assertEquals("Ya existe una empresa registrada con el RUC 1722039953001", ruc.getMessage());
        assertEquals(409, ruc.getError().estadoHttp());

        CodigoDuplicadoException codigo = new CodigoDuplicadoException("Ya existe el establecimiento 001");
        assertEquals("Ya existe el establecimiento 001", codigo.getMessage());
        assertEquals(409, codigo.getError().estadoHttp());

        CertificadoInvalidoException cert = new CertificadoInvalidoException("clave incorrecta", new RuntimeException());
        assertEquals("clave incorrecta", cert.getMessage());
        assertEquals(400, cert.getError().estadoHttp());
    }
}
