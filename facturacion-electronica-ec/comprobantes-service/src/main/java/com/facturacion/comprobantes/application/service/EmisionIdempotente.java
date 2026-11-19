package com.facturacion.comprobantes.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.ConflictoException;
import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.application.dto.ResultadoEmision;
import com.facturacion.comprobantes.application.mapper.ComprobanteResponseMapper;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.RegistroIdempotencia;
import com.facturacion.comprobantes.domain.port.CalculadorHuella;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.domain.repository.IdempotenciaRepository;
import com.facturacion.comprobantes.shared.constant.ApiConstants;
import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.comprobantes.shared.error.CodigoLog;

/**
 * Emisión idempotente: un cliente que reintenta un {@code POST} (timeout, corte de red) con la misma cabecera
 * {@code Idempotency-Key} recibe el comprobante que ya se creó, no uno nuevo que gastaría otro secuencial.
 * <p>
 * La clave se reserva al inicio de la transacción de la emisión y se asocia al comprobante en esa misma transacción:
 * <ul>
 *   <li>Clave nueva: se emite normalmente.</li>
 *   <li>Clave ya usada con el mismo contenido: se devuelve el comprobante original ({@code repetida = true}).</li>
 *   <li>Clave ya usada con otro contenido: {@code 422}.</li>
 *   <li>Dos solicitudes simultáneas con la misma clave: la segunda espera a la primera y luego recibe su comprobante.</li>
 *   <li>Si la emisión falla (validación, SRI no aplica, base de datos), la transacción se deshace con la reserva, así
 *   que el reintento con la misma clave vuelve a intentar.</li>
 * </ul>
 * Sin cabecera se emite como siempre, salvo que {@code comprobantes.idempotencia.obligatoria} sea {@code true}.
 */
@ApplicationScoped
public class EmisionIdempotente {

    private static final Logger LOG = Logger.getLogger(EmisionIdempotente.class);

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 128;
    private static final Pattern FORMATO_CLAVE = Pattern.compile("[A-Za-z0-9_.:-]{" + LONGITUD_MINIMA + ","
        + LONGITUD_MAXIMA + "}");

    private final IdempotenciaRepository idempotenciaRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final ComprobanteResponseMapper mapper;
    private final CalculadorHuella calculadorHuella;
    private final boolean obligatoria;
    private final Duration retencion;

    @Inject
    public EmisionIdempotente(IdempotenciaRepository idempotenciaRepository,
            ComprobanteRepository comprobanteRepository, ComprobanteResponseMapper mapper,
            CalculadorHuella calculadorHuella,
            @ConfigProperty(name = "comprobantes.idempotencia.obligatoria", defaultValue = "false") boolean obligatoria,
            @ConfigProperty(name = "comprobantes.idempotencia.retencion", defaultValue = "P7D") Duration retencion) {
        this.idempotenciaRepository = idempotenciaRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.mapper = mapper;
        this.calculadorHuella = calculadorHuella;
        this.obligatoria = obligatoria;
        this.retencion = retencion;
    }

    /**
     * Corre {@code emision} una sola vez por clave. Debe llamarse dentro de la transacción de la emisión.
     *
     * @param clave valor de la cabecera {@code Idempotency-Key}; {@code null} o vacía si el cliente no la mandó
     */
    public Uni<ResultadoEmision> ejecutar(String ruc, String clave, TipoComprobante tipo, Object solicitud,
            Supplier<Uni<ComprobanteResponse>> emision) {
        if (clave == null || clave.isBlank()) {
            return obligatoria
                ? Uni.createFrom().failure(new ErrorNegocioException(CodigoError.IDEMPOTENCIA_CLAVE_REQUERIDA,
                    ApiConstants.HEADER_CLAVE_IDEMPOTENCIA))
                : emision.get().map(comprobante -> new ResultadoEmision(comprobante, false));
        }
        if (!FORMATO_CLAVE.matcher(clave).matches()) {
            return Uni.createFrom().failure(new ErrorNegocioException(CodigoError.IDEMPOTENCIA_CLAVE_INVALIDA,
                ApiConstants.HEADER_CLAVE_IDEMPOTENCIA, LONGITUD_MINIMA, LONGITUD_MAXIMA));
        }
        String huella = calculadorHuella.calcularHuella(List.of(tipo.name(), solicitud));
        return idempotenciaRepository.reservar(ruc, clave, huella)
            .onItem().transformToUni(nueva -> nueva
                ? emitirYAsociar(ruc, clave, emision)
                : devolverLaYaEmitida(ruc, clave, huella));
    }

    private Uni<ResultadoEmision> emitirYAsociar(String ruc, String clave,
            Supplier<Uni<ComprobanteResponse>> emision) {
        return emision.get()
            .onItem().transformToUni(comprobante -> idempotenciaRepository
                .asociarComprobante(ruc, clave, comprobante.getId())
                .replaceWith(new ResultadoEmision(comprobante, false)));
    }

    private Uni<ResultadoEmision> devolverLaYaEmitida(String ruc, String clave, String huella) {
        return idempotenciaRepository.buscar(ruc, clave)
            .onItem().transformToUni(registro -> exigirMismoContenido(registro, huella))
            .onItem().transformToUni(registro -> comprobanteRepository.findById(registro.comprobanteId()))
            .onItem().invoke(comprobante -> LogCodificado.info(LOG, CodigoLog.EMISION_REPETIDA_DEVUELTA,
                "comprobanteId=" + comprobante.getId()))
            .map(comprobante -> new ResultadoEmision(mapper.toResponse(comprobante), true));
    }

    /**
     * La clave existe pero no se puede devolver su comprobante: con otro contenido es un error del cliente (422); sin
     * comprobante asociado la solicitud original sigue en curso o se deshizo (409, que reintente).
     */
    private static Uni<RegistroIdempotencia> exigirMismoContenido(RegistroIdempotencia registro, String huella) {
        if (registro == null || registro.comprobanteId() == null) {
            return Uni.createFrom().failure(new ConflictoException(CodigoError.IDEMPOTENCIA_EN_CURSO));
        }
        if (!registro.huella().equals(huella)) {
            return Uni.createFrom().failure(new ErrorNegocioException(CodigoError.IDEMPOTENCIA_CLAVE_REUTILIZADA));
        }
        return Uni.createFrom().item(registro);
    }

    /** Borra las claves más viejas que el plazo de retención; devuelve cuántas. */
    public Uni<Long> purgarVencidas() {
        return idempotenciaRepository.purgarCreadasAntesDe(Instant.now().minus(retencion))
            .onItem().invoke(borradas -> {
                if (borradas > 0) {
                    LogCodificado.info(LOG, CodigoLog.IDEMPOTENCIA_PURGADA, "cantidad=" + borradas);
                }
            });
    }
}
