package com.example.schedule.controller;

import com.example.schedule.model.Colegio;
import com.example.schedule.repository.ColegioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/colegio")
@RequiredArgsConstructor
public class ColegioController {

    private final ColegioRepository colegioRepository;

    // Público: la pantalla de login lo consume antes de autenticarse.
    @GetMapping
    public ResponseEntity<Colegio> getColegio() {
        return colegioRepository.findTopByOrderByIdAsc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
