package com.example.schedule.controller;

import com.example.schedule.dto.*;
import com.example.schedule.service.CursoService;
import com.example.schedule.service.EstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;
    private final EstudianteService estudianteService;

    @GetMapping
    public List<CursoResponse> findAll() {
        return cursoService.findAll();
    }

    @GetMapping("/{id}")
    public CursoDetalleResponse findById(@PathVariable Long id) {
        return cursoService.findByIdConDetalle(id);
    }

    @GetMapping("/{id}/horario")
    public GrillaHorarioResponse getHorario(@PathVariable Long id) {
        return cursoService.getHorario(id);
    }

    @GetMapping("/{id}/estudiantes")
    public Page<EstudianteResponse> getEstudiantes(@PathVariable Long id,
                                                   @RequestParam(required = false) Integer anio,
                                                   Pageable pageable) {
        return estudianteService.findByCurso(id, anio, pageable);
    }

    // Asigna o quita el profesor jefe del curso. body: { "profesorId": 3 } o { "profesorId": null }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/profesor-jefe")
    public CursoResponse asignarProfesorJefe(@PathVariable Long id,
                                             @RequestBody java.util.Map<String, Long> body) {
        return cursoService.asignarProfesorJefe(id, body.get("profesorId"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/asignaturas")
    public ResponseEntity<CursoAsignaturaResponse> asignarAsignatura(
            @PathVariable Long id,
            @Valid @RequestBody CursoAsignaturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoService.asignarAsignatura(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/asignaturas/{asignaturaId}")
    public ResponseEntity<Void> quitarAsignatura(@PathVariable Long id, @PathVariable Long asignaturaId) {
        cursoService.quitarAsignatura(id, asignaturaId);
        return ResponseEntity.noContent().build();
    }
}
