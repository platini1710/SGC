package com.example.schedule.security;

import com.example.schedule.model.Usuario;
import com.example.schedule.repository.CursoAsignaturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotaAccessChecker {

    private final CursoAsignaturaRepository cursoAsignaturaRepository;

    public void verificar(Long cursoId, Long asignaturaId) {
        Usuario usuario = usuarioAutenticado();

        if ("ADMIN".equals(usuario.getRol())) {
            return;
        }

        if ("DOCENTE".equals(usuario.getRol())) {
            if (usuario.getProfesor() == null) {
                throw new AccessDeniedException("El docente no tiene profesor asignado");
            }
            Long profesorId = usuario.getProfesor().getId();
            if (!cursoAsignaturaRepository.existsByCursoIdAndAsignaturaIdAndProfesorId(
                    cursoId, asignaturaId, profesorId)) {
                throw new AccessDeniedException("No tiene acceso a este curso/asignatura");
            }
            return;
        }

        throw new AccessDeniedException("Acceso denegado");
    }

    /**
     * Verifica el acceso al curso completo (usado por el boletín por alumno).
     * ADMIN pasa siempre; DOCENTE pasa si tiene al menos una asignatura asignada
     * en el curso; cualquier otro caso lanza {@link AccessDeniedException} (403).
     */
    public void verificarCurso(Long cursoId) {
        Usuario usuario = usuarioAutenticado();

        if ("ADMIN".equals(usuario.getRol())) {
            return;
        }

        if ("DOCENTE".equals(usuario.getRol())) {
            if (usuario.getProfesor() == null) {
                throw new AccessDeniedException("El docente no tiene profesor asignado");
            }
            Long profesorId = usuario.getProfesor().getId();
            if (!cursoAsignaturaRepository.existsByCursoIdAndProfesorId(cursoId, profesorId)) {
                throw new AccessDeniedException("No tiene acceso a este curso");
            }
            return;
        }

        throw new AccessDeniedException("Acceso denegado");
    }

    public Usuario usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new AccessDeniedException("Acceso denegado");
        }
        return usuario;
    }
}
