package com.example.schedule.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrillaHorarioResponse {
    private Long cursoId;
    private String nombreCurso;
    private Map<Integer, Map<Integer, HorarioCeldaDTO>> grilla;
    private List<BloqueDTO> bloques;
}
