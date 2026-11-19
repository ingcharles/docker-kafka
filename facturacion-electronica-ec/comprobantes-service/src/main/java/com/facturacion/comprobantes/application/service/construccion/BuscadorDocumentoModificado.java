package com.facturacion.comprobantes.application.service.construccion;

import java.util.EnumSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.domain.service.DocumentoModificadoValidador.NumeroDocumento;

/**
 * Busca, para una nota de crédito o de débito, el documento que modifica cuando lo emitió este sistema para la empresa.
 * Solo se buscan los tipos que este sistema emite y una nota puede modificar; cualquier otro (o un documento que no está
 * aquí) da {@code null}: no hay nada que comprobar. La fila queda bloqueada hasta el final de la transacción.
 */
@ApplicationScoped
public class BuscadorDocumentoModificado {

    private static final Set<TipoComprobante> TIPOS_COMPROBABLES = EnumSet.of(TipoComprobante.FACTURA,
        TipoComprobante.LIQUIDACION_COMPRA);

    private final ComprobanteRepository comprobanteRepository;

    @Inject
    public BuscadorDocumentoModificado(ComprobanteRepository comprobanteRepository) {
        this.comprobanteRepository = comprobanteRepository;
    }

    /** El documento emitido por este sistema, o {@code null} si no es de un tipo comprobable o no existe. */
    public Uni<Comprobante> buscar(String empresaRuc, String docModificadoTipo, NumeroDocumento numero) {
        return TipoComprobante.buscarPorCodigo(docModificadoTipo).filter(TIPOS_COMPROBABLES::contains)
            .map(tipo -> comprobanteRepository.buscarParaModificar(empresaRuc, tipo, numero.establecimiento(),
                numero.puntoEmision(), numero.secuencial()))
            .orElseGet(() -> Uni.createFrom().nullItem());
    }
}
