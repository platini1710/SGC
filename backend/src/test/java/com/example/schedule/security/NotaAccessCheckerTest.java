package com.example.schedule.security;

import com.example.schedule.model.Profesor;
import com.example.schedule.model.Usuario;
import com.example.schedule.repository.CursoAsignaturaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotaAccessCheckerTest {

    private final CursoAsignaturaRepository repo = mock(CursoAsignaturaRepository.class);
    private final NotaAccessChecker checker = new NotaAccessChecker(repo);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    @Test
    void adminSiempreTieneAcceso() {
        Usuario admin = Usuario.builder().username("admin").passwordHash("x").rol("ADMIN").activo(true).build();
        autenticar(admin);

        assertDoesNotThrow(() -> checker.verificar(1L, 2L));
        verifyNoInteractions(repo);
    }

    @Test
    void docenteConAsignacionTieneAcceso() {
        Profesor profesor = Profesor.builder().id(7L).build();
        Usuario docente = Usuario.builder().username("rut").passwordHash("x")
                .rol("DOCENTE").profesor(profesor).activo(true).build();
        autenticar(docente);
        when(repo.existsByCursoIdAndAsignaturaIdAndProfesorId(1L, 2L, 7L)).thenReturn(true);

        assertDoesNotThrow(() -> checker.verificar(1L, 2L));
    }

    @Test
    void docenteSinAsignacionEsRechazado() {
        Profesor profesor = Profesor.builder().id(7L).build();
        Usuario docente = Usuario.builder().username("rut").passwordHash("x")
                .rol("DOCENTE").profesor(profesor).activo(true).build();
        autenticar(docente);
        when(repo.existsByCursoIdAndAsignaturaIdAndProfesorId(1L, 2L, 7L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> checker.verificar(1L, 2L));
    }

    @Test
    void docenteSinProfesorEsRechazadoFailClosed() {
        Usuario docente = Usuario.builder().username("rut").passwordHash("x")
                .rol("DOCENTE").profesor(null).activo(true).build();
        autenticar(docente);

        assertThrows(AccessDeniedException.class, () -> checker.verificar(1L, 2L));
        verifyNoInteractions(repo);
    }

    @Test
    void sinAutenticacionEsRechazado() {
        assertThrows(AccessDeniedException.class, () -> checker.verificar(1L, 2L));
    }
}
