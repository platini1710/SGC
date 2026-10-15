package com.example.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String rut;
    private Boolean activo;
    private List<String> especialidades;
}
