package com.facturacion.firma.infrastructure.scheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;

import com.facturacion.firma.application.service.CertificadoService;

/**
 * Cada hora pasa a EXPIRADO los certificados activos que ya vencieron, para que se vea en la API y no quede ningún
 * certificado "activo" que el SRI rechazaría. La firma además comprueba la fecha en el momento de firmar
 * ({@code FirmaXadesService}), así que un certificado que vence entre dos ejecuciones tampoco se usa.
 */
@ApplicationScoped
public class CertificadoExpiracionJob {

    private static final Logger LOG = Logger.getLogger(CertificadoExpiracionJob.class);

    private final CertificadoService certificadoService;

    @Inject
    public CertificadoExpiracionJob(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @Scheduled(every = "1h", delayed = "1m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> marcarCertificadosVencidos() {
        return certificadoService.marcarVencidos()
            .onItem().invoke(marcados -> {
                if (marcados > 0) {
                    LOG.warnf("%d certificado(s) de firma vencieron y pasaron a EXPIRADO: hay que subir uno nuevo", marcados);
                }
            })
            .replaceWithVoid();
    }
}
