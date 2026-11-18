package com.facturacion.firma.application.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.reactive.TrabajoBloqueante;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.firma.domain.model.entity.CertificadoDigital;
import com.facturacion.firma.domain.model.enums.EstadoCertificado;
import com.facturacion.firma.domain.port.Cifrador;
import com.facturacion.firma.domain.port.FirmadorXml;
import com.facturacion.firma.domain.repository.CertificadoDigitalRepository;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.shared.error.FirmaError;
import com.facturacion.firma.shared.exception.CertificadoInvalidoException;

/**
 * Firma un comprobante ya generado (sin firmar) usando el certificado
 * activo de la empresa dueña de ese comprobante, identificado por
 * {@code ruc} (no por el {@code id} interno de este servicio — el resto del
 * ecosistema ya no conoce ese UUID, ver ARQUITECTURA.md "Empresa sin ID"). Es
 * lo que invoca {@code FirmarComprobanteConsumer} al recibir un comando
 * {@code FirmarComprobante} desde {@code facturacion-orchestrator-service}.
 */
@ApplicationScoped
public class FirmaXadesService {

    private final EmpresaRepository empresaRepository;
    private final CertificadoDigitalRepository certificadoRepository;
    private final Cifrador cifradoService;
    private final FirmadorXml firmador;

    @Inject
    public FirmaXadesService(EmpresaRepository empresaRepository, CertificadoDigitalRepository certificadoRepository,
            Cifrador cifradoService, FirmadorXml firmador) {
        this.empresaRepository = empresaRepository;
        this.certificadoRepository = certificadoRepository;
        this.cifradoService = cifradoService;
        this.firmador = firmador;
    }

    /**
     * No lleva su propia anotación {@code @WithTransaction}/{@code @WithSession}
     * a propósito: siempre se invoca desde {@code FirmarComprobanteConsumer},
     * que ya administra la transacción — reutiliza esa sesión en vez de anidar una nueva.
     */
    public Uni<String> firmarXml(String ruc, String xmlSinFirmar) {
        return empresaRepository.findByRuc(ruc)
            .onItem().ifNull().failWith(() ->
                new CertificadoInvalidoException(FirmaError.EMPRESA_SIN_REGISTRO_PARA_FIRMAR, ruc))
            .onItem().transformToUni(empresa -> obtenerCertificadoVigente(ruc, empresa.getId()))
            .onItem().transformToUni(certificado -> firmarEnWorkerPool(certificado, xmlSinFirmar));
    }

    /**
     * El certificado activo de la empresa, siempre que siga vigente. Uno vencido se marca EXPIRADO y la firma falla
     * con un mensaje claro (la saga queda en FIRMA_FALLIDA): firmar con él solo llevaría a que el SRI lo rechace.
     */
    private Uni<CertificadoDigital> obtenerCertificadoVigente(String ruc, UUID empresaId) {
        Instant ahora = Instant.now();
        return certificadoRepository.findActivoByEmpresaId(empresaId).onItem().transformToUni(activo -> {
            if (activo == null) {
                return certificadoRepository.findUltimoByEmpresaId(empresaId).onItem().transformToUni(ultimo ->
                    Uni.createFrom().<CertificadoDigital>failure(
                        ultimo != null && ultimo.getEstado() == EstadoCertificado.EXPIRADO
                            ? crearErrorVencido(ruc, ultimo)
                            : new CertificadoInvalidoException(FirmaError.EMPRESA_SIN_CERTIFICADO_PARA_FIRMAR, ruc)));
            }
            if (activo.estaVencidoEn(ahora)) {
                return certificadoRepository.marcarExpiradosHasta(ahora).onItem().transformToUni(marcados ->
                    Uni.createFrom().<CertificadoDigital>failure(crearErrorVencido(ruc, activo)));
            }
            return Uni.createFrom().item(activo);
        });
    }

    private static CertificadoInvalidoException crearErrorVencido(String ruc, CertificadoDigital certificado) {
        return new CertificadoInvalidoException(FirmaError.CERTIFICADO_EMPRESA_VENCIDO, ruc,
            FechasSri.formatearFecha(certificado.getFechaExpiracionCertificado()));
    }

    private Uni<String> firmarEnWorkerPool(CertificadoDigital certificado, String xmlSinFirmar) {
        return TrabajoBloqueante.ejecutar(() -> {
            byte[] contenidoP12 = cifradoService.descifrar(certificado.getContenidoCifrado());
            byte[] contrasenaBytes = cifradoService.descifrar(certificado.getContrasenaCifrada());
            char[] contrasena = new String(contrasenaBytes, StandardCharsets.UTF_8).toCharArray();
            byte[] xmlFirmado = firmador.firmar(
                xmlSinFirmar.getBytes(StandardCharsets.UTF_8), contenidoP12, contrasena);
            return new String(xmlFirmado, StandardCharsets.UTF_8);
        });
    }
}
