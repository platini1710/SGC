package com.example.schedule.service;

import com.example.schedule.model.Curso;
import com.example.schedule.model.Horario;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.HorarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression guard para el 422 de runtime: con la semilla real (perfil test = H2 con
 * create-drop, misma siembra que producción), la generación JEC de 1°-4° debe COMPLETAR sin
 * lanzar la SchedulingException de "máximo de pasos" y dejar 19 bloques/semana por curso
 * (Lun-Jue = 4, Vie = 3). Ejercita el mismo método de servicio que hay detrás de
 * POST /api/horarios/generar (HorarioService.generarHorario), que corre la pasada JEC y la
 * REGULAR en una sola transacción con el set de ocupación de profesores compartido.
 */
@SpringBootTest
@ActiveProfiles("test")
class JecGeneracionITTest {

    @Autowired
    private HorarioService horarioService;
    @Autowired
    private HorarioRepository horarioRepository;
    @Autowired
    private CursoRepository cursoRepository;

    @Test
    @Transactional
    void generarHorarioCompletaYLlenaLasGrillasJecDe1a4() {
        // No debe explotar el backtracking JEC (el 422 original) ni fallar la pasada REGULAR.
        assertDoesNotThrow(() -> horarioService.generarHorario(),
                "La generación de horario no debe lanzar SchedulingException con la semilla distribuida");

        for (Curso curso : cursoRepository.findAllByOrderByNivelAsc()) {
            if (curso.getNivel() == null || curso.getNivel() > 4) continue;

            List<Horario> filas = horarioRepository.findByCursoId(curso.getId());

            // 19 ranuras de bloque por semana (Lun-Jue 4 + Vie 3).
            assertEquals(19, filas.size(),
                    "El curso " + curso.getNombre() + " debe tener 19 bloques JEC por semana");

            Map<Integer, Long> porDia = new HashMap<>();
            for (Horario h : filas) {
                porDia.merge(h.getDiaSemana(), 1L, Long::sum);
            }
            assertEquals(4L, porDia.get(1), curso.getNombre() + " — Lunes debe tener 4 bloques");
            assertEquals(4L, porDia.get(2), curso.getNombre() + " — Martes debe tener 4 bloques");
            assertEquals(4L, porDia.get(3), curso.getNombre() + " — Miércoles debe tener 4 bloques");
            assertEquals(4L, porDia.get(4), curso.getNombre() + " — Jueves debe tener 4 bloques");
            assertEquals(3L, porDia.get(5), curso.getNombre() + " — Viernes debe tener 3 bloques");

            // Exactamente 2 medios bloques compartidos por semana ({CN,TEC} y {HIS,ORI}).
            long mediosCompartidos = filas.stream()
                    .filter(h -> Boolean.TRUE.equals(h.getEsMedioBloque()))
                    .count();
            assertEquals(2L, mediosCompartidos,
                    curso.getNombre() + " — deben existir 2 medios bloques compartidos");
        }
    }
}
