package com.example.schedule.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioBloqueadoDTO {
    private Long id;
    private String username;
    // Nombre del profesor asociado (null para usuarios sin profesor, ej. admin).
    private String nombre;
    private String rol;
    // true: bloqueo permanente; false: temporal (ver bloqueadoHasta).
    private boolean permanente;
    private LocalDateTime bloqueadoHasta;
}
