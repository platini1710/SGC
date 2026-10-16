package com.example.schedule.controller;

import com.example.schedule.dto.PlanEstudioDetalleResponse;
import com.example.schedule.dto.PlanEstudioRequest;
import com.example.schedule.dto.PlanEstudioResponse;
import com.example.schedule.service.PlanEstudioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planes-estudio")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PlanEstudioController {

    private final PlanEstudioService planEstudioService;

    @GetMapping
    public List<PlanEstudioResponse> findAll() {
        return planEstudioService.findAll();
    }

    @GetMapping("/{id}")
    public PlanEstudioDetalleResponse findById(@PathVariable Long id) {
        return planEstudioService.findById(id);
    }

    @PostMapping
    public ResponseEntity<PlanEstudioResponse> create(@Valid @RequestBody PlanEstudioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planEstudioService.create(request));
    }

    @PutMapping("/{id}")
    public PlanEstudioResponse update(@PathVariable Long id, @Valid @RequestBody PlanEstudioRequest request) {
        return planEstudioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planEstudioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // body: { "asignaturaId": n }
    @PostMapping("/{id}/asignaturas")
    public PlanEstudioDetalleResponse addAsignatura(@PathVariable Long id,
                                                    @RequestBody Map<String, Long> body) {
        return planEstudioService.addAsignatura(id, body.get("asignaturaId"));
    }

    @DeleteMapping("/{id}/asignaturas/{asignaturaId}")
    public ResponseEntity<Void> removeAsignatura(@PathVariable Long id, @PathVariable Long asignaturaId) {
        planEstudioService.removeAsignatura(id, asignaturaId);
        return ResponseEntity.noContent().build();
    }
}
