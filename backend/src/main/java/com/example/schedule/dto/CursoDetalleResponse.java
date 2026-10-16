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
    private Long profesorJefeId;
    private String nombreProfesorJefe;
    private List<CursoAsignaturaResponse> asignaturas;
}
