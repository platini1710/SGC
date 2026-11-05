package com.example.schedule.service;

import com.example.schedule.dto.FaceEnrollRequest;
import com.example.schedule.dto.FaceLoginRequest;
import com.example.schedule.dto.LoginResponse;
import com.example.schedule.exception.DemasiadosIntentosException;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Usuario;
import com.example.schedule.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login facial por verificación 1:1: el cliente envía usuario + descriptor (128 valores de
 * face-api.js) y el backend lo compara por distancia euclidiana con el descriptor registrado.
 */
@Service
public class FaceAuthService {

    public static final int DESCRIPTOR_SIZE = 128;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final LoginAttemptService loginAttempts;
    private final ObjectMapper objectMapper;
    private final double umbral;

    public FaceAuthService(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           AuthService authService,
                           LoginAttemptService loginAttempts,
                           ObjectMapper objectMapper,
                           @Value("${schedule.face.threshold:0.5}") double umbral) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.loginAttempts = loginAttempts;
        this.objectMapper = objectMapper;
        this.umbral = umbral;
    }

    @Transactional
    public void registrar(String username, FaceEnrollRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        // Reconfirma la contraseña: un token robado no debe poder registrar un rostro ajeno.
        // 403 (no 401): la sesión es válida; un 401 haría que el cliente cierre la sesión.
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new AccessDeniedException("La contraseña es incorrecta");
        }
        try {
            validarDescriptor(request.getDescriptor());
        } catch (IllegalArgumentException e) {
            throw new SchedulingException(e.getMessage());
        }
        usuario.setFaceDescriptor(serializar(request.getDescriptor()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(String username) {
        usuarioRepository.findByUsername(username).ifPresent(u -> {
            u.setFaceDescriptor(null);
            usuarioRepository.save(u);
        });
    }

    public boolean tieneRostro(String username) {
        return usuarioRepository.findByUsername(username)
                .map(u -> u.getFaceDescriptor() != null && !u.getFaceDescriptor().isBlank())
                .orElse(false);
    }

    public LoginResponse login(FaceLoginRequest request) {
        String username = request.getUsername();
        Usuario encontrado = usuarioRepository.findByUsername(username).orElse(null);

        // Cuenta ya bloqueada: se rechaza aunque el rostro coincida.
        if (encontrado != null && !encontrado.isAccountNonLocked()) {
            throw new DemasiadosIntentosException(loginAttempts.mensajeBloqueo(username));
        }

        boolean valido = encontrado != null
                && encontrado.isEnabled()
                && encontrado.getFaceDescriptor() != null && !encontrado.getFaceDescriptor().isBlank()
                && coincide(request.getDescriptor(), encontrado.getFaceDescriptor());

        // Mismo error para usuario inexistente, sin rostro o rostro distinto (evita enumerar usuarios).
        if (!valido) {
            if (loginAttempts.registrarFallo(username)) {
                throw new DemasiadosIntentosException(loginAttempts.mensajeBloqueo(username));
            }
            throw new BadCredentialsException("Credenciales inválidas");
        }

        loginAttempts.registrarExito(username);
        return authService.emitirRespuesta(encontrado);
    }

    private boolean coincide(double[] candidato, String almacenadoJson) {
        try {
            validarDescriptor(candidato);
            double[] almacenado = objectMapper.readValue(almacenadoJson, double[].class);
            if (almacenado.length != DESCRIPTOR_SIZE) {
                return false;
            }
            return distancia(candidato, almacenado) <= umbral;
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return false;
        }
    }

    static double distancia(double[] a, double[] b) {
        double suma = 0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            suma += d * d;
        }
        return Math.sqrt(suma);
    }

    private void validarDescriptor(double[] descriptor) {
        if (descriptor == null || descriptor.length != DESCRIPTOR_SIZE) {
            throw new IllegalArgumentException("Descriptor facial inválido");
        }
        for (double v : descriptor) {
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException("Descriptor facial inválido");
            }
        }
    }

    private String serializar(double[] descriptor) {
        try {
            return objectMapper.writeValueAsString(descriptor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el descriptor facial", e);
        }
    }
}
