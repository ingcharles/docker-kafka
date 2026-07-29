package com.facturacion.notificacion.infrastructure.mail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;

/**
 * Envío real (no simulado) del comprobante autorizado por correo, con el
 * RIDE en PDF y el XML autorizado (el envoltorio {@code <autorizacion>}
 * completo devuelto por el SRI) adjuntos — usa la extensión oficial de
 * Quarkus Mailer en vez de armar el protocolo SMTP a mano (a diferencia del
 * SOAP del SRI o la firma XAdES-BES, acá sí hay un stack estándar que
 * encaja perfecto, no hay motivo para reinventarlo). El SRI exige que el
 * receptor pueda quedarse con el XML autorizado, no solo con la
 * representación impresa — mandar únicamente el PDF no cumple con eso.
 * El envío en sí pasa antes por {@link MailRateLimiter} para no mandar
 * muchos correos de golpe (ver su javadoc).
 */
@ApplicationScoped
public class CorreoService {

    private static final java.nio.charset.Charset UTF8 = java.nio.charset.StandardCharsets.UTF_8;

    private final ReactiveMailer mailer;
    private final MailRateLimiter rateLimiter;

    @ConfigProperty(name = "quarkus.mailer.from")
    String remitenteDefecto;

    @Inject
    public CorreoService(ReactiveMailer mailer, MailRateLimiter rateLimiter) {
        this.mailer = mailer;
        this.rateLimiter = rateLimiter;
    }

    /**
     * @param asunto           override opcional (clave {@code notificacion.asunto} de
     *                         {@code configuracion_empresa}) — {@code null} usa el asunto por defecto.
     * @param remitenteNombre  override opcional (clave {@code notificacion.remitente_nombre}) — solo
     *                         cambia el nombre mostrado, nunca la dirección real de envío
     *                         ({@code quarkus.mailer.from}, la única autenticada ante el SMTP).
     */
    public Uni<Void> enviarComprobante(String destinatario, String claveAcceso, String tipoComprobante,
            byte[] ridePdf, String xmlAutorizado, String asunto, String remitenteNombre) {
        String asuntoFinal = asunto != null && !asunto.isBlank() ? asunto
            : "Comprobante electrónico autorizado — " + tipoComprobante;
        Mail correo = Mail.withText(destinatario, asuntoFinal,
                "Adjunto encontrará el comprobante electrónico autorizado por el SRI (RIDE en PDF y XML autorizado).\n\n"
                    + "Clave de acceso: " + claveAcceso + "\n")
            .addAttachment(claveAcceso + ".pdf", ridePdf, "application/pdf")
            .addAttachment(claveAcceso + ".xml", xmlAutorizado.getBytes(UTF8), "application/xml");
        if (remitenteNombre != null && !remitenteNombre.isBlank()) {
            correo.setFrom(remitenteNombre + " <" + remitenteDefecto + ">");
        }
        return rateLimiter.esperarTurno().chain(() -> mailer.send(correo));
    }
}
