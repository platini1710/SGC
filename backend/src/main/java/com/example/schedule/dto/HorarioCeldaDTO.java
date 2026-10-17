package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioCeldaDTO {
    private Long horarioId;
    private Long asignaturaId;
    private String nombreAsignatura;
    private Long profesorId;
    private String nombreProfesor;
    private String color;
    private String nombreCurso;

    // Segundo subtramo opcional (medios bloques JEC).
    private Long asignaturaSecId;
    private String nombreAsignaturaSec;
    private Long profesorSecId;
    private String nombreProfesorSec;
    private Boolean esMedioBloque;
}
