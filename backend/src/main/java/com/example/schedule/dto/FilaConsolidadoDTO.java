package com.example.schedule.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaConsolidadoDTO {
    private Long estudianteId;
    private String nombreEstudiante;
    private BigDecimal promedioT1;
    private BigDecimal promedioT2;
    private BigDecimal promedioT3;
    private BigDecimal notaFinal;
}
