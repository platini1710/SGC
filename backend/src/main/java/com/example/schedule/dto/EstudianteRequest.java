package com.example.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteRequest {
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String rut;
    private LocalDate fechaNacimiento;
    private Long cursoId;

    // El apoderado es obligatorio: debe existir previamente en el mantenedor de Apoderados.
    @NotNull(message = "El apoderado es obligatorio")
    private Long apoderadoId;

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
}
