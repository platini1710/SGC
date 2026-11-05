package com.example.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordCodeRequest {

    @NotBlank(message = "El correo electrónico es obligatorio.")
    private String email;
}
