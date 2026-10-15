package com.example.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoverBloqueRequest {

    @Min(value = 1, message = "El día debe ser entre 1 y 5")
    @Max(value = 5, message = "El día debe ser entre 1 y 5")
    private int diaSemana;

    @NotNull(message = "El bloque es obligatorio")
    private Long bloqueId;

    @NotNull(message = "El profesor es obligatorio")
    private Long profesorId;
}
