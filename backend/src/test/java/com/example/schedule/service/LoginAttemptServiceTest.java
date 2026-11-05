package com.example.schedule.service;

import com.example.schedule.model.Usuario;
import com.example.schedule.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(usuarioRepository);
    }

    private Usuario docente() {
        return Usuario.builder().id(7L).username("12.345.678-9").passwordHash("x").rol("DOCENTE").activo(true).build();
    }

    private Usuario admin() {
        return Usuario.builder().id(1L).username("admin").passwordHash("x").rol("ADMIN").activo(true).build();
    }

    @Test
    void docente_tercerFallo_bloqueoPermanente() {
        Usuario u = docente();
        when(usuarioRepository.findByUsername(u.getUsername())).thenReturn(Optional.of(u));

        assertFalse(service.registrarFallo(u.getUsername()));
        assertFalse(service.registrarFallo(u.getUsername()));
        assertTrue(u.isAccountNonLocked());

        assertTrue(service.registrarFallo(u.getUsername()));
        assertTrue(u.isBloqueado());
        assertFalse(u.isAccountNonLocked());
        assertNull(u.getBloqueadoHasta());
        assertEquals(LoginAttemptService.MSG_PERMANENTE, service.mensajeBloqueo(u.getUsername()));
    }

    @Test
    void admin_tercerFallo_bloqueoTemporalDe15Minutos() {
        Usuario u = admin();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));

        service.registrarFallo("admin");
        service.registrarFallo("admin");
        assertTrue(service.registrarFallo("admin"));

        assertFalse(u.isBloqueado());
        assertNotNull(u.getBloqueadoHasta());
        assertTrue(u.getBloqueadoHasta().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertFalse(u.isAccountNonLocked());
        assertEquals(LoginAttemptService.MSG_TEMPORAL, service.mensajeBloqueo("admin"));
    }

    @Test
    void admin_bloqueoTemporalVencido_seLimpiaYCuentaDeNuevo() {
        Usuario u = admin();
        u.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        u.setIntentosFallidos(0);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));

        assertTrue(u.isAccountNonLocked());
        assertFalse(service.registrarFallo("admin"));
        assertNull(u.getBloqueadoHasta());
        assertEquals(1, u.getIntentosFallidos());
    }

    @Test
    void exito_reiniciaElContador() {
        Usuario u = docente();
        u.setIntentosFallidos(2);
        when(usuarioRepository.findByUsername(u.getUsername())).thenReturn(Optional.of(u));

        service.registrarExito(u.getUsername());

        assertEquals(0, u.getIntentosFallidos());
        verify(usuarioRepository).save(u);
    }

    @Test
    void exito_sinFallosPrevios_noEscribe() {
        Usuario u = docente();
        when(usuarioRepository.findByUsername(u.getUsername())).thenReturn(Optional.of(u));

        service.registrarExito(u.getUsername());

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioInexistente_noHaceNada() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertFalse(service.registrarFallo("nadie"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desbloquear_limpiaTodo() {
        Usuario u = docente();
        u.setBloqueado(true);
        u.setIntentosFallidos(3);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(u));

        service.desbloquear(7L);

        assertFalse(u.isBloqueado());
        assertEquals(0, u.getIntentosFallidos());
        assertTrue(u.isAccountNonLocked());
        verify(usuarioRepository).save(u);
    }
}
