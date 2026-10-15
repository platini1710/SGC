package com.example.schedule.controller;

import com.example.schedule.service.InformeAlumnoService;
import com.example.schedule.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
public class ReporteController {

    private final ReporteService reporteService;
    private final InformeAlumnoService informeAlumnoService;

    @GetMapping("/notas/pdf")
    public ResponseEntity<byte[]> reporteNotasPdf(
            @RequestParam Long cursoId,
            @RequestParam Long asignaturaId,
            @RequestParam(required = false) Long trimestreId) {

        byte[] pdf = reporteService.generarPdf(cursoId, asignaturaId, trimestreId);

        String periodo = trimestreId != null ? "T" + trimestreId : "anual";
        String filename = "reporte-notas-" + cursoId + "-" + asignaturaId + "-" + periodo + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping("/alumno/pdf")
    public ResponseEntity<byte[]> informeAlumnoPdf(
            @RequestParam Long cursoId,
            @RequestParam Long trimestreId,
            @RequestParam(required = false) Long estudianteId) {

        byte[] pdf = informeAlumnoService.generarBoletin(cursoId, trimestreId, estudianteId);

        String filename = "informe-notas-curso" + cursoId + "-T" + trimestreId
                + (estudianteId != null ? "-alumno" + estudianteId : "") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
