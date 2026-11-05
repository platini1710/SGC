package com.example.schedule.service;

import com.example.schedule.model.Usuario;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Reglas de contraseña del RF-01, centralizadas para reusarlas en login y cambio de clave.
 * Solo aplican al perfil DOCENTE; ADMIN queda exento (RF-01.1 y RF-01.2).
 */
public final class PasswordPolicyService {

    public static final int VIGENCIA_MESES = 6;

    // RF-01.6: mínimo 6 caracteres, al menos un número y un carácter especial.
    private static final Pattern DOCENTE_PWD = Pattern.compile(
            "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,}$");

    public static final String MSG_DEBIL =
            "La contraseña debe tener al menos 6 caracteres, incluir al menos un número y un carácter especial.";
    public static final String MSG_EXPIRADA =
            "Su contraseña ha caducado por motivos de seguridad (vigencia superada de 6 meses). "
                    + "Debe actualizarla para continuar.";

    private PasswordPolicyService() {
    }

    public static boolean esDocente(Usuario u) {
        return "DOCENTE".equals(u.getRol());
    }

    public static boolean cumpleComplejidad(Usuario u, String password) {
        if (!esDocente(u)) {
            return true; // ADMIN: directiva propia, sin esta restricción.
        }
        return password != null && DOCENTE_PWD.matcher(password).matches();
    }

    /** true si la contraseña del docente ya superó los 6 meses de vigencia. */
    public static boolean estaExpirada(Usuario u) {
        if (!esDocente(u)) {
            return false;
        }
        LocalDateTime base = u.getPasswordUpdatedAt();
        if (base == null) {
            return false; // sin fecha conocida: no forzamos (la migración la rellena).
        }
        return LocalDateTime.now().isAfter(base.plusMonths(VIGENCIA_MESES));
    }

    /** El login debe forzar el cambio si está marcado o si la contraseña expiró. */
    public static boolean debeCambiar(Usuario u) {
        return u.isMustChangePassword() || estaExpirada(u);
    }
}
