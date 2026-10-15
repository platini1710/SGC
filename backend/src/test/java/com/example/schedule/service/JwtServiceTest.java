package com.example.schedule.service;

import com.example.schedule.config.JwtProperties;
import com.example.schedule.model.Profesor;
import com.example.schedule.model.Usuario;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "c2Nob29sLXNjaGVkdWxlLWRldi1zZWNyZXQta2V5LTAxMjM0NTY3ODk=";

    private JwtService jwtService;

    private JwtProperties props(long expiration) {
        JwtProperties p = new JwtProperties();
        p.setSecret(SECRET);
        p.setExpiration(expiration);
        p.setIssuer("school-schedule-test");
        return p;
    }

    private Usuario docente() {
        Profesor profesor = Profesor.builder().id(42L).nombre("Ana").apellido("Rodríguez").build();
        return Usuario.builder()
                .id(5L)
                .username("12.345.678-9")
                .passwordHash("x")
                .rol("DOCENTE")
                .profesor(profesor)
                .activo(true)
                .build();
    }

    private Usuario admin() {
        return Usuario.builder()
                .id(1L)
                .username("admin")
                .passwordHash("x")
                .rol("ADMIN")
                .activo(true)
                .build();
    }

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(props(86_400_000L));
    }

    @Test
    void generaTokenConUsernameRolYProfesorId() {
        Usuario docente = docente();
        String token = jwtService.generateToken(docente);

        assertEquals("12.345.678-9", jwtService.extractUsername(token));
        Claims claims = jwtService.extractAllClaims(token);
        assertEquals("DOCENTE", claims.get("rol"));
        assertEquals(42, ((Number) claims.get("profesorId")).longValue());
        assertTrue(jwtService.isTokenValid(token, docente));
    }

    @Test
    void adminNoLlevaProfesorId() {
        Usuario admin = admin();
        String token = jwtService.generateToken(admin);

        Claims claims = jwtService.extractAllClaims(token);
        assertEquals("ADMIN", claims.get("rol"));
        assertNull(claims.get("profesorId"));
    }

    @Test
    void tokenExpiradoNoEsValido() {
        JwtService expiredService = new JwtService(props(-1000L));
        Usuario docente = docente();
        String token = expiredService.generateToken(docente);

        assertFalse(expiredService.isTokenValid(token, docente));
    }

    @Test
    void tokenDeOtroUsuarioNoEsValido() {
        Usuario docente = docente();
        String token = jwtService.generateToken(docente);

        assertFalse(jwtService.isTokenValid(token, admin()));
    }
}
