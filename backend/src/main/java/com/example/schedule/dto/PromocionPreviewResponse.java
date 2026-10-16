package com.example.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionPreviewResponse {
    private Integer anioOrigen;
    private Integer anioDestino;
    private List<PromocionItemDTO> items;
    private List<String> incidencias;
}
