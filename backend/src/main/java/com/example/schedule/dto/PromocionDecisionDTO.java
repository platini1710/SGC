package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionDecisionDTO {
    private Long estudianteId;
    // Enum AccionPromocion tipado como String: se parsea en el servicio (ver diseño).
    private String accion;
    private Long cursoDestinoId;
}
