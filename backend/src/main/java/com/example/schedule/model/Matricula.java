package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "matriculas", uniqueConstraints = @UniqueConstraint(columnNames = {"estudiante_id", "anio"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false)
    private Integer anio;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private EstadoMatricula estado = EstadoMatricula.ACTIVA;

    // ─── Auditoría ───────────────────────────────────────────────────────────
    @Column(name = "usuario_modificacion", length = 50)
    private String usuarioModificacion;

    @Column(name = "fecha_modificacion")
    private LocalDate fechaModificacion;

    @Column(name = "hora_modificacion")
    private LocalTime horaModificacion;
}
