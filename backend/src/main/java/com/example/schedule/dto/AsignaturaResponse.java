package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignaturaResponse {
    private Long id;
    private String nombre;
    private String codigo;
    private String descripcion;
}
