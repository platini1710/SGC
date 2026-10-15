package com.example.schedule.controller;

import com.example.schedule.dto.EspecialidadRequest;
import com.example.schedule.model.Especialidad;
import com.example.schedule.service.EspecialidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public List<Especialidad> findAll() {
        return especialidadService.findAll();
    }

    @GetMapping("/{id}")
    public Especialidad findById(@PathVariable Long id) {
        return especialidadService.findById(id);
    }

    @PostMapping
    public Especialidad create(@Valid @RequestBody EspecialidadRequest request) {
        return especialidadService.create(request);
    }

    @PutMapping("/{id}")
    public Especialidad update(@PathVariable Long id, @Valid @RequestBody EspecialidadRequest request) {
        return especialidadService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        especialidadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
