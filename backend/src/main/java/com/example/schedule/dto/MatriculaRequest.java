package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatriculaRequest {
    private Long estudianteId;
    private Long cursoId;
    private Integer anio;
    // Enum EstadoMatricula tipado como String: se parsea en el servicio (ver diseño).
    private String estado;
}
