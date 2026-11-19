package com.facturacion.comprobantes.application.service.construccion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.commons.time.FechasSri;
import com.facturacion.comprobantes.application.dto.CrearNotaDebitoRequest;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.domain.service.DocumentoModificadoValidador;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * La nota de débito se comprueba contra el documento que modifica: un número o una fecha mal puestos fallan sin consultar
 * la base, y solo se busca un documento de los tipos que este sistema emite.
 */
class ConstructorNotaDebitoTest {

    private static final String RUC = "1722039953001";
    private static final String CODIGO_FACTURA = "01";
    private static final String CODIGO_NOTA_CREDITO = "04";

    private final List<String> consultas = new ArrayList<>();
    private final CatalogosSriAFecha catalogos = new CatalogosSriAFecha(FechasSri.obtenerHoy(), List.of(), List.of(),
        List.of(), List.of(), List.of(), List.of(), List.of());

    /** Repositorio de prueba: anota cada búsqueda y devuelve el documento indicado (o ninguno). */
    private ComprobanteRepository repositorio(Comprobante encontrado) {
        return (ComprobanteRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {ComprobanteRepository.class}, (proxy, metodo, args) -> {
                if (!metodo.getName().equals("buscarParaModificar")) {
                    throw new UnsupportedOperationException(metodo.getName());
                }
                consultas.add(args[1] + " " + args[2] + "-" + args[3] + "-" + args[4]);
                return Uni.createFrom().item(encontrado);
            });
    }

    private ConstructorNotaDebito constructor() {
        return new ConstructorNotaDebito(new ConstructorComun(), new BuscadorDocumentoModificado(repositorio(null)));
    }

    private CrearNotaDebitoRequest nota(String numero, String fecha) {
        CrearNotaDebitoRequest request = new CrearNotaDebitoRequest();
        request.setDocModificadoTipo(CODIGO_FACTURA);
        request.setDocModificadoNumero(numero);
        request.setDocModificadoFecha(fecha);
        return request;
    }

    @Test
    void unNumeroDeDocumentoMalFormadoSeRechazaSinConsultarLaBase() {
        ErrorNegocioException error = assertThrows(ErrorNegocioException.class,
            () -> constructor().construir(nota("1-1-1", FechasSri.formatearFecha(FechasSri.obtenerHoy())), 1L, catalogos));

        assertEquals(CodigoError.DOCUMENTO_MODIFICADO_NUMERO_INVALIDO, error.getError());
        assertTrue(consultas.isEmpty());
    }

    @Test
    void unaFechaDeDocumentoFuturaSeRechazaSinConsultarLaBase() {
        String manana = FechasSri.formatearFecha(FechasSri.obtenerHoy().plusDays(1));

        ErrorNegocioException error = assertThrows(ErrorNegocioException.class,
            () -> constructor().construir(nota("001-001-000000001", manana), 1L, catalogos));

        assertEquals(CodigoError.DOCUMENTO_MODIFICADO_FECHA_FUTURA, error.getError());
        assertTrue(consultas.isEmpty());
    }

    @Test
    void unaFacturaSeBuscaPorSuNumeroEnLaBase() {
        BuscadorDocumentoModificado buscador = new BuscadorDocumentoModificado(repositorio(null));

        Comprobante encontrado = buscador.buscar(RUC, CODIGO_FACTURA,
            DocumentoModificadoValidador.interpretarNumero("001-002-000000123")).await().atMost(Duration.ofSeconds(2));

        assertEquals(null, encontrado);
        assertEquals(List.of(TipoComprobante.FACTURA + " 001-002-000000123"), consultas);
    }

    @Test
    void unTipoQueEsteSistemaNoComprueba_noSeBusca() {
        BuscadorDocumentoModificado buscador = new BuscadorDocumentoModificado(repositorio(new Comprobante()));

        Comprobante encontrado = buscador.buscar(RUC, CODIGO_NOTA_CREDITO,
            DocumentoModificadoValidador.interpretarNumero("001-002-000000123")).await().atMost(Duration.ofSeconds(2));

        assertEquals(null, encontrado);
        assertTrue(consultas.isEmpty());
    }
}
