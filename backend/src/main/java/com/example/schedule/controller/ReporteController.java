package com.example.schedule.controller;

import com.example.schedule.service.CertificadoService;
import com.example.schedule.service.FichaAlumnoService;
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
    private final CertificadoService certificadoService;
    private final FichaAlumnoService fichaAlumnoService;

    @GetMapping("/notas/pdf")
    public ResponseEntity<byte[]> reporteNotasPdf(
            @RequestParam Long cursoId,
            @RequestParam Long asignaturaId,
            @RequestParam(required = false) Long trimestreId,
            @RequestParam(required = false) Integer anio) {

        byte[] pdf = reporteService.generarPdf(cursoId, asignaturaId, trimestreId, anio);

        String periodo = trimestreId != null ? "T" + trimestreId
                : (anio != null ? String.valueOf(anio) : "actual");
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

    @GetMapping("/certificado/pdf")
    public ResponseEntity<byte[]> certificadoPdf(
            @RequestParam Long cursoId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Long estudianteId) {

        byte[] pdf = certificadoService.generarCertificado(cursoId, anio, estudianteId);

        String periodo = anio != null ? String.valueOf(anio) : "actual";
        String filename = "certificado-estudios-curso" + cursoId + "-" + periodo
                + (estudianteId != null ? "-alumno" + estudianteId : "") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping("/ficha/pdf")
    public ResponseEntity<byte[]> fichaAlumnoPdf(
            @RequestParam Long estudianteId) {

        byte[] pdf = fichaAlumnoService.generarFicha(estudianteId);

        String filename = "ficha-alumno-" + estudianteId + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
