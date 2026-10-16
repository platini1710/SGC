package com.example.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanEstudioDetalleResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private String decreto;
    private Integer nivelDesde;
    private Integer nivelHasta;
    private Boolean activo;
    private int cantidadAsignaturas;
    private List<AsignaturaResponse> asignaturas;
}
