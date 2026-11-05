package com.example.schedule.service;

import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Usuario;
import com.example.schedule.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contador de intentos de login fallidos, persistido en la tabla usuario.
 * Contraseña y rostro comparten el mismo contador.
 * Al llegar a MAX_INTENTOS:
 *  - DOCENTE: bloqueo permanente (solo un ADMIN lo quita).
 *  - ADMIN:   bloqueo temporal de BLOQUEO_ADMIN, para no dejar el sistema sin administrador.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final int MAX_INTENTOS = 3;
    public static final Duration BLOQUEO_ADMIN = Duration.ofMinutes(15);

    static final String MSG_PERMANENTE =
            "Cuenta bloqueada por " + MAX_INTENTOS + " intentos fallidos. Contacte al administrador.";
    static final String MSG_TEMPORAL =
            "Cuenta bloqueada temporalmente por " + MAX_INTENTOS
                    + " intentos fallidos. Intente nuevamente en " + BLOQUEO_ADMIN.toMinutes() + " minutos.";

    private final UsuarioRepository usuarioRepository;

    /**
     * Suma un intento fallido. Devuelve true si con este intento la cuenta quedó bloqueada.
     * Si el usuario no existe no hace nada (no hay cuenta que bloquear).
     */
    @Transactional
    public boolean registrarFallo(String username) {
        return usuarioRepository.findByUsername(username)
                .map(this::sumarFallo)
                .orElse(false);
    }

    private boolean sumarFallo(Usuario u) {
        // Un bloqueo temporal ya vencido se limpia y el conteo parte de cero.
        if (u.getBloqueadoHasta() != null && !u.bloqueoTemporalVigente()) {
            u.setBloqueadoHasta(null);
            u.setIntentosFallidos(0);
        }
        int intentos = u.getIntentosFallidos() + 1;
        boolean bloquear = intentos >= MAX_INTENTOS;
        if (bloquear && esAdmin(u)) {
            u.setBloqueadoHasta(LocalDateTime.now().plus(BLOQUEO_ADMIN));
            u.setIntentosFallidos(0);
        } else {
            u.setIntentosFallidos(intentos);
            if (bloquear) {
                u.setBloqueado(true);
            }
        }
        usuarioRepository.save(u);
        return bloquear;
    }

    /** Login correcto: reinicia el contador. */
    @Transactional
    public void registrarExito(String username) {
        usuarioRepository.findByUsername(username).ifPresent(u -> {
            if (u.getIntentosFallidos() != 0 || u.getBloqueadoHasta() != null) {
                u.setIntentosFallidos(0);
                u.setBloqueadoHasta(null);
                usuarioRepository.save(u);
            }
        });
    }

    /** Mensaje a mostrar en el login para una cuenta bloqueada. */
    public String mensajeBloqueo(String username) {
        return usuarioRepository.findByUsername(username)
                .map(u -> u.isBloqueado() ? MSG_PERMANENTE : MSG_TEMPORAL)
                .orElse(MSG_PERMANENTE);
    }

    @Transactional(readOnly = true)
    public List<Usuario> bloqueados() {
        return usuarioRepository.findByBloqueadoTrueOrBloqueadoHastaAfterOrderByUsernameAsc(LocalDateTime.now());
    }

    @Transactional
    public void desbloquear(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        u.setBloqueado(false);
        u.setBloqueadoHasta(null);
        u.setIntentosFallidos(0);
        usuarioRepository.save(u);
    }

    private boolean esAdmin(Usuario u) {
        return "ADMIN".equals(u.getRol());
    }
}
