package com.example.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteResponse {
    private Long id;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    // Derivado (apellidoPaterno [+ ' ' + apellidoMaterno]) para compatibilidad.
    private String apellido;
    private String rut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    private Long cursoId;
    private String nombreCurso;

    private Long apoderadoId;
    private String nombreApoderado;

    // Identificación
    private String lugarNacimiento;
    private String nacionalidad;
    private String sexo;
    private String comuna;
    private String telefono;
    private String domicilio;
    private String foto;

    // Antecedentes familiares
    private String padreNombre;
    private String padreRut;
    private String padreOcupacion;
    private String madreNombre;
    private String madreRut;
    private String madreOcupacion;
    private String apoderadoRelacion;

    // Información médica
    private String prevision;
    private String alergias;
    private String condicionMedica;
    private String contactoEmergencia;
    private String fonoEmergencia;

    // Estado y auditoría
    private Boolean activo;
    private String usuarioModificacion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaModificacion;

    private LocalTime horaModificacion;

    private String inactivadoPor;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInactivacion;

    private LocalTime horaInactivacion;
}
