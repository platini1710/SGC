package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "estudiantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellido;

    @Column(unique = true, length = 20)
    private String rut;

    private LocalDate fechaNacimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apoderado_id")
    private Apoderado apoderado;

    // ─── Identificación ──────────────────────────────────────────────────────
    @Column(length = 100)
    private String lugarNacimiento;

    @Column(length = 60)
    private String nacionalidad;

    @Column(length = 20)
    private String sexo;

    @Column(length = 100)
    private String comuna;

    @Column(length = 30)
    private String telefono;

    @Column(length = 200)
    private String domicilio;

    // Foto como data URI Base64 (ej. "data:image/png;base64,....").
    // TEXT para permitir imágenes grandes. NO usar @Lob: en PostgreSQL
    // @Lob sobre String crea un Large Object (oid) que no puede leerse en
    // modo auto-commit (fuera de transacción). Mismo patrón que Colegio.logo.
    @Column(columnDefinition = "TEXT")
    private String foto;

    // ─── Antecedentes familiares ───────────────────────────────────────────
    @Column(length = 150)
    private String padreNombre;

    @Column(length = 20)
    private String padreRut;

    @Column(length = 100)
    private String padreOcupacion;

    @Column(length = 150)
    private String madreNombre;

    @Column(length = 20)
    private String madreRut;

    @Column(length = 100)
    private String madreOcupacion;

    @Column(length = 60)
    private String apoderadoRelacion;

    // ─── Información médica ──────────────────────────────────────────────────
    @Column(length = 60)
    private String prevision;

    @Column(length = 200)
    private String alergias;

    @Column(length = 200)
    private String condicionMedica;

    @Column(length = 150)
    private String contactoEmergencia;

    @Column(length = 30)
    private String fonoEmergencia;
}
