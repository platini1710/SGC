package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionPreviewRequest {
    private Integer anioOrigen;
    private Integer anioDestino;
}
