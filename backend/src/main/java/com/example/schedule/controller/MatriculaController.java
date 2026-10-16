package com.example.schedule.controller;

import com.example.schedule.dto.PromocionConfirmRequest;
import com.example.schedule.dto.PromocionPreviewRequest;
import com.example.schedule.dto.PromocionPreviewResponse;
import com.example.schedule.dto.PromocionResultadoResponse;
import com.example.schedule.service.MatriculaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MatriculaController {

    private final MatriculaService matriculaService;

    // Previsualización de la promoción masiva: no escribe nada (POST por simetría con confirmar).
    @PostMapping("/promover/preview")
    public PromocionPreviewResponse preview(@RequestBody PromocionPreviewRequest request) {
        return matriculaService.previewPromocion(request.getAnioOrigen(), request.getAnioDestino());
    }

    // Confirmación: aplica la propuesta (posiblemente editada) de forma transaccional e idempotente.
    @PostMapping("/promover/confirmar")
    public PromocionResultadoResponse confirmar(@RequestBody PromocionConfirmRequest request) {
        return matriculaService.confirmarPromocion(request);
    }
}
