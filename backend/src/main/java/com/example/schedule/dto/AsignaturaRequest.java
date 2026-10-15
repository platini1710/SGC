package com.example.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignaturaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String codigo;

    private String descripcion;
}
