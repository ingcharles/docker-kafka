package com.facturacion.firma.application.service;

import java.util.Base64;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.firma.application.dto.LogoResponse;
import com.facturacion.firma.domain.model.entity.Empresa;
import com.facturacion.firma.domain.repository.EmpresaRepository;
import com.facturacion.firma.shared.exception.LogoInvalidoException;

/**
 * Logo de la empresa para el RIDE. Se guarda en la tabla {@code empresa} y, al subirlo o quitarlo, se publica como el
 * ajuste {@value #CLAVE_RIDE_LOGO} (base64) por el tópico de configuración que ya consumen los demás servicios:
 * comprobantes-service lo lee al generar el RIDE sin llamar a este servicio.
 */
@ApplicationScoped
public class LogoEmpresaService {

    /** Ajuste con el que el logo viaja a comprobantes-service. */
    public static final String CLAVE_RIDE_LOGO = "ride.logo";

    /** Tope del archivo: el logo viaja en base64 por Kafka (límite por defecto 1 MB por mensaje). */
    static final int MAXIMO_BYTES = 200 * 1024;

    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /** El logo y su tipo MIME, tal como se devuelve al cliente. */
    public record Logo(byte[] contenido, String tipo) {
    }

    private final EmpresaRepository empresaRepository;
    private final ConfiguracionEmpresaService configuracionService;

    @Inject
    public LogoEmpresaService(EmpresaRepository empresaRepository, ConfiguracionEmpresaService configuracionService) {
        this.empresaRepository = empresaRepository;
        this.configuracionService = configuracionService;
    }

    @WithTransaction
    public Uni<LogoResponse> subir(UUID empresaId, byte[] contenido) {
        String tipo = tipoDe(contenido);
        return buscar(empresaId).onItem().transformToUni(empresa -> empresaRepository
            .actualizarLogo(empresaId, contenido, tipo)
            .chain(() -> configuracionService.publicarAjusteDerivado(empresa, CLAVE_RIDE_LOGO,
                Base64.getEncoder().encodeToString(contenido)))
            .replaceWith(new LogoResponse(tipo, contenido.length)));
    }

    @WithSession
    public Uni<Logo> obtener(UUID empresaId) {
        return buscar(empresaId).map(empresa -> {
            if (empresa.getLogo() == null) {
                throw new NotFoundException("La empresa " + empresaId + " no tiene logo");
            }
            return new Logo(empresa.getLogo(), empresa.getLogoTipo());
        });
    }

    @WithTransaction
    public Uni<Void> eliminar(UUID empresaId) {
        return buscar(empresaId).onItem().transformToUni(empresa -> {
            if (empresa.getLogo() == null) {
                return Uni.createFrom().failure(new NotFoundException("La empresa " + empresaId + " no tiene logo"));
            }
            return empresaRepository.actualizarLogo(empresaId, null, null)
                .chain(() -> configuracionService.retirarAjusteDerivado(empresa, CLAVE_RIDE_LOGO));
        });
    }

    private Uni<Empresa> buscar(UUID empresaId) {
        return empresaRepository.findById(empresaId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Empresa no encontrada: " + empresaId));
    }

    /** Tipo MIME según los primeros bytes (no se confía en el nombre ni en el Content-Type que mande el cliente). */
    static String tipoDe(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new LogoInvalidoException("El archivo del logo está vacío");
        }
        if (contenido.length > MAXIMO_BYTES) {
            throw new LogoInvalidoException("El logo pesa " + contenido.length / 1024 + " KB; el máximo es "
                + MAXIMO_BYTES / 1024 + " KB");
        }
        if (empiezaCon(contenido, FIRMA_PNG)) {
            return "image/png";
        }
        if (empiezaCon(contenido, FIRMA_JPEG)) {
            return "image/jpeg";
        }
        throw new LogoInvalidoException("El logo debe ser una imagen PNG o JPEG");
    }

    private static boolean empiezaCon(byte[] contenido, byte[] firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
