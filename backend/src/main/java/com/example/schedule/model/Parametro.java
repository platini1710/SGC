package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parametro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 'S' = se aproxima la nota al decimal superior; cualquier otro valor = no se aproxima.
    @Column(name = "aprox_nota", length = 1)
    private String aproxNota;

    // Segundos de inactividad antes del cierre automático de sesión en el frontend.
    @Column(name = "timeout_inactividad")
    private Integer timeoutInactividad;
}
