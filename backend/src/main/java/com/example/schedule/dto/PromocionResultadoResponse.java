package com.example.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionResultadoResponse {
    private Integer anioOrigen;
    private Integer anioDestino;
    private int promovidos;
    private int repitentes;
    private int egresados;
    private int excluidos;
    private int actualizados;
    private List<String> incidencias;
}
