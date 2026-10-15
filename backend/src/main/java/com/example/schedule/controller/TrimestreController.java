package com.example.schedule.controller;

import com.example.schedule.dto.TrimestreResponse;
import com.example.schedule.service.TrimestreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trimestres")
@RequiredArgsConstructor
public class TrimestreController {

    private final TrimestreService trimestreService;

    @GetMapping
    public List<TrimestreResponse> findByAnio(@RequestParam(required = false) Integer anio) {
        return trimestreService.findByAnio(anio);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/abrir")
    public TrimestreResponse abrir(@PathVariable Long id) {
        return trimestreService.abrir(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/cerrar")
    public TrimestreResponse cerrar(@PathVariable Long id) {
        return trimestreService.cerrar(id);
    }
}
