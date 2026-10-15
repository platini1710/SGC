package com.example.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoDetalleResponse {
    private Long id;
    private String nombre;
    private Integer nivel;
    private List<CursoAsignaturaResponse> asignaturas;
}
