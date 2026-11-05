package com.example.schedule.service;

import com.example.schedule.dto.FaceEnrollRequest;
import com.example.schedule.dto.FaceLoginRequest;
import com.example.schedule.dto.LoginResponse;
import com.example.schedule.exception.DemasiadosIntentosException;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Usuario;
import com.example.schedule.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceAuthServiceTest {

    private static final double UMBRAL = 0.5;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthService authService;
    @Mock
    private LoginAttemptService loginAttempts;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FaceAuthService service;

    @BeforeEach
    void setUp() {
        service = new FaceAuthService(usuarioRepository, passwordEncoder, authService,
                loginAttempts, objectMapper, UMBRAL);
    }

    private double[] descriptor(double valor) {
        double[] d = new double[128];
        Arrays.fill(d, valor);
        return d;
    }

    private Usuario usuarioConRostro(double valor) throws Exception {
        return Usuario.builder().id(1L).username("admin").passwordHash("hash").rol("ADMIN")
                .activo(true).faceDescriptor(objectMapper.writeValueAsString(descriptor(valor))).build();
    }

    private FaceLoginRequest loginReq(String username, double[] d) {
        return FaceLoginRequest.builder().username(username).descriptor(d).build();
    }

    @Test
    void login_rostroCoincide_emiteRespuestaYReiniciaContador() throws Exception {
        Usuario u = usuarioConRostro(0.1);
        LoginResponse esperado = LoginResponse.builder().token("jwt").username("admin").rol("ADMIN").build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(authService.emitirRespuesta(u)).thenReturn(esperado);

        // distancia = sqrt(128 * 0.01^2) ~ 0.113 < 0.5
        LoginResponse r = service.login(loginReq("admin", descriptor(0.11)));

        assertEquals("jwt", r.getToken());
        verify(loginAttempts).registrarExito("admin");
        verify(loginAttempts, never()).registrarFallo(anyString());
    }

    @Test
    void login_rostroDistinto_sumaFalloYLanzaCredencialesInvalidas() throws Exception {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioConRostro(0.1)));

        // distancia = sqrt(128 * 0.1^2) ~ 1.13 > 0.5
        assertThrows(BadCredentialsException.class,
                () -> service.login(loginReq("admin", descriptor(0.2))));
        verify(loginAttempts).registrarFallo("admin");
        verify(authService, never()).emitirRespuesta(any());
    }

    @Test
    void login_falloQueBloquea_lanzaDemasiadosIntentos() throws Exception {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioConRostro(0.1)));
        when(loginAttempts.registrarFallo("admin")).thenReturn(true);
        when(loginAttempts.mensajeBloqueo("admin")).thenReturn("bloqueada");

        DemasiadosIntentosException ex = assertThrows(DemasiadosIntentosException.class,
                () -> service.login(loginReq("admin", descriptor(0.9))));
        assertEquals("bloqueada", ex.getMessage());
    }

    @Test
    void login_cuentaBloqueada_rechazaAunqueElRostroCoincida() throws Exception {
        Usuario u = usuarioConRostro(0.1);
        u.setBloqueado(true);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(loginAttempts.mensajeBloqueo("admin")).thenReturn("bloqueada");

        assertThrows(DemasiadosIntentosException.class,
                () -> service.login(loginReq("admin", descriptor(0.1))));
        verify(authService, never()).emitirRespuesta(any());
        verify(loginAttempts, never()).registrarFallo(anyString());
    }

    @Test
    void login_usuarioInexistenteOSinRostro_mismoError() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        Usuario sinRostro = Usuario.builder().username("sin").passwordHash("h").rol("DOCENTE").activo(true).build();
        when(usuarioRepository.findByUsername("sin")).thenReturn(Optional.of(sinRostro));

        assertThrows(BadCredentialsException.class, () -> service.login(loginReq("nadie", descriptor(0.1))));
        assertThrows(BadCredentialsException.class, () -> service.login(loginReq("sin", descriptor(0.1))));
    }

    @Test
    void login_usuarioInactivo_esRechazado() throws Exception {
        Usuario u = usuarioConRostro(0.1);
        u.setActivo(false);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));

        assertThrows(BadCredentialsException.class,
                () -> service.login(loginReq("admin", descriptor(0.1))));
    }

    @Test
    void login_descriptorConNaN_esRechazado() throws Exception {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioConRostro(0.1)));
        double[] malo = descriptor(0.1);
        malo[0] = Double.NaN;

        assertThrows(BadCredentialsException.class, () -> service.login(loginReq("admin", malo)));
    }

    @Test
    void registrar_guardaDescriptorConPasswordCorrecta() {
        Usuario u = Usuario.builder().username("admin").passwordHash("hash").rol("ADMIN").activo(true).build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("secreta", "hash")).thenReturn(true);

        service.registrar("admin", FaceEnrollRequest.builder()
                .descriptor(descriptor(0.3)).password("secreta").consentimiento(true).build());

        assertNotNull(u.getFaceDescriptor());
        verify(usuarioRepository).save(u);
    }

    @Test
    void registrar_passwordIncorrecta_noGuarda() {
        Usuario u = Usuario.builder().username("admin").passwordHash("hash").rol("ADMIN").activo(true).build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.registrar("admin",
                FaceEnrollRequest.builder().descriptor(descriptor(0.3)).password("mala").consentimiento(true).build()));
        assertNull(u.getFaceDescriptor());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_descriptorConInfinito_lanza422() {
        Usuario u = Usuario.builder().username("admin").passwordHash("hash").rol("ADMIN").activo(true).build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);
        double[] malo = descriptor(0.3);
        malo[5] = Double.POSITIVE_INFINITY;

        assertThrows(SchedulingException.class, () -> service.registrar("admin",
                FaceEnrollRequest.builder().descriptor(malo).password("ok").consentimiento(true).build()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void eliminarYEstado_reflejanElRegistro() throws Exception {
        Usuario u = usuarioConRostro(0.1);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(u));

        assertTrue(service.tieneRostro("admin"));
        service.eliminar("admin");
        assertFalse(service.tieneRostro("admin"));
        assertNull(u.getFaceDescriptor());
    }
}
