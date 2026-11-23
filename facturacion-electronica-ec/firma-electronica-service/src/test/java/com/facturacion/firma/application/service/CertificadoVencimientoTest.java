package com.facturacion.firma.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import com.facturacion.firma.domain.model.entity.CertificadoDigital;
import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.model.enums.EstadoCertificado;
import com.facturacion.firma.domain.port.Cifrador;
import com.facturacion.firma.domain.port.FirmadorXml;
import com.facturacion.firma.domain.repository.CertificadoDigitalRepository;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.shared.exception.CertificadoInvalidoException;

/** Un certificado vencido no firma: se marca EXPIRADO y la firma falla con un motivo claro. */
class CertificadoVencimientoTest {

    private static final String RUC = "1722039953001";

    private final UUID empresaId = UUID.randomUUID();
    private final List<String> llamadas = new ArrayList<>();
    private CertificadoDigital activo;
    private CertificadoDigital ultimo;
    private Empresa empresa;

    private FirmaXadesService servicio() {
        EmpresaRepository empresas = (EmpresaRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {EmpresaRepository.class}, (proxy, metodo, args) -> {
                if (metodo.getName().equals("findByRuc")) {
                    return Uni.createFrom().item(empresa);
                }
                throw new UnsupportedOperationException(metodo.getName());
            });
        CertificadoDigitalRepository certificados = (CertificadoDigitalRepository) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {CertificadoDigitalRepository.class},
            (proxy, metodo, args) -> {
                llamadas.add(metodo.getName());
                return switch (metodo.getName()) {
                    case "findActivoByEmpresaId" -> Uni.createFrom().item(activo);
                    case "findUltimoByEmpresaId" -> Uni.createFrom().item(ultimo);
                    case "marcarExpiradosHasta" -> Uni.createFrom().item(1);
                    default -> throw new UnsupportedOperationException(metodo.getName());
                };
            });
        Cifrador cifrador = new Cifrador() {
            @Override
            public byte[] cifrar(byte[] datosClaros) {
                return datosClaros;
            }

            @Override
            public byte[] descifrar(byte[] datosCifrados) {
                return datosCifrados;
            }
        };
        FirmadorXml firmador = (xml, p12, contrasena) -> xml;
        return new FirmaXadesService(empresas, certificados, cifrador, firmador);
    }

    private CertificadoDigital certificado(EstadoCertificado estado, Instant vence) {
        return new CertificadoDigital(UUID.randomUUID(), empresaId, "alias", "huella", new byte[0], new byte[0],
            vence.minusSeconds(86400L * 365), vence, estado, Instant.now());
    }

    private String fallo() {
        CompletionException e = assertThrows(CompletionException.class,
            () -> servicio().firmarXml(RUC, "<factura/>").subscribeAsCompletionStage().join());
        assertEquals(CertificadoInvalidoException.class, e.getCause().getClass());
        return e.getCause().getMessage();
    }

    private void conEmpresa() {
        empresa = new Empresa();
        empresa.setId(empresaId);
    }

    @Test
    void testDeberiaUnCertificadoActivoPeroVencidoSeMarcaExpiradoYNoFirma() {
        conEmpresa();
        activo = certificado(EstadoCertificado.ACTIVO, Instant.now().minusSeconds(3600));

        String mensaje = fallo();

        assertTrue(mensaje.contains("venció"), mensaje);
        assertTrue(mensaje.contains(RUC), mensaje);
        assertTrue(llamadas.contains("marcarExpiradosHasta"));
    }

    @Test
    void testDeberiaSiYaEstabaMarcadoExpiradoElMotivoSigueDiciendoQueVencio() {
        conEmpresa();
        activo = null;
        ultimo = certificado(EstadoCertificado.EXPIRADO, Instant.now().minusSeconds(86400));

        String mensaje = fallo();

        assertTrue(mensaje.contains("venció"), mensaje);
        assertFalse(llamadas.contains("marcarExpiradosHasta"), "ya estaba marcado");
    }

    @Test
    void testDeberiaSinCertificadoAlguno() {
        conEmpresa();
        activo = null;
        ultimo = null;

        assertEquals("La empresa con ruc " + RUC + " no tiene un certificado activo", fallo());
    }

    @Test
    void testDeberiaUnCertificadoRevocadoNoSeDiceVencido() {
        conEmpresa();
        activo = null;
        ultimo = certificado(EstadoCertificado.REVOCADO, Instant.now().plusSeconds(86400));

        assertEquals("La empresa con ruc " + RUC + " no tiene un certificado activo", fallo());
    }

    @Test
    void testDeberiaUnaEmpresaQueNoExiste() {
        empresa = null;

        assertEquals("No hay ninguna empresa registrada con ruc " + RUC, fallo());
    }

    @Test
    void testDeberiaElDominioSabeSiEstaVencido() {
        Instant ahora = Instant.now();
        assertTrue(certificado(EstadoCertificado.ACTIVO, ahora.minusSeconds(1)).estaVencidoEn(ahora));
        assertFalse(certificado(EstadoCertificado.ACTIVO, ahora.plusSeconds(60)).estaVencidoEn(ahora));
    }

    @Test
    void testDeberiaAlSubirSoloSeAceptaUnCertificadoVigente() {
        Instant ahora = Instant.now();

        CertificadoService.exigirVigente(ahora.minusSeconds(86400), ahora.plusSeconds(86400), ahora);

        CertificadoInvalidoException vencido = assertThrows(CertificadoInvalidoException.class,
            () -> CertificadoService.exigirVigente(ahora.minusSeconds(86400L * 400), ahora.minusSeconds(86400), ahora));
        assertTrue(vencido.getMessage().contains("venció"));

        CertificadoInvalidoException futuro = assertThrows(CertificadoInvalidoException.class,
            () -> CertificadoService.exigirVigente(ahora.plusSeconds(86400), ahora.plusSeconds(86400L * 400), ahora));
        assertTrue(futuro.getMessage().contains("todavía no es válido"));
    }
}
