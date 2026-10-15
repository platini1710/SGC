package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrimestreResponse {
    private Long id;
    private Integer numero;
    private Integer anio;
    private String estado;
}
