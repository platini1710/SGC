package com.example.schedule.controller;

import com.example.schedule.dto.HorarioResponse;
import com.example.schedule.dto.ProfesorRequest;
import com.example.schedule.dto.ProfesorResponse;
import com.example.schedule.service.ProfesorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProfesorController {

    private final ProfesorService profesorService;

    @GetMapping
    public List<ProfesorResponse> findAll() {
        return profesorService.findAll();
    }

    @GetMapping("/{id}")
    public ProfesorResponse findById(@PathVariable Long id) {
        return profesorService.findById(id);
    }

    @GetMapping("/{id}/horario")
    public List<HorarioResponse> getHorario(@PathVariable Long id) {
        return profesorService.getHorario(id);
    }

    @PostMapping
    public ResponseEntity<ProfesorResponse> create(@Valid @RequestBody ProfesorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profesorService.create(request));
    }

    @PutMapping("/{id}")
    public ProfesorResponse update(@PathVariable Long id, @Valid @RequestBody ProfesorRequest request) {
        return profesorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        profesorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
