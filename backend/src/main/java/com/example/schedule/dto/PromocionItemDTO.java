package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionItemDTO {
    private Long estudianteId;
    private String nombreEstudiante;
    private Long cursoOrigenId;
    private String cursoOrigenNombre;
    private Integer nivelOrigen;
    // PROMOVER | EGRESA | SIN_CURSO_DESTINO
    private String accionPropuesta;
    private Long cursoDestinoId;
    private String cursoDestinoNombre;
    private boolean yaExisteEnDestino;
}
