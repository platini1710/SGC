package com.example.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionConfirmRequest {
    private Integer anioOrigen;
    private Integer anioDestino;
    private List<PromocionDecisionDTO> decisiones;
}
