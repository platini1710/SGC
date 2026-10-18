package com.example.schedule.service;

import com.example.schedule.dto.HorarioResponse;
import com.example.schedule.exception.ResourceNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integración de la regeneración de un único curso (POST /api/horarios/generar/curso/{id} →
 * HorarioService.generarHorarioCurso). Usa la semilla real (perfil test = H2 create-drop).
 * Verifica que:
 * <ul>
 *   <li>regenerar un curso 1°-4° produce sus 19 ranuras JEC sin lanzar excepción;</li>
 *   <li>NO borra el horario de los otros cursos;</li>
 *   <li>un id de curso inexistente lanza ResourceNotFoundException (404).</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class GenerarHorarioCursoITTest {

    @Autowired
    private HorarioService horarioService;
    @Autowired
    private HorarioRepository horarioRepository;
    @Autowired
    private CursoRepository cursoRepository;

    @Test
    @Transactional
    void regeneraUnCursoJecSinBorrarLosDemas() {
        // Arranca con todo el horario generado para los 8 cursos.
        horarioService.generarHorario();

        Curso curso1 = cursoRepository.findByNivel(1).orElseThrow();
        Curso otro = cursoRepository.findByNivel(2).orElseThrow();

        int filasOtroAntes = horarioRepository.findByCursoId(otro.getId()).size();
        assertTrue(filasOtroAntes > 0, "El otro curso debe tener horario antes de la prueba");

        List<HorarioResponse> respuesta = assertDoesNotThrow(
                () -> horarioService.generarHorarioCurso(curso1.getId()),
                "Regenerar un curso 1°-4° no debe lanzar SchedulingException");

        // El curso objetivo recupera sus 19 ranuras JEC (Lun-Jue 4 + Vie 3).
        List<Horario> filas = horarioRepository.findByCursoId(curso1.getId());
        assertEquals(19, filas.size(), "El curso regenerado debe tener 19 bloques JEC");
        assertEquals(19, respuesta.size(), "La respuesta debe contener las 19 filas del curso");

        Map<Integer, Long> porDia = new HashMap<>();
        for (Horario h : filas) {
            porDia.merge(h.getDiaSemana(), 1L, Long::sum);
        }
        assertEquals(4L, porDia.get(1));
        assertEquals(4L, porDia.get(2));
        assertEquals(4L, porDia.get(3));
        assertEquals(4L, porDia.get(4));
        assertEquals(3L, porDia.get(5));

        // El horario del otro curso permanece intacto (no se borró).
        int filasOtroDespues = horarioRepository.findByCursoId(otro.getId()).size();
        assertEquals(filasOtroAntes, filasOtroDespues,
                "Regenerar un curso no debe tocar el horario de los demás");
    }

    @Test
    @Transactional
    void cursoInexistenteLanza404() {
        assertThrows(ResourceNotFoundException.class,
                () -> horarioService.generarHorarioCurso(999_999L));
    }
}
