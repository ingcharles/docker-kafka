package com.facturacion.notificacion.domain.port;

import java.math.BigDecimal;

import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.vo.LogoEmpresa;

/** Puerto de salida: envío del comprobante autorizado por correo (SMTP). */
public interface CorreoSalida {

    /**
     * Ajustes de la empresa que cambian el correo; todos opcionales (sin ellos sale el correo por defecto).
     *
     * @param asunto          asunto propio (clave {@code notificacion.asunto}); reemplaza al de la plantilla
     * @param remitenteNombre nombre mostrado del remitente (clave {@code notificacion.remitente_nombre}); solo cambia el
     *                        nombre mostrado, nunca la dirección real de envío
     * @param nombreEmisor    nombre de la empresa para el texto del correo: el del remitente, o el nombre comercial, o la razón
     *                        social
     * @param logo            logo de la empresa (ajuste {@code ride.logo}); sin él, el correo muestra el nombre de la empresa
     * @param plantilla       plantilla propia vigente para este tipo (o la genérica de la empresa); {@code null} usa la de archivo
     */
    record Ajustes(String asunto, String remitenteNombre, String nombreEmisor, LogoEmpresa logo,
            PlantillaCorreoEmpresa plantilla) {

        /** Una empresa sin ningún ajuste. */
        public static Ajustes ninguno() {
            return new Ajustes(null, null, null, null, null);
        }
    }

    /**
     * Lo que lleva el correo de un comprobante autorizado.
     *
     * @param destinatario    correo del receptor
     * @param claveAcceso     clave de acceso de 49 dígitos (de ella salen el número, la fecha de emisión y el RUC del texto)
     * @param tipoComprobante nombre del tipo ({@code FACTURA}, {@code NOTA_CREDITO}...); uno desconocido usa la plantilla genérica
     * @param nombreReceptor  razón social del receptor para el saludo ({@code null} o vacío: saludo sin nombre)
     * @param importeTotal    total del comprobante ({@code null} si no se conoce)
     * @param ridePdf         el RIDE en PDF
     * @param xmlAutorizado   el XML autorizado completo devuelto por el SRI
     * @param ajustes         ajustes de la empresa
     */
    record Comprobante(String destinatario, String claveAcceso, String tipoComprobante, String nombreReceptor,
            BigDecimal importeTotal, byte[] ridePdf, String xmlAutorizado, Ajustes ajustes) {
    }

    Uni<Void> enviarComprobante(Comprobante comprobante);
}
