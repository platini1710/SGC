package com.example.schedule.controller;

import com.example.schedule.dto.AsignaturaRequest;
import com.example.schedule.dto.AsignaturaResponse;
import com.example.schedule.service.AsignaturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignaturas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AsignaturaController {

    private final AsignaturaService asignaturaService;

    @GetMapping
    public List<AsignaturaResponse> findAll() {
        return asignaturaService.findAll();
    }

    @GetMapping("/{id}")
    public AsignaturaResponse findById(@PathVariable Long id) {
        return asignaturaService.findById(id);
    }

    @PostMapping
    public ResponseEntity<AsignaturaResponse> create(@Valid @RequestBody AsignaturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignaturaService.create(request));
    }

    @PutMapping("/{id}")
    public AsignaturaResponse update(@PathVariable Long id, @Valid @RequestBody AsignaturaRequest request) {
        return asignaturaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        asignaturaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
