package com.example.schedule.controller;

import com.example.schedule.model.Parametro;
import com.example.schedule.repository.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
public class ParametroController {

    private final ParametroRepository parametroRepository;

    private static final int TIMEOUT_DEFAULT = 60; // segundos

    // Devuelve los parámetros: { "aproxNota": "S", "timeoutInactividad": 60 }.
    @GetMapping
    public Map<String, Object> getParametros() {
        return parametroRepository.findTopByOrderByIdAsc()
                .map(p -> Map.<String, Object>of(
                        "aproxNota", p.getAproxNota() == null ? "N" : p.getAproxNota(),
                        "timeoutInactividad", p.getTimeoutInactividad() == null ? TIMEOUT_DEFAULT : p.getTimeoutInactividad()
                ))
                .orElse(Map.of("aproxNota", "N", "timeoutInactividad", TIMEOUT_DEFAULT));
    }

    // Permite cambiar los parámetros: body { "aproxNota": "S", "timeoutInactividad": 120 }.
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Map<String, Object> setParametros(@RequestBody Map<String, Object> body) {
        Parametro p = parametroRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> Parametro.builder().build());

        if (body.containsKey("aproxNota") && body.get("aproxNota") != null) {
            p.setAproxNota(String.valueOf(body.get("aproxNota")));
        }
        if (body.containsKey("timeoutInactividad") && body.get("timeoutInactividad") != null) {
            p.setTimeoutInactividad(Integer.valueOf(String.valueOf(body.get("timeoutInactividad"))));
        }
        parametroRepository.save(p);

        return Map.of(
                "aproxNota", p.getAproxNota() == null ? "N" : p.getAproxNota(),
                "timeoutInactividad", p.getTimeoutInactividad() == null ? TIMEOUT_DEFAULT : p.getTimeoutInactividad()
        );
    }
}
