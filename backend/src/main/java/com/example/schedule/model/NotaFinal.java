package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "nota_final", uniqueConstraints = @UniqueConstraint(
        columnNames = {"estudiante_id", "asignatura_id", "anio"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @Column(nullable = false)
    private int anio;

    @Column(name = "nf_t1", precision = 3, scale = 1)
    private BigDecimal nfT1;

    @Column(name = "nf_t2", precision = 3, scale = 1)
    private BigDecimal nfT2;

    @Column(name = "nf_t3", precision = 3, scale = 1)
    private BigDecimal nfT3;

    @Column(name = "nf_anual", precision = 3, scale = 1)
    private BigDecimal nfAnual;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
