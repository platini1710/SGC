package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoResponse {
    private Long id;
    private String nombre;
    private Integer nivel;
    private String profesorJefe;
}
