package com.example.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanEstudioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    private String decreto;

    @NotNull(message = "El nivel inicial es obligatorio")
    @Min(value = 1, message = "El nivel inicial debe ser al menos 1")
    private Integer nivelDesde;

    @NotNull(message = "El nivel final es obligatorio")
    @Min(value = 1, message = "El nivel final debe ser al menos 1")
    private Integer nivelHasta;

    private Boolean activo;
}
