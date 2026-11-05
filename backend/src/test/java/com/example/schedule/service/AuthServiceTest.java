package com.example.schedule.service;

import com.example.schedule.dto.LoginRequest;
import com.example.schedule.dto.LoginResponse;
import com.example.schedule.exception.DemasiadosIntentosException;
import com.example.schedule.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    private LoginRequest req(String password) {
        return LoginRequest.builder().username("docente").password(password).build();
    }

    @Test
    void loginCorrecto_reiniciaContadorYEmiteToken() {
        Usuario u = Usuario.builder().username("docente").passwordHash("x").rol("DOCENTE").activo(true).build();
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
        when(jwtService.generateToken(u)).thenReturn("jwt");

        LoginResponse r = authService.login(req("ok"));

        assertEquals("jwt", r.getToken());
        verify(loginAttemptService).registrarExito("docente");
    }

    @Test
    void passwordIncorrecta_sumaFalloYPropagaCredencialesInvalidas() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));
        when(loginAttemptService.registrarFallo("docente")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(req("mala")));
        verify(loginAttemptService).registrarFallo("docente");
    }

    @Test
    void tercerFallo_respondeCuentaBloqueada() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));
        when(loginAttemptService.registrarFallo("docente")).thenReturn(true);
        when(loginAttemptService.mensajeBloqueo("docente")).thenReturn("bloqueada");

        DemasiadosIntentosException ex = assertThrows(DemasiadosIntentosException.class,
                () -> authService.login(req("mala")));
        assertEquals("bloqueada", ex.getMessage());
    }

    @Test
    void cuentaYaBloqueada_rechazaSinSumarFallo() {
        // Spring lanza LockedException antes de revisar la contraseña.
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));
        when(loginAttemptService.mensajeBloqueo("docente")).thenReturn("bloqueada");

        assertThrows(DemasiadosIntentosException.class, () -> authService.login(req("correcta")));
        verify(loginAttemptService, never()).registrarFallo(anyString());
        verify(jwtService, never()).generateToken(any());
    }
}
