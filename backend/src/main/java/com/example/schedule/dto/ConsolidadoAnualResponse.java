package com.example.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidadoAnualResponse {
    private Long cursoId;
    private Long asignaturaId;
    private Integer anio;
    private List<FilaConsolidadoDTO> filas;
}
