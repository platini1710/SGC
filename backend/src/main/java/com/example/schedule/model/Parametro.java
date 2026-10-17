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

    // Año escolar activo (año "en curso" por defecto para listados, notas y reportes).
    @Column(name = "anio_escolar_activo")
    private Integer anioEscolarActivo;

    // ─── Parámetros de Jornada Escolar Completa (JEC) ────────────────────────
    // Tipo de jornada institucional por defecto ("JEC" por ahora).
    @Column(name = "tipo_jornada")
    @Builder.Default
    private String tipoJornada = "JEC";

    // Duración del recreo en minutos.
    @Column(name = "duracion_recreo")
    @Builder.Default
    private Integer duracionRecreo = 15;

    // Hora de inicio de la jornada en formato "HH:mm".
    @Column(name = "hora_inicio_jornada")
    @Builder.Default
    private String horaInicioJornada = "08:00";

    // Duración de un bloque de clase en minutos (90' = 2 horas pedagógicas).
    @Column(name = "duracion_bloque_min")
    @Builder.Default
    private Integer duracionBloqueMin = 90;

    // Duración del almuerzo / recreo largo en minutos.
    @Column(name = "duracion_almuerzo")
    @Builder.Default
    private Integer duracionAlmuerzo = 75;
}
