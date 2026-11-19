package com.facturacion.notificacion.application.service;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.vo.LogoEmpresa;
import com.facturacion.notificacion.domain.port.CorreoSalida;
import com.facturacion.notificacion.domain.repository.ConfiguracionEmpresaLocalRepository;
import com.facturacion.notificacion.domain.repository.PlantillaCorreoRepository;
import com.facturacion.notificacion.shared.error.CodigoLog;

/**
 * Lee todo lo que una empresa puede cambiar del correo: el asunto y el nombre del remitente (ajustes
 * {@code notificacion.asunto} y {@code notificacion.remitente_nombre}), el logo ({@code ride.logo}, el mismo que usa el
 * RIDE) y su plantilla propia. Ninguna falla de lo opcional (un logo dañado, la plantilla que no se puede leer) impide el
 * correo: se registra y sale con lo que tenga por defecto. Debe llamarse con una sesión de base de datos abierta.
 */
@ApplicationScoped
public class LectorAjustesCorreo {

    private static final Logger LOG = Logger.getLogger(LectorAjustesCorreo.class);
    private static final String CLAVE_ASUNTO = "notificacion.asunto";
    private static final String CLAVE_REMITENTE_NOMBRE = "notificacion.remitente_nombre";
    private static final String CLAVE_LOGO = "ride.logo";

    private final ConfiguracionEmpresaLocalRepository configuracionRepository;
    private final PlantillaCorreoRepository plantillaRepository;

    @Inject
    public LectorAjustesCorreo(ConfiguracionEmpresaLocalRepository configuracionRepository,
            PlantillaCorreoRepository plantillaRepository) {
        this.configuracionRepository = configuracionRepository;
        this.plantillaRepository = plantillaRepository;
    }

    /**
     * Las lecturas van una detrás de otra a propósito (no Uni.combine): dos consultas Hibernate Reactive concurrentes sobre la
     * misma sesión rompen el procesamiento del result set ("Illegal pop() with non-matching JdbcValuesSourceProcessingState").
     *
     * @param nombrePorDefecto nombre de la empresa si no configuró {@code notificacion.remitente_nombre} (su nombre comercial
     *                         o su razón social); puede ser {@code null}
     */
    public Uni<CorreoSalida.Ajustes> leer(String empresaRuc, TipoComprobanteCorreo tipo, String nombrePorDefecto) {
        return leerValor(empresaRuc, CLAVE_ASUNTO).chain(asunto -> leerValor(empresaRuc, CLAVE_REMITENTE_NOMBRE)
            .chain(remitente -> leerValor(empresaRuc, CLAVE_LOGO)
                .chain(logoBase64 -> buscarPlantilla(empresaRuc, tipo)
                    .map(plantilla -> new CorreoSalida.Ajustes(asunto, remitente,
                        elegirNombre(remitente, nombrePorDefecto), leerLogo(logoBase64, empresaRuc),
                        plantilla.orElse(null))))));
    }

    private Uni<String> leerValor(String empresaRuc, String clave) {
        return configuracionRepository.findByRucAndClave(empresaRuc, clave).map(ajuste -> ajuste == null ? null
            : ajuste.getValor());
    }

    /** El nombre del remitente si lo configuró; si no, el de la empresa. */
    private static String elegirNombre(String remitente, String nombrePorDefecto) {
        return remitente != null && !remitente.isBlank() ? remitente : nombrePorDefecto;
    }

    /** Un logo que no se puede leer no impide el correo: sale sin logo. */
    private static LogoEmpresa leerLogo(String base64, String empresaRuc) {
        try {
            return LogoEmpresa.desdeBase64(base64).orElse(null);
        } catch (IllegalArgumentException e) {
            LogCodificado.warn(LOG, CodigoLog.LOGO_NO_VALIDO, "empresa=" + empresaRuc);
            return null;
        }
    }

    /** La plantilla activa de la empresa para ese tipo y, si no hay, la genérica de la empresa. Si falla la lectura, ninguna. */
    private Uni<Optional<PlantillaCorreoEmpresa>> buscarPlantilla(String empresaRuc, TipoComprobanteCorreo tipo) {
        return plantillaRepository.buscarActiva(empresaRuc, tipo)
            .chain(propia -> propia.isPresent() || tipo == TipoComprobanteCorreo.GENERICO ? Uni.createFrom().item(propia)
                : plantillaRepository.buscarActiva(empresaRuc, TipoComprobanteCorreo.GENERICO))
            .onFailure().recoverWithItem(falla -> {
                LogCodificado.warn(LOG, CodigoLog.PLANTILLA_EMPRESA_FALLIDA, "empresa=" + empresaRuc,
                    falla.getClass().getSimpleName());
                return Optional.empty();
            });
    }
}
