package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

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

    @Column(name = "apellido_paterno", length = 100)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 100)
    private String apellidoMaterno;

    @Column(unique = true, length = 20)
    private String rut;

    private LocalDate fechaNacimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apoderado_id")
    private Apoderado apoderado;

    // Soft delete: "Eliminar" marca inactivo en vez de borrar. Solo se listan los activos.
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

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

    // ─── Auditoría ───────────────────────────────────────────────────────────
    // Quién/cuándo modificó por última vez (create/update/reactivar).
    @Column(name = "usuario_modificacion", length = 50)
    private String usuarioModificacion;

    @Column(name = "fecha_modificacion")
    private LocalDate fechaModificacion;

    @Column(name = "hora_modificacion")
    private LocalTime horaModificacion;

    // Quién/cuándo inactivó (soft delete). Se limpian a null al reactivar.
    @Column(name = "inactivado_por", length = 50)
    private String inactivadoPor;

    @Column(name = "fecha_inactivacion")
    private LocalDate fechaInactivacion;

    @Column(name = "hora_inactivacion")
    private LocalTime horaInactivacion;
}
