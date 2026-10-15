package com.example.schedule.controller;

import com.example.schedule.dto.AsignacionDocenteDTO;
import com.example.schedule.dto.ConsolidadoAnualResponse;
import com.example.schedule.dto.GrillaNotasResponse;
import com.example.schedule.dto.NotaRequest;
import com.example.schedule.dto.NotaResponse;
import com.example.schedule.service.NotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
public class NotaController {

    private final NotaService notaService;

    @GetMapping("/mis-asignaciones")
    public List<AsignacionDocenteDTO> misAsignaciones() {
        return notaService.misAsignaciones();
    }

    @GetMapping("/grilla")
    public GrillaNotasResponse getGrilla(@RequestParam Long cursoId,
                                         @RequestParam Long asignaturaId,
                                         @RequestParam Long trimestreId) {
        return notaService.getGrilla(cursoId, asignaturaId, trimestreId);
    }

    @PostMapping
    public NotaResponse guardar(@Valid @RequestBody NotaRequest request) {
        return notaService.guardar(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        notaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consolidado")
    public ConsolidadoAnualResponse getConsolidado(@RequestParam Long cursoId,
                                                   @RequestParam Long asignaturaId,
                                                   @RequestParam int anio) {
        return notaService.getConsolidado(cursoId, asignaturaId, anio);
    }
}
