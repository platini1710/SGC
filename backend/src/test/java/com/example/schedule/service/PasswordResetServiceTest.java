package com.example.schedule.service;

import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.PasswordResetToken;
import com.example.schedule.model.Profesor;
import com.example.schedule.model.Usuario;
import com.example.schedule.repository.PasswordResetTokenRepository;
import com.example.schedule.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private LoginAttemptService loginAttemptService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(usuarioRepository, tokenRepository, encoder,
                emailService, loginAttemptService);
        ReflectionTestUtils.setField(service, "exposeCode", true);
    }

    private Usuario docente(String email) {
        Profesor p = Profesor.builder().id(3L).nombre("Ana").apellido("P").email(email).build();
        return Usuario.builder().id(7L).username("rut").passwordHash(encoder.encode("vieja1@"))
                .rol("DOCENTE").activo(true).profesor(p)
                .passwordUpdatedAt(LocalDateTime.now().minusMonths(7)).build();
    }

    @Test
    void solicitarCodigo_guardaTokenYExponeCodigoEnDemo() {
        Usuario u = docente("a@b.cl");
        when(usuarioRepository.findDocenteByProfesorEmailIgnoreCase("a@b.cl")).thenReturn(Optional.of(u));

        PasswordResetService.CodeResult r = service.solicitarCodigo("a@b.cl");

        assertNotNull(r.debugCode());
        assertTrue(r.debugCode().matches("\\d{5}"));
        verify(tokenRepository).invalidarVigentes(u);
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void solicitarCodigo_conCorreoEnviado_noExponeCodigo() {
        Usuario u = docente("a@b.cl");
        when(usuarioRepository.findDocenteByProfesorEmailIgnoreCase("a@b.cl")).thenReturn(Optional.of(u));
        when(emailService.enviar(any(), any(), any())).thenReturn(true);

        PasswordResetService.CodeResult r = service.solicitarCodigo("a@b.cl");

        assertEquals(PasswordResetService.MSG_REENVIADO, r.mensaje());
        org.junit.jupiter.api.Assertions.assertNull(r.debugCode());
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void solicitarCodigo_emailDesconocido_noRevela() {
        when(usuarioRepository.findDocenteByProfesorEmailIgnoreCase("x@b.cl")).thenReturn(Optional.empty());

        PasswordResetService.CodeResult r = service.solicitarCodigo("x@b.cl");

        assertEquals(PasswordResetService.MSG_REENVIADO, r.mensaje());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void confirmarCambio_codigoCorrectoYClaveValida_actualiza() {
        Usuario u = docente("a@b.cl");
        when(usuarioRepository.findDocenteByProfesorEmailIgnoreCase("a@b.cl")).thenReturn(Optional.of(u));
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(u).codeHash(encoder.encode("12345"))
                .expiresAt(LocalDateTime.now().plusMinutes(10)).used(false)
                .createdAt(LocalDateTime.now()).build();
        when(tokenRepository.findTopByUsuarioAndUsedFalseOrderByCreatedAtDesc(u)).thenReturn(Optional.of(token));

        service.confirmarCambio("a@b.cl", "12345", "Nueva1@");

        assertTrue(token.isUsed());
        assertTrue(encoder.matches("Nueva1@", u.getPasswordHash()));
        assertFalse(u.isMustChangePassword());
        assertTrue(u.getPasswordUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(cap.capture());
        assertEquals(7L, cap.getValue().getId());
        // Cambiar la contraseña también levanta el bloqueo por intentos fallidos.
        verify(loginAttemptService).desbloquear(7L);
    }

    @Test
    void confirmarCambio_codigoIncorrecto_lanza422() {
        Usuario u = docente("a@b.cl");
        when(usuarioRepository.findDocenteByProfesorEmailIgnoreCase("a@b.cl")).thenReturn(Optional.of(u));
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(u).codeHash(encoder.encode("99999"))
                .expiresAt(LocalDateTime.now().plusMinutes(10)).used(false)
                .createdAt(LocalDateTime.now()).build();
        when(tokenRepository.findTopByUsuarioAndUsedFalseOrderByCreatedAtDesc(u)).thenReturn(Optional.of(token));

        SchedulingException ex = assertThrows(SchedulingException.class,
                () -> service.confirmarCambio("a@b.cl", "12345", "Nueva1@"));
        assertEquals(PasswordResetService.MSG_CODIGO_INVALIDO, ex.getDetalle());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void confirmarCambio_codigoExpirado_lanza422() {
        Usuario u = docente("a@b.cl");
        when(usuarioRepository.findDocenteByProfesorEmailIgnoreCase("a@b.cl")).thenReturn(Optional.of(u));
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(u).codeHash(encoder.encode("12345"))
                .expiresAt(LocalDateTime.now().minusMinutes(1)).used(false)
                .createdAt(LocalDateTime.now().minusMinutes(20)).build();
        when(tokenRepository.findTopByUsuarioAndUsedFalseOrderByCreatedAtDesc(u)).thenReturn(Optional.of(token));

        assertThrows(SchedulingException.class,
                () -> service.confirmarCambio("a@b.cl", "12345", "Nueva1@"));
    }

    @Test
    void confirmarCambio_claveDebil_lanza422ConMensajeDePolitica() {
        Usuario u = docente("a@b.cl");
        when(usuarioRepository.findDocenteByProfesorEmailIgnoreCase("a@b.cl")).thenReturn(Optional.of(u));
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(u).codeHash(encoder.encode("12345"))
                .expiresAt(LocalDateTime.now().plusMinutes(10)).used(false)
                .createdAt(LocalDateTime.now()).build();
        when(tokenRepository.findTopByUsuarioAndUsedFalseOrderByCreatedAtDesc(u)).thenReturn(Optional.of(token));

        SchedulingException ex = assertThrows(SchedulingException.class,
                () -> service.confirmarCambio("a@b.cl", "12345", "debil"));
        assertEquals(PasswordPolicyService.MSG_DEBIL, ex.getDetalle());
        assertFalse(token.isUsed());
        verify(usuarioRepository, never()).save(any());
    }
}
