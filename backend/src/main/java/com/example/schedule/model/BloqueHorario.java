package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "bloques_horario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Integer numero;

    @Column
    private LocalTime horaInicio;

    @Column
    private LocalTime horaFin;

    @Column(nullable = false)
    @Builder.Default
    private Boolean esRecreo = false;
}
