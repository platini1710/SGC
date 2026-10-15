package com.example.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioResponse {
    private Long id;
    private Long cursoId;
    private String nombreCurso;
    private Long asignaturaId;
    private String nombreAsignatura;
    private Long profesorId;
    private String nombreProfesor;
    private Integer diaSemana;
    private Long bloqueId;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    private Boolean editadoManualmente;
}
