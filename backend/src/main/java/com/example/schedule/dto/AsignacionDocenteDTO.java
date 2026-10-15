package com.example.schedule.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AsignacionDocenteDTO {
    private Long cursoId;
    private String nombreCurso;
    private Long asignaturaId;
    private String nombreAsignatura;
}
