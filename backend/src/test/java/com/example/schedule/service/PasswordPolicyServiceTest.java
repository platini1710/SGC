package com.example.schedule.service;

import com.example.schedule.model.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyServiceTest {

    private Usuario docente() {
        return Usuario.builder().username("rut").passwordHash("x").rol("DOCENTE").activo(true).build();
    }

    private Usuario admin() {
        return Usuario.builder().username("admin").passwordHash("x").rol("ADMIN").activo(true).build();
    }

    @Test
    void complejidad_docente_rechazaDebiles() {
        Usuario d = docente();
        assertFalse(PasswordPolicyService.cumpleComplejidad(d, "abcdef"));   // sin número ni especial
        assertFalse(PasswordPolicyService.cumpleComplejidad(d, "abc123"));   // sin especial
        assertFalse(PasswordPolicyService.cumpleComplejidad(d, "ab1@"));     // menos de 6
        assertFalse(PasswordPolicyService.cumpleComplejidad(d, null));
    }

    @Test
    void complejidad_docente_aceptaValidas() {
        Usuario d = docente();
        assertTrue(PasswordPolicyService.cumpleComplejidad(d, "abc1@d"));
        assertTrue(PasswordPolicyService.cumpleComplejidad(d, "Clave2025!"));
    }

    @Test
    void complejidad_admin_exento() {
        assertTrue(PasswordPolicyService.cumpleComplejidad(admin(), "admin"));
        assertTrue(PasswordPolicyService.cumpleComplejidad(admin(), "1234"));
    }

    @Test
    void expiracion_docenteMasDe6Meses_expira() {
        Usuario d = docente();
        d.setPasswordUpdatedAt(LocalDateTime.now().minusMonths(6).minusDays(1));
        assertTrue(PasswordPolicyService.estaExpirada(d));
        assertTrue(PasswordPolicyService.debeCambiar(d));
    }

    @Test
    void expiracion_docenteReciente_noExpira() {
        Usuario d = docente();
        d.setPasswordUpdatedAt(LocalDateTime.now().minusMonths(1));
        assertFalse(PasswordPolicyService.estaExpirada(d));
        assertFalse(PasswordPolicyService.debeCambiar(d));
    }

    @Test
    void expiracion_sinFecha_noFuerza() {
        Usuario d = docente();
        d.setPasswordUpdatedAt(null);
        assertFalse(PasswordPolicyService.estaExpirada(d));
    }

    @Test
    void expiracion_adminNoExpiraAunqueSeaAntiguo() {
        Usuario a = admin();
        a.setPasswordUpdatedAt(LocalDateTime.now().minusYears(2));
        assertFalse(PasswordPolicyService.estaExpirada(a));
    }

    @Test
    void debeCambiar_banderaManual() {
        Usuario d = docente();
        d.setPasswordUpdatedAt(LocalDateTime.now());
        d.setMustChangePassword(true);
        assertTrue(PasswordPolicyService.debeCambiar(d));
    }
}
