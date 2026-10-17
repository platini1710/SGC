package com.example.schedule.controller;

import com.example.schedule.dto.BloqueDTO;
import com.example.schedule.dto.HorarioResponse;
import com.example.schedule.dto.MoverBloqueRequest;
import com.example.schedule.service.HorarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class HorarioController {

    private final HorarioService horarioService;

    @GetMapping
    public List<HorarioResponse> getHorarioCompleto() {
        return horarioService.getHorarioCompleto();
    }

    @GetMapping("/bloques")
    public List<BloqueDTO> getBloques() {
        return horarioService.listarBloques();
    }

    @PostMapping("/generar")
    public List<HorarioResponse> generarHorario() {
        return horarioService.generarHorario();
    }

    @PutMapping("/{id}")
    public HorarioResponse moverBloque(@PathVariable Long id, @Valid @RequestBody MoverBloqueRequest request) {
        return horarioService.moverBloque(id, request);
    }

    @DeleteMapping
    public ResponseEntity<Void> borrarHorario() {
        horarioService.borrarHorario();
        return ResponseEntity.noContent().build();
    }
}
