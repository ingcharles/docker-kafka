package com.facturacion.firma.application.service;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.firma.application.dto.CertificadoMetadataResponse;
import com.facturacion.firma.application.mapper.CertificadoDigitalResponseMapper;
import com.facturacion.firma.domain.model.entity.CertificadoDigital;
import com.facturacion.firma.domain.model.enums.EstadoCertificado;
import com.facturacion.firma.domain.port.Cifrador;
import com.facturacion.firma.domain.repository.CertificadoDigitalRepository;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.shared.exception.CertificadoInvalidoException;

/**
 * Carga y custodia del certificado {@code .p12} de una empresa. Nunca guarda
 * ni devuelve el contenido o la contraseña en claro — ver {@link Cifrador}.
 */
@ApplicationScoped
public class CertificadoService {

    private static final java.time.format.DateTimeFormatter FORMATO_FECHA = java.time.format.DateTimeFormatter
        .ofPattern("dd/MM/yyyy").withZone(java.time.ZoneId.of("America/Guayaquil"));

    private final EmpresaRepository empresaRepository;
    private final CertificadoDigitalRepository certificadoRepository;
    private final CertificadoDigitalResponseMapper mapper;
    private final Cifrador cifradoService;

    @Inject
    public CertificadoService(EmpresaRepository empresaRepository, CertificadoDigitalRepository certificadoRepository,
            CertificadoDigitalResponseMapper mapper, Cifrador cifradoService) {
        this.empresaRepository = empresaRepository;
        this.certificadoRepository = certificadoRepository;
        this.mapper = mapper;
        this.cifradoService = cifradoService;
    }

    @WithTransaction
    public Uni<CertificadoMetadataResponse> subirCertificado(UUID empresaId, byte[] contenidoP12, String contrasena) {
        // validarYExtraerMetadata corre en el worker pool de Mutiny (ver su
        // Javadoc); hay que volver explícitamente al contexto de Vert.x
        // antes de tocar cualquier repositorio de Hibernate Reactive, o
        // revienta con HR000068/69 — mismo problema y misma solución que el
        // salto de contexto de OutboxRelay tras el ack asíncrono de Kafka.
        io.vertx.core.Context callingContext = Vertx.currentContext();
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + empresaId))
            .onItem().transformToUni(empresa -> validarYExtraerMetadata(contenidoP12, contrasena)
                .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
                .onItem().transformToUni(metadata -> certificadoRepository.revocarActivosDelEmpresa(empresaId)
                    .chain(ignorado -> {
                        CertificadoDigital certificado = new CertificadoDigital(
                            UUID.randomUUID(),
                            empresaId,
                            metadata.alias(),
                            metadata.huellaDigital(),
                            cifradoService.cifrar(contenidoP12),
                            cifradoService.cifrar(contrasena.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            metadata.fechaEmision(),
                            metadata.fechaExpiracion(),
                            EstadoCertificado.ACTIVO,
                            Instant.now());
                        return certificadoRepository.persist(certificado);
                    })
                    .map(mapper::toResponse)));
    }

    /**
     * Revoca el certificado activo de la empresa sin subir uno nuevo en su
     * lugar (ej. certificado comprometido) — reutiliza el mismo update masivo
     * que ya usa {@link #subirCertificado} antes de guardar el reemplazo.
     */
    @WithTransaction
    public Uni<Void> revocarActivo(UUID empresaId) {
        return certificadoRepository.findActivoByEmpresaId(empresaId)
            .onItem().ifNull().failWith(
                () -> new NotFoundException("La empresa " + empresaId + " no tiene un certificado activo"))
            .onItem().transformToUni(ignorado -> certificadoRepository.revocarActivosDelEmpresa(empresaId))
            .replaceWithVoid();
    }

    @WithSession
    public Uni<CertificadoMetadataResponse> obtenerActivoPorEmpresa(UUID empresaId) {
        return certificadoRepository.findActivoByEmpresaId(empresaId)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("La empresa " + empresaId + " no tiene un certificado activo"))
            .map(mapper::toResponse);
    }

    /** Un certificado que ya venció, o que todavía no empieza, no sirve para firmar: se rechaza al subirlo. */
    static void exigirVigente(Instant desde, Instant hasta, Instant ahora) {
        if (hasta.isBefore(ahora)) {
            throw new CertificadoInvalidoException("El certificado venció el " + FORMATO_FECHA.format(hasta)
                + ": sube uno vigente");
        }
        if (desde.isAfter(ahora)) {
            throw new CertificadoInvalidoException("El certificado todavía no es válido: empieza el "
                + FORMATO_FECHA.format(desde));
        }
    }

    /**
     * Pasa a EXPIRADO los certificados activos que ya vencieron (lo llama la tarea programada). Es idempotente: varias
     * instancias a la vez dan el mismo resultado.
     *
     * @return cuántos certificados se marcaron
     */
    @WithTransaction
    public Uni<Integer> marcarVencidos() {
        return certificadoRepository.marcarExpiradosHasta(Instant.now());
    }

    /**
     * Parsear un {@code .p12} implica IO/CPU (criptografía RSA, ASN.1) —
     * se corre en el worker pool de Mutiny para no bloquear el event loop
     * de Vert.x mientras el resto del pipeline reactivo sigue su curso.
     */
    private Uni<MetadataCertificado> validarYExtraerMetadata(byte[] contenidoP12, String contrasena) {
        return Uni.createFrom().item(() -> {
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(new ByteArrayInputStream(contenidoP12), contrasena.toCharArray());
                var aliases = keyStore.aliases();
                if (!aliases.hasMoreElements()) {
                    throw new CertificadoInvalidoException("El archivo .p12 no contiene ningún alias/entrada");
                }
                String alias = aliases.nextElement();
                X509Certificate certificado = (X509Certificate) keyStore.getCertificate(alias);
                if (certificado == null) {
                    throw new CertificadoInvalidoException("No se encontró un certificado X.509 en el .p12");
                }
                String huella = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(certificado.getEncoded()));
                Instant desde = certificado.getNotBefore().toInstant();
                Instant hasta = certificado.getNotAfter().toInstant();
                exigirVigente(desde, hasta, Instant.now());
                return new MetadataCertificado(alias, huella, desde, hasta);
            } catch (CertificadoInvalidoException e) {
                throw e;
            } catch (Exception e) {
                throw new CertificadoInvalidoException(
                    "El archivo no es un .p12 válido o la contraseña dada no lo abre", e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private record MetadataCertificado(String alias, String huellaDigital, Instant fechaEmision,
            Instant fechaExpiracion) {
    }
}
