package com.facturacion.comprobantes.infrastructure.ticket;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.facturacion.comprobantes.domain.model.vo.AnchoTicket;
import com.facturacion.comprobantes.domain.port.AnchosTicket;

/**
 * Los anchos de papel salen de {@link ConfiguracionTicket}. Una configuración sin anchos, con columnas no positivas o
 * cuyo ancho por defecto no esté entre los anchos falla al arrancar: mejor que descubrirlo al imprimir.
 */
@ApplicationScoped
public class AnchosTicketConfigurados implements AnchosTicket {

    private static final String ERROR_SIN_ANCHOS = "ticket.anchos debe declarar al menos un ancho de papel";
    private static final String ERROR_COLUMNAS = "ticket.anchos.%d debe tener columnas positivas, no %d";
    private static final String ERROR_POR_DEFECTO = "ticket.ancho-por-defecto=%d no está entre ticket.anchos %s";

    private final TreeMap<Integer, AnchoTicket> anchos = new TreeMap<>();
    private final AnchoTicket porDefecto;

    @Inject
    public AnchosTicketConfigurados(ConfiguracionTicket configuracion) {
        if (configuracion.anchos().isEmpty()) {
            throw new IllegalStateException(ERROR_SIN_ANCHOS);
        }
        configuracion.anchos().forEach((milimetros, columnas) -> {
            if (columnas <= 0) {
                throw new IllegalStateException(ERROR_COLUMNAS.formatted(milimetros, columnas));
            }
            anchos.put(milimetros, new AnchoTicket(milimetros, columnas));
        });
        this.porDefecto = anchos.get(configuracion.anchoPorDefecto());
        if (porDefecto == null) {
            throw new IllegalStateException(
                ERROR_POR_DEFECTO.formatted(configuracion.anchoPorDefecto(), anchos.keySet()));
        }
    }

    @Override
    public Optional<AnchoTicket> buscar(int milimetros) {
        return Optional.ofNullable(anchos.get(milimetros));
    }

    @Override
    public AnchoTicket obtenerPorDefecto() {
        return porDefecto;
    }

    @Override
    public List<Integer> listarDisponibles() {
        return List.copyOf(anchos.keySet());
    }
}
