package com.example.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceLoginRequest {

    @NotBlank(message = "El usuario es obligatorio")
    private String username;

    @NotNull(message = "El descriptor facial es obligatorio")
    @Size(min = 128, max = 128, message = "El descriptor facial debe tener 128 valores")
    private double[] descriptor;
}
