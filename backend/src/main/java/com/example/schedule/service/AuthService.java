package com.example.schedule.service;

import com.example.schedule.dto.LoginRequest;
import com.example.schedule.dto.LoginResponse;
import com.example.schedule.exception.DemasiadosIntentosException;
import com.example.schedule.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        } catch (LockedException e) {
            // Cuenta ya bloqueada: se rechaza aunque la contraseña sea correcta.
            throw new DemasiadosIntentosException(loginAttemptService.mensajeBloqueo(username));
        } catch (BadCredentialsException e) {
            if (loginAttemptService.registrarFallo(username)) {
                throw new DemasiadosIntentosException(loginAttemptService.mensajeBloqueo(username));
            }
            throw e;
        }

        Usuario usuario = (Usuario) authentication.getPrincipal();
        loginAttemptService.registrarExito(usuario.getUsername());
        return emitirRespuesta(usuario);
    }

    // Genera el JWT y la respuesta de login para un usuario ya autenticado (contraseña o rostro).
    public LoginResponse emitirRespuesta(Usuario usuario) {
        String token = jwtService.generateToken(usuario);

        return LoginResponse.builder()
                .token(token)
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .profesorId(usuario.getProfesor() != null ? usuario.getProfesor().getId() : null)
                .mustChangePassword(PasswordPolicyService.debeCambiar(usuario))
                .build();
    }
}
