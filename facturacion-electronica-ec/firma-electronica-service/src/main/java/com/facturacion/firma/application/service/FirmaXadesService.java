package com.facturacion.firma.application.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;

import com.facturacion.firma.domain.model.entity.CertificadoDigital;
import com.facturacion.firma.domain.repository.CertificadoDigitalRepository;
import com.facturacion.firma.infrastructure.crypto.CifradoService;
import com.facturacion.firma.infrastructure.crypto.XadesBesFirmador;
import com.facturacion.firma.shared.exception.CertificadoInvalidoException;

/**
 * Firma un comprobante ya generado (sin firmar) usando el certificado
 * activo del emisor dueño de ese comprobante. Es lo que invoca
 * {@code FirmarComprobanteConsumer} al recibir un comando
 * {@code FirmarComprobante} desde {@code facturacion-orchestrator-service}.
 */
@ApplicationScoped
public class FirmaXadesService {

    private final CertificadoDigitalRepository certificadoRepository;
    private final CifradoService cifradoService;

    @Inject
    public FirmaXadesService(CertificadoDigitalRepository certificadoRepository, CifradoService cifradoService) {
        this.certificadoRepository = certificadoRepository;
        this.cifradoService = cifradoService;
    }

    /**
     * No lleva su propia anotación {@code @WithTransaction}/{@code @WithSession}
     * a propósito: siempre se invoca desde {@code FirmarComprobanteConsumer},
     * que ya administra la transacción — reutiliza esa sesión en vez de anidar una nueva.
     */
    public Uni<String> firmarXml(UUID emisorId, String xmlSinFirmar) {
        // El llamador (FirmarComprobanteConsumer) sigue la cadena tocando
        // Hibernate Reactive justo después de este Uni — hay que devolverle
        // el control en el mismo contexto de Vert.x en el que fuimos
        // invocados, no en el worker thread donde corrió la firma.
        io.vertx.core.Context callingContext = Vertx.currentContext();
        return certificadoRepository.findActivoByEmisorId(emisorId)
            .onItem().ifNull().failWith(() ->
                new CertificadoInvalidoException("El emisor " + emisorId + " no tiene un certificado activo"))
            .onItem().transformToUni(certificado -> firmarEnWorkerPool(certificado, xmlSinFirmar)
                .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run())));
    }

    private Uni<String> firmarEnWorkerPool(CertificadoDigital certificado, String xmlSinFirmar) {
        return Uni.createFrom().item(() -> {
            byte[] contenidoP12 = cifradoService.descifrar(certificado.getContenidoCifrado());
            byte[] contrasenaBytes = cifradoService.descifrar(certificado.getContrasenaCifrada());
            char[] contrasena = new String(contrasenaBytes, StandardCharsets.UTF_8).toCharArray();
            byte[] xmlFirmado = XadesBesFirmador.firmar(
                xmlSinFirmar.getBytes(StandardCharsets.UTF_8), contenidoP12, contrasena);
            return new String(xmlFirmado, StandardCharsets.UTF_8);
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
