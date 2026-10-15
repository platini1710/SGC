package com.example.schedule.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaRequest {

    @NotNull(message = "El estudiante es obligatorio")
    private Long estudianteId;

    @NotNull(message = "La asignatura es obligatoria")
    private Long asignaturaId;

    @NotNull(message = "El trimestre es obligatorio")
    private Long trimestreId;

    @NotNull(message = "La posición es obligatoria")
    @Min(value = 1, message = "La posición debe ser al menos 1")
    @Max(value = 9, message = "La posición debe ser como máximo 9")
    private Integer posicion;

    @NotNull(message = "El valor es obligatorio")
    @DecimalMin(value = "1.0", message = "El valor mínimo es 1.0")
    @DecimalMax(value = "7.0", message = "El valor máximo es 7.0")
    private BigDecimal valor;
}
