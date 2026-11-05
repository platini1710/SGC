package com.example.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetRequest {

    @NotBlank(message = "El correo electrónico es obligatorio.")
    private String email;

    @Pattern(regexp = "^\\d{5}$",
            message = "El código de seguridad ingresado es inválido o ha expirado. Por favor, intente nuevamente.")
    private String code;

    @NotBlank(message = "La nueva contraseña es obligatoria.")
    private String newPassword;
}
