package com.example.schedule.controller;

import com.example.schedule.dto.EstudianteRequest;
import com.example.schedule.dto.EstudianteResponse;
import com.example.schedule.service.EstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EstudianteController {

    private final EstudianteService estudianteService;

    @GetMapping
    public Page<EstudianteResponse> findAll(
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Integer anio,
            Pageable pageable) {
        if (cursoId != null) {
            return estudianteService.findByCurso(cursoId, anio, pageable);
        }
        return estudianteService.findAll(pageable);
    }

    @GetMapping("/inactivos")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<EstudianteResponse> findInactivos(Pageable pageable) {
        return estudianteService.findInactivos(pageable);
    }

    @GetMapping("/{id}")
    public EstudianteResponse findById(@PathVariable Long id) {
        return estudianteService.findById(id);
    }

    @PostMapping
    public ResponseEntity<EstudianteResponse> create(@Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estudianteService.create(request));
    }

    @PutMapping("/{id}")
    public EstudianteResponse update(@PathVariable Long id, @Valid @RequestBody EstudianteRequest request) {
        return estudianteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        estudianteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public EstudianteResponse reactivar(@PathVariable Long id) {
        return estudianteService.reactivar(id);
    }
}
