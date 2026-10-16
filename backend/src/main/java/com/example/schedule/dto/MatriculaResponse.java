package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatriculaResponse {
    private Long id;
    private Long estudianteId;
    private String nombreEstudiante;
    private Long cursoId;
    private String nombreCurso;
    private Integer anio;
    private String estado;
}
