package com.facturacion.sri.infrastructure.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.facturacion.sri.domain.port.RecepcionResultado;

class SriRecepcionClientTest {

    private static String respuesta(String estado, String identificador, String mensaje) {
        String detalle = identificador == null ? "<comprobantes/>"
            : "<comprobantes><comprobante><claveAcceso>1</claveAcceso><mensajes><mensaje><identificador>" + identificador
                + "</identificador><mensaje>" + mensaje + "</mensaje><tipo>ERROR</tipo></mensaje></mensajes></comprobante></comprobantes>";
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
            + "<ns2:validarComprobanteResponse xmlns:ns2=\"http://ec.gob.sri.ws.recepcion\"><RespuestaRecepcionComprobante>"
            + "<estado>" + estado + "</estado>" + detalle
            + "</RespuestaRecepcionComprobante></ns2:validarComprobanteResponse></soap:Body></soap:Envelope>";
    }

    @Test
    void testDeberiaUnaRespuestaRecibidaEsRecibida() throws Exception {
        RecepcionResultado r = SriRecepcionClient.parsear(respuesta("RECIBIDA", null, null));
        assertTrue(r.esRecibida());
        assertFalse(r.claveYaRegistrada());
    }

    @Test
    void testDeberiaUnaDevueltaPorEstructuraEsUnRechazo() throws Exception {
        RecepcionResultado r = SriRecepcionClient.parsear(respuesta("DEVUELTA", "35", "ARCHIVO NO CUMPLE ESTRUCTURA XML"));
        assertFalse(r.esRecibida());
        assertEquals("ARCHIVO NO CUMPLE ESTRUCTURA XML", r.mensajes());
    }

    @Test
    void testDeberiaUnaClaveYaRegistradaNoEsUnRechazoSinoQueElSriYaLaTiene() throws Exception {
        RecepcionResultado r = SriRecepcionClient.parsear(respuesta("DEVUELTA", "43", "CLAVE ACCESO REGISTRADA"));
        assertTrue(r.esRecibida());
        assertTrue(r.claveYaRegistrada());
    }

    @Test
    void testDeberiaUnComprobanteEnProcesamientoTampocoEsUnRechazo() throws Exception {
        assertTrue(SriRecepcionClient.parsear(respuesta("DEVUELTA", "70", "CLAVE ACCESO EN PROCESAMIENTO")).esRecibida());
    }

    @Test
    void testDeberiaUnSecuencialYaRegistradoSiEsUnRechazo() throws Exception {
        // identificador 45: el secuencial se repitió con otra clave de acceso — hay que corregir el comprobante
        assertFalse(SriRecepcionClient.parsear(respuesta("DEVUELTA", "45", "ERROR SECUENCIAL REGISTRADO")).esRecibida());
    }
}
