package com.example.schedule.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaResponse {
    private Long id;
    private Integer posicion;
    private BigDecimal valor;
}
