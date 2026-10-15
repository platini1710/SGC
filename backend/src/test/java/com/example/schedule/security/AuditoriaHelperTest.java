package com.example.schedule.security;

import com.example.schedule.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditoriaHelperTest {

    private final AuditoriaHelper helper = new AuditoriaHelper();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinContextoDevuelveSistema() {
        assertEquals("sistema", helper.usuarioActual());
    }

    @Test
    void conAutenticacionDevuelveUsername() {
        Usuario usuario = Usuario.builder()
                .username("12.345.678-9").passwordHash("x").rol("DOCENTE").activo(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));

        assertEquals("12.345.678-9", helper.usuarioActual());
    }
}
