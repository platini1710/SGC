package com.example.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String rut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    private Long cursoId;
    private String nombreCurso;

    private Long apoderadoId;
    private String nombreApoderado;
}
