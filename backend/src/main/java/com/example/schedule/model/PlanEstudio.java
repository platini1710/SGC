package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plan_estudio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(length = 20)
    private String decreto;

    @Column(nullable = false)
    private Integer nivelDesde;

    @Column(nullable = false)
    private Integer nivelHasta;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "plan_estudio_asignaturas",
            joinColumns = @JoinColumn(name = "plan_estudio_id"),
            inverseJoinColumns = @JoinColumn(name = "asignatura_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"plan_estudio_id", "asignatura_id"}))
    @Builder.Default
    private List<Asignatura> asignaturas = new ArrayList<>();
}
