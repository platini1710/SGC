package com.example.schedule.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de correo. Si no hay SMTP configurado (sin JavaMailSender o sin remitente),
 * queda deshabilitado y el llamador usa el log como respaldo.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender; // null si no hay starter-mail activo con host
    private final String from;

    public EmailService(@org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender,
                        @Value("${schedule.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public boolean habilitado() {
        return mailSender != null && from != null && !from.isBlank();
    }

    /** Envía el correo. Devuelve true si se envió; false si el correo está deshabilitado o falló. */
    public boolean enviar(String destinatario, String asunto, String cuerpo) {
        if (!habilitado()) {
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(destinatario);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            mailSender.send(msg);
            return true;
        } catch (Exception e) {
            // No propagamos: el flujo de OTP no debe caerse por un fallo de SMTP.
            log.warn("No se pudo enviar el correo a {}: {}", destinatario, e.getMessage());
            return false;
        }
    }
}
