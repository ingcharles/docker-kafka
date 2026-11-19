package com.facturacion.notificacion.infrastructure.mail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.domain.model.vo.ContenidoCorreo;
import com.facturacion.notificacion.domain.model.vo.LogoEmpresa;
import com.facturacion.notificacion.domain.port.ArmadorContenidoCorreo;
import com.facturacion.notificacion.domain.port.CorreoSalida;

/**
 * Envío real (no simulado) del comprobante autorizado por correo, con el
 * RIDE en PDF y el XML autorizado (el envoltorio {@code <autorizacion>}
 * completo devuelto por el SRI) adjuntos — usa la extensión oficial de
 * Quarkus Mailer en vez de armar el protocolo SMTP a mano (a diferencia del
 * SOAP del SRI o la firma XAdES-BES, acá sí hay un stack estándar que
 * encaja perfecto, no hay motivo para reinventarlo). El SRI exige que el
 * receptor pueda quedarse con el XML autorizado, no solo con la
 * representación impresa — mandar únicamente el PDF no cumple con eso.
 * El asunto y el cuerpo (HTML con alternativa en texto plano) los arma {@link ArmadorContenidoCorreo}; si el HTML usa el
 * logo de la empresa, la imagen viaja incrustada en el mismo correo (no como adjunto ni como enlace externo, que muchos
 * clientes bloquean).
 * El envío en sí pasa antes por {@link MailRateLimiter} para no mandar
 * muchos correos de golpe (ver su javadoc).
 */
@ApplicationScoped
public class CorreoService implements CorreoSalida {

    private static final java.nio.charset.Charset UTF8 = java.nio.charset.StandardCharsets.UTF_8;
    private static final String NOMBRE_LOGO_INCRUSTADO = "logo";
    private static final String REFERENCIA_AL_LOGO = "cid:" + LogoEmpresa.CID;
    private static final String IDENTIFICADOR_DEL_LOGO = "<" + LogoEmpresa.CID + ">";

    private final ReactiveMailer mailer;
    private final MailRateLimiter rateLimiter;
    private final ArmadorContenidoCorreo armador;

    @ConfigProperty(name = "quarkus.mailer.from")
    String remitenteDefecto;

    @ConfigProperty(name = "notificacion.correo.timeout")
    java.time.Duration timeoutEnvio;

    @Inject
    public CorreoService(ReactiveMailer mailer, MailRateLimiter rateLimiter, ArmadorContenidoCorreo armador) {
        this.mailer = mailer;
        this.rateLimiter = rateLimiter;
        this.armador = armador;
    }

    @Override
    public Uni<Void> enviarComprobante(CorreoSalida.Comprobante comprobante) {
        ContenidoCorreo contenido = armador.armar(comprobante);
        Mail correo = Mail.withHtml(comprobante.destinatario(), contenido.asunto(), contenido.html())
            .setText(contenido.texto())
            .addAttachment(comprobante.claveAcceso() + ".pdf", comprobante.ridePdf(), "application/pdf")
            .addAttachment(comprobante.claveAcceso() + ".xml", comprobante.xmlAutorizado().getBytes(UTF8), "application/xml");
        LogoEmpresa logo = comprobante.ajustes().logo();
        if (logo != null && contenido.html().contains(REFERENCIA_AL_LOGO)) {
            correo.addInlineAttachment(NOMBRE_LOGO_INCRUSTADO, logo.datos(), logo.tipoContenido(), IDENTIFICADOR_DEL_LOGO);
        }
        String remitenteNombre = comprobante.ajustes().remitenteNombre();
        if (remitenteNombre != null && !remitenteNombre.isBlank()) {
            correo.setFrom(remitenteNombre + " <" + remitenteDefecto + ">");
        }
        // El plazo cuenta solo el envío (no la espera de turno del limitador): un SMTP que no contesta no cuelga el consumidor.
        return rateLimiter.esperarTurno().chain(() -> mailer.send(correo).ifNoItem().after(timeoutEnvio).fail());
    }
}
