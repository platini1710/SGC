package com.example.schedule.controller;

import com.example.schedule.dto.UsuarioBloqueadoDTO;
import com.example.schedule.model.Usuario;
import com.example.schedule.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioBloqueoController {

    private final LoginAttemptService loginAttemptService;

    @GetMapping("/bloqueados")
    public List<UsuarioBloqueadoDTO> bloqueados() {
        return loginAttemptService.bloqueados().stream().map(this::toDto).toList();
    }

    @PutMapping("/{id}/desbloquear")
    public ResponseEntity<Void> desbloquear(@PathVariable Long id) {
        loginAttemptService.desbloquear(id);
        return ResponseEntity.noContent().build();
    }

    private UsuarioBloqueadoDTO toDto(Usuario u) {
        String nombre = u.getProfesor() != null
                ? (u.getProfesor().getNombre() + " " + u.getProfesor().getApellido()).trim()
                : null;
        return UsuarioBloqueadoDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nombre(nombre)
                .rol(u.getRol())
                .permanente(u.isBloqueado())
                .bloqueadoHasta(u.isBloqueado() ? null : u.getBloqueadoHasta())
                .build();
    }
}
