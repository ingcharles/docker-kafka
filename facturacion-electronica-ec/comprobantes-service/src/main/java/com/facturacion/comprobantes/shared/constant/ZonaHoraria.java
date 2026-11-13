package com.facturacion.comprobantes.shared.constant;

import java.time.ZoneOffset;

public final class ZonaHoraria {

    /** Hora de Ecuador continental (UTC-5): la que usa el SRI y contra la que rigen las vigencias de los catálogos. */
    public static final ZoneOffset ECUADOR = ZoneOffset.of("-05:00");

    private ZonaHoraria() {
    }
}
