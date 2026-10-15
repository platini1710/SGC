package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trimestres", uniqueConstraints = @UniqueConstraint(columnNames = {"numero", "anio"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trimestre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private Integer anio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoTrimestre estado = EstadoTrimestre.ABIERTO;
}
