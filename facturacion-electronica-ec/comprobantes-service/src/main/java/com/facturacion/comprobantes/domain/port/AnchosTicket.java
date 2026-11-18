package com.facturacion.comprobantes.domain.port;

import java.util.List;
import java.util.Optional;

import com.facturacion.comprobantes.domain.model.vo.AnchoTicket;

/** Puerto de salida: los anchos de papel que se pueden pedir para el ticket y el que se usa si no se pide ninguno. */
public interface AnchosTicket {

    Optional<AnchoTicket> buscar(int milimetros);

    AnchoTicket obtenerPorDefecto();

    /** Milímetros disponibles, de menor a mayor (para el mensaje cuando piden uno que no existe). */
    List<Integer> listarDisponibles();
}
