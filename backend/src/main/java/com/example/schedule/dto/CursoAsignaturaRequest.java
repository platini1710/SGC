package com.example.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoAsignaturaRequest {

    @NotNull(message = "El id de asignatura es obligatorio")
    private Long asignaturaId;

    @Min(value = 1, message = "Las horas semanales deben ser al menos 1")
    private int horasSemanales;

    private Long profesorId;
}
