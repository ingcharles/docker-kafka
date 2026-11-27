package com.facturacion.commons.limite;

import java.time.Duration;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.error.CodigoLogComun;
import com.facturacion.commons.error.LogCodificado;
import com.facturacion.commons.exception.LimiteExcedidoException;

/**
 * Límites por usuario y por empresa de las operaciones delicadas, según lo configurado para cada empresa. Si la base no
 * responde, la operación pasa y se registra: un límite caído no debe impedir facturar (la protección general sigue en el
 * gateway).
 */
@ApplicationScoped
public class LimitadorDeUso {

    /** Valor de {@code ruc} para las empresas sin límite propio y para lo que no pertenece a ninguna. */
    public static final String CUALQUIER_EMPRESA = "*";

    private static final Logger LOG = Logger.getLogger(LimitadorDeUso.class);

    /** Cuánto se espera a la base antes de dejar pasar. */
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(2);
    private static final String SEPARADOR = ":";
    private static final String AMBITO_USUARIO = "por usuario";
    private static final String AMBITO_EMPRESA = "por empresa";

    private final ContadorDeUso contador;
    private final ResolutorDeLimites resolutor;

    @Inject
    public LimitadorDeUso(ContadorDeUso contador, ResolutorDeLimites resolutor) {
        this.contador = contador;
        this.resolutor = resolutor;
    }

    /**
     * Cuenta un uso al usuario y a su empresa, y falla con 429 si alguno supera su límite. Quien ya superó el suyo no
     * consume el cupo de la empresa.
     *
     * @param usuario a quién se le cuenta: el id del usuario, o un dato que identifique la cuenta sin revelarla
     * @param ruc     empresa por la que se hace, ya comprobado que el usuario puede actuar por ella; {@code null} si no hay
     */
    public Uni<Void> exigir(OperacionLimitada operacion, Object usuario, String ruc) {
        return resolutor.resolver(operacion, ruc)
            .ifNoItem().after(ESPERA_MAXIMA).fail()
            .onFailure().recoverWithItem(falla -> {
                registrarBaseCaida(operacion, falla);
                return Optional.of(Limite.NINGUNO);
            })
            .map(configurado -> configurado.orElseGet(() -> {
                LogCodificado.warn(LOG, CodigoLogComun.LIMITE_SIN_CONFIGURACION, null, operacion.nombre());
                return Limite.NINGUNO;
            }))
            .chain(limite -> consumir(operacion, armarClaveDeUsuario(operacion, usuario, ruc),
                    limite.maximoPorUsuario(), limite.ventana(), AMBITO_USUARIO, usuario)
                .chain(() -> ruc == null ? Uni.createFrom().voidItem()
                    : consumir(operacion, armarClaveDeEmpresa(operacion, ruc), limite.maximoPorEmpresa(),
                        limite.ventana(), AMBITO_EMPRESA, usuario)));
    }

    /** Pone en cero lo consumido por el usuario; si no se puede, solo se registra. */
    public Uni<Void> reiniciar(OperacionLimitada operacion, Object usuario, String ruc) {
        return contador.reiniciar(armarClaveDeUsuario(operacion, usuario, ruc))
            .ifNoItem().after(ESPERA_MAXIMA).fail()
            .onFailure().recoverWithItem(falla -> {
                registrarBaseCaida(operacion, falla);
                return null;
            });
    }

    private Uni<Void> consumir(OperacionLimitada operacion, String clave, int maximo, Duration ventana, String ambito,
            Object usuario) {
        if (maximo <= 0) {
            return Uni.createFrom().voidItem();
        }
        return contador.consumir(clave, ventana)
            .ifNoItem().after(ESPERA_MAXIMA).fail()
            .onFailure().recoverWithItem(falla -> {
                registrarBaseCaida(operacion, falla);
                return new Uso(0, 0);
            })
            .chain(uso -> {
                if (uso.usos() <= maximo) {
                    return Uni.createFrom().voidItem();
                }
                LogCodificado.warn(LOG, CodigoLogComun.LIMITE_EXCEDIDO, null, ambito, operacion.nombre(), usuario);
                return Uni.createFrom().failure(new LimiteExcedidoException(uso.segundosRestantes()));
            });
    }

    private static String armarClaveDeEmpresa(OperacionLimitada operacion, String ruc) {
        return operacion.nombre() + SEPARADOR + ruc;
    }

    private static String armarClaveDeUsuario(OperacionLimitada operacion, Object usuario, String ruc) {
        return armarClaveDeEmpresa(operacion, ruc == null ? CUALQUIER_EMPRESA : ruc) + SEPARADOR + usuario;
    }

    private static void registrarBaseCaida(OperacionLimitada operacion, Throwable falla) {
        LogCodificado.warn(LOG, CodigoLogComun.LIMITE_SIN_CONTADOR, null, operacion.nombre(), falla.toString());
    }
}
