package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanEstudioResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private String decreto;
    private Integer nivelDesde;
    private Integer nivelHasta;
    private Boolean activo;
    private int cantidadAsignaturas;
}
