package com.example.schedule.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaNotaDTO {
    private Long estudianteId;
    private String nombreEstudiante;
    private List<NotaResponse> notas;
    private BigDecimal promedio;
    private BigDecimal notaFinalT1;
    private BigDecimal notaFinalT2;
    private BigDecimal notaFinalT3;
    private BigDecimal notaFinalAnual;
}
