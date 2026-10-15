package com.example.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApoderadoResponse {
    private Long id;
    private String rut;
    private String nombre;
    private String apellidoPaterno;
    private String region;
    private String comuna;
    private String direccion;
    private String telefonoCelular;
    private String correo;
    private int cantidadEstudiantes;
}
