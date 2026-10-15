package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoAsignaturaResponse {
    private Long asignaturaId;
    private String nombreAsignatura;
    private Integer horasSemanales;
    private Long profesorId;
    private String nombreProfesor;
}
