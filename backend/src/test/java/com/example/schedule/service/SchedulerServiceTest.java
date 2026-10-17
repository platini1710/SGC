package com.example.schedule.service;

import com.example.schedule.model.Asignatura;
import com.example.schedule.model.BloqueHorario;
import com.example.schedule.model.Curso;
import com.example.schedule.model.CursoAsignatura;
import com.example.schedule.model.Horario;
import com.example.schedule.model.Profesor;
import com.example.schedule.repository.BloqueHorarioRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.HorarioRepository;
import com.example.schedule.security.AuditoriaHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private AuditoriaHelper auditoriaHelper;

    @InjectMocks
    private SchedulerService schedulerService;

    private BloqueHorario bloque(long id, int numero) {
        return BloqueHorario.builder().id(id).numero(numero).esRecreo(false).build();
    }

    private CursoAsignatura carga(Curso curso, Asignatura asignatura, Profesor profesor, int horas) {
        return CursoAsignatura.builder()
                .curso(curso).asignatura(asignatura).profesor(profesor).horasSemanales(horas).build();
    }

    @Test
    void generaHorarioConCargasDentroDeCapacidad() {
        Curso c1 = Curso.builder().id(1L).nombre("1° Básico").nivel(1).build();
        Curso c2 = Curso.builder().id(2L).nombre("2° Básico").nivel(2).build();
        Asignatura len = Asignatura.builder().id(10L).nombre("Lenguaje").build();
        Asignatura mat = Asignatura.builder().id(11L).nombre("Matemática").build();
        Profesor p1 = Profesor.builder().id(100L).nombre("Ana").apellido("López").build();
        Profesor p2 = Profesor.builder().id(101L).nombre("Luis").apellido("Soto").build();

        // Cargas pequeñas, muy por debajo de la capacidad de 30 bloques por curso.
        List<CursoAsignatura> cargas = List.of(
                carga(c1, len, p1, 3),
                carga(c1, mat, p2, 3),
                carga(c2, len, p1, 3),
                carga(c2, mat, p2, 3));

        List<BloqueHorario> bloques = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            bloques.add(bloque(i, i));
        }

        when(cursoAsignaturaRepository.findAll()).thenReturn(cargas);
        when(bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("REGULAR")).thenReturn(bloques);
        when(auditoriaHelper.usuarioActual()).thenReturn("tester");
        when(horarioRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Horario> resultado = assertDoesNotThrow(() -> schedulerService.generarHorario());

        // 12 horas semanales en total deben quedar colocadas.
        assertEquals(12, resultado.size());
    }
}
