package com.example.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    // RF-01.4: el correo es obligatorio y debe tener formato válido.
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Por favor, ingrese un formato de correo electrónico válido (ejemplo: usuario@dominio.cl).")
    private String email;

    private String rut;

    @Builder.Default
    private List<Long> especialidadIds = new ArrayList<>();
}
