package com.facturacion.commons.time;

import java.time.ZoneOffset;

/** Zona horaria con la que se trabajan todas las fechas del sistema. */
public final class ZonaHoraria {

    /** Hora de Ecuador continental (UTC-5, sin horario de verano): la que usa el SRI y contra la que rigen las vigencias de los catálogos. */
    public static final ZoneOffset ECUADOR = ZoneOffset.ofHours(-5);

    private ZonaHoraria() {
    }
}
