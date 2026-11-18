package com.facturacion.comprobantes.infrastructure.xml;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.port.GeneradorXml;
import com.facturacion.comprobantes.domain.service.GeneradorXmlComprobante;
import com.facturacion.comprobantes.shared.exception.TipoComprobanteNoSoportadoException;

/**
 * Elige el {@link GeneradorXmlComprobante} correcto según el tipo — hoy solo
 * hay un adaptador registrado (Factura); agregar Nota de Crédito, Nota de
 * Débito, Guía de Remisión, Retención o Liquidación de Compra es agregar
 * una clase nueva que implemente el puerto, sin tocar este resolver.
 */
@ApplicationScoped
public class GeneradorXmlComprobanteResolver implements GeneradorXml {

    @Inject
    @Any
    Instance<GeneradorXmlComprobante> generadores;

    public String generar(Comprobante comprobante) {
        TipoComprobante tipo = comprobante.getTipoComprobante();
        return generadores.stream()
            .filter(generador -> generador.esTipoSoportado(tipo))
            .findFirst()
            .orElseThrow(() -> new TipoComprobanteNoSoportadoException(tipo))
            .generar(comprobante);
    }
}
