package com.example.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrillaNotasResponse {
    private Long cursoId;
    private String nombreCurso;
    private Long asignaturaId;
    private String nombreAsignatura;
    private Long trimestreId;
    private Integer numeroTrimestre;
    private String estadoTrimestre;
    private List<FilaNotaDTO> filas;
}
