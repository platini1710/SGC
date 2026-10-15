package com.example.schedule.controller;

import com.example.schedule.dto.ApoderadoRequest;
import com.example.schedule.dto.ApoderadoResponse;
import com.example.schedule.service.ApoderadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apoderados")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApoderadoController {

    private final ApoderadoService apoderadoService;

    @GetMapping
    public List<ApoderadoResponse> findAll() {
        return apoderadoService.findAll();
    }

    @GetMapping("/{id}")
    public ApoderadoResponse findById(@PathVariable Long id) {
        return apoderadoService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ApoderadoResponse> create(@Valid @RequestBody ApoderadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apoderadoService.create(request));
    }

    @PutMapping("/{id}")
    public ApoderadoResponse update(@PathVariable Long id, @Valid @RequestBody ApoderadoRequest request) {
        return apoderadoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        apoderadoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
