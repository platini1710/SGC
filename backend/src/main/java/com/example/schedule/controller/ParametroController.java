package com.example.schedule.controller;

import com.example.schedule.model.Parametro;
import com.example.schedule.repository.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.Map;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
public class ParametroController {

    private final ParametroRepository parametroRepository;

    private static final int TIMEOUT_DEFAULT = 60; // segundos

    // Devuelve los parámetros: { "aproxNota": "S", "timeoutInactividad": 60, "anioEscolarActivo": 2025 }.
    @GetMapping
    public Map<String, Object> getParametros() {
        return parametroRepository.findTopByOrderByIdAsc()
                .map(p -> {
                    int anioEscolarActivo = p.getAnioEscolarActivo() == null
                            ? Year.now().getValue() : p.getAnioEscolarActivo();
                    return Map.<String, Object>of(
                            "aproxNota", p.getAproxNota() == null ? "N" : p.getAproxNota(),
                            "timeoutInactividad", p.getTimeoutInactividad() == null ? TIMEOUT_DEFAULT : p.getTimeoutInactividad(),
                            "anioEscolarActivo", anioEscolarActivo
                    );
                })
                .orElseGet(() -> Map.of(
                        "aproxNota", "N",
                        "timeoutInactividad", TIMEOUT_DEFAULT,
                        "anioEscolarActivo", Year.now().getValue()
                ));
    }

    // Permite cambiar los parámetros: body { "aproxNota": "S", "timeoutInactividad": 120, "anioEscolarActivo": 2026 }.
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
        if (body.containsKey("anioEscolarActivo") && body.get("anioEscolarActivo") != null) {
            p.setAnioEscolarActivo(Integer.valueOf(String.valueOf(body.get("anioEscolarActivo"))));
        }
        parametroRepository.save(p);

        int anioEscolarActivo = p.getAnioEscolarActivo() == null
                ? Year.now().getValue() : p.getAnioEscolarActivo();
        return Map.of(
                "aproxNota", p.getAproxNota() == null ? "N" : p.getAproxNota(),
                "timeoutInactividad", p.getTimeoutInactividad() == null ? TIMEOUT_DEFAULT : p.getTimeoutInactividad(),
                "anioEscolarActivo", anioEscolarActivo
        );
    }
}
