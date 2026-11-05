package com.example.schedule.service;

import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.PasswordResetToken;
import com.example.schedule.model.Profesor;
import com.example.schedule.model.Usuario;
import com.example.schedule.repository.PasswordResetTokenRepository;
import com.example.schedule.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Flujo de cambio de contraseña con código OTP (RF-01.3).
 * Sin SMTP: el código se escribe en el log y, en modo demo, se devuelve en la respuesta.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration OTP_VIGENCIA = Duration.ofMinutes(15);
    private final SecureRandom random = new SecureRandom();

    public static final String MSG_CODIGO_INVALIDO =
            "El código de seguridad ingresado es inválido o ha expirado. Por favor, intente nuevamente.";
    public static final String MSG_REENVIADO =
            "Hemos enviado un nuevo código de 5 dígitos a su correo electrónico.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    // Modo demo: expone el código en la respuesta para probar sin SMTP. Apagar al conectar correo real.
    @Value("${schedule.otp.expose-code:true}")
    private boolean exposeCode;

    public record CodeResult(String mensaje, String debugCode) {}

    /**
     * Genera y "envía" un código de 5 dígitos al correo del profesor.
     * Responde igual exista o no la cuenta, para no revelar correos registrados.
     */
    @Transactional
    public CodeResult solicitarCodigo(String email) {
        Optional<Usuario> opt = buscarDocentePorEmail(email);
        if (opt.isEmpty()) {
            return new CodeResult(MSG_REENVIADO, null);
        }
        Usuario usuario = opt.get();
        tokenRepository.invalidarVigentes(usuario);

        String code = String.format("%05d", random.nextInt(100_000));
        tokenRepository.save(PasswordResetToken.builder()
                .usuario(usuario)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(LocalDateTime.now().plus(OTP_VIGENCIA))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build());

        boolean enviado = emailService.enviar(
                email,
                "Código de cambio de contraseña",
                "Su código de seguridad es: " + code + "\n"
                        + "Vence en 15 minutos. Si no solicitó este cambio, ignore este correo.");

        if (!enviado) {
            // Sin SMTP: el código queda en el log del servidor como respaldo.
            log.info("OTP cambio de contraseña para {} ({}): {}", usuario.getUsername(), email, code);
        }
        // Expone el código solo en modo demo Y cuando no se envió por correo.
        return new CodeResult(MSG_REENVIADO, (exposeCode && !enviado) ? code : null);
    }

    /**
     * Valida el código y, si es correcto, cambia la contraseña aplicando la política.
     * Lanza SchedulingException (422) con el mensaje de UI correspondiente.
     */
    @Transactional
    public void confirmarCambio(String email, String code, String nuevaPassword) {
        Usuario usuario = buscarDocentePorEmail(email)
                .orElseThrow(() -> new SchedulingException(MSG_CODIGO_INVALIDO));

        PasswordResetToken token = tokenRepository
                .findTopByUsuarioAndUsedFalseOrderByCreatedAtDesc(usuario)
                .filter(PasswordResetToken::estaVigente)
                .filter(t -> passwordEncoder.matches(code, t.getCodeHash()))
                .orElseThrow(() -> new SchedulingException(MSG_CODIGO_INVALIDO));

        if (!PasswordPolicyService.cumpleComplejidad(usuario, nuevaPassword)) {
            throw new SchedulingException(PasswordPolicyService.MSG_DEBIL);
        }

        token.setUsed(true);
        tokenRepository.save(token);

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setPasswordUpdatedAt(LocalDateTime.now());
        usuario.setMustChangePassword(false);
        usuarioRepository.save(usuario);

        // Cambiar la contraseña con el código también levanta el bloqueo por intentos fallidos
        // (permanente o temporal) y reinicia el contador.
        loginAttemptService.desbloquear(usuario.getId());
    }

    private Optional<Usuario> buscarDocentePorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String limpio = email.trim();
        return usuarioRepository.findDocenteByProfesorEmailIgnoreCase(limpio)
                .filter(u -> {
                    Profesor p = u.getProfesor();
                    return p != null && limpio.equalsIgnoreCase(p.getEmail());
                });
    }
}
