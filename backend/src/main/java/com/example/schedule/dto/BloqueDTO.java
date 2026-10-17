package com.example.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueDTO {
    private Long id;
    private Integer numero;
    private String jornada;           // "JEC" | "REGULAR"

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    private Boolean esRecreo;
    private String claveRender;       // = jornada + "-" + numero (clave única usada por la grilla)
}
