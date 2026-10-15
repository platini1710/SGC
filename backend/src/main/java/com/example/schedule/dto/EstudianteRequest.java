package com.example.schedule.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteRequest {
    private String nombre;
    private String apellido;
    private String rut;
    private LocalDate fechaNacimiento;
    private Long cursoId;
    private Long apoderadoId;
}
