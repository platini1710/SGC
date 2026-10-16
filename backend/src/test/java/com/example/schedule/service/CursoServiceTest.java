package com.example.schedule.service;

import com.example.schedule.dto.AsignaturaResponse;
import com.example.schedule.dto.CursoAsignaturaRequest;
import com.example.schedule.dto.CursoResponse;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Asignatura;
import com.example.schedule.model.Curso;
import com.example.schedule.model.PlanEstudio;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.BloqueHorarioRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.HorarioRepository;
import com.example.schedule.repository.PlanEstudioRepository;
import com.example.schedule.repository.ProfesorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private PlanEstudioRepository planEstudioRepository;

    @InjectMocks
    private CursoService cursoService;

    private Asignatura asignatura(Long id, String nombre) {
        return Asignatura.builder().id(id).nombre(nombre).codigo("C" + id).descripcion("d").build();
    }

    private PlanEstudio planCon(Long id, Asignatura... asigs) {
        return PlanEstudio.builder()
                .id(id).nombre("Plan " + id).nivelDesde(1).nivelHasta(4).activo(true)
                .asignaturas(new ArrayList<>(List.of(asigs)))
                .build();
    }

    private CursoAsignaturaRequest request(Long asignaturaId) {
        return CursoAsignaturaRequest.builder()
                .asignaturaId(asignaturaId)
                .horasSemanales(4)
                .build();
    }

    @Test
    void asignarAsignatura_fueraDelPlan_lanza422() {
        Asignatura mat = asignatura(7L, "Matemática");
        PlanEstudio plan = planCon(1L); // vacío, no contiene la asignatura
        Curso curso = Curso.builder().id(3L).nombre("1° Básico").nivel(1).planEstudio(plan).build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(asignaturaRepository.findById(7L)).thenReturn(Optional.of(mat));
        when(planEstudioRepository.findByIdFetchAsignaturas(1L)).thenReturn(Optional.of(plan));

        assertThrows(SchedulingException.class, () -> cursoService.asignarAsignatura(3L, request(7L)));
        verify(cursoAsignaturaRepository, never()).save(any());
    }

    @Test
    void asignarAsignatura_dentroDelPlan_persiste() {
        Asignatura mat = asignatura(7L, "Matemática");
        PlanEstudio plan = planCon(1L, mat);
        Curso curso = Curso.builder().id(3L).nombre("1° Básico").nivel(1).planEstudio(plan).build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(asignaturaRepository.findById(7L)).thenReturn(Optional.of(mat));
        when(planEstudioRepository.findByIdFetchAsignaturas(1L)).thenReturn(Optional.of(plan));
        when(cursoAsignaturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cursoService.asignarAsignatura(3L, request(7L));

        verify(cursoAsignaturaRepository).save(any());
    }

    @Test
    void asignarAsignatura_cursoSinPlan_lanza422() {
        Asignatura mat = asignatura(7L, "Matemática");
        Curso curso = Curso.builder().id(3L).nombre("1° Básico").nivel(1).build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(asignaturaRepository.findById(7L)).thenReturn(Optional.of(mat));

        assertThrows(SchedulingException.class, () -> cursoService.asignarAsignatura(3L, request(7L)));
        verify(cursoAsignaturaRepository, never()).save(any());
    }

    @Test
    void asignaturasDisponibles_resuelveViaPlan() {
        Asignatura mat = asignatura(7L, "Matemática");
        Asignatura len = asignatura(8L, "Lenguaje");
        PlanEstudio plan = planCon(1L, mat, len);
        Curso curso = Curso.builder().id(3L).nombre("1° Básico").nivel(1).planEstudio(plan).build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(planEstudioRepository.findByIdFetchAsignaturas(1L)).thenReturn(Optional.of(plan));

        List<AsignaturaResponse> disponibles = cursoService.asignaturasDisponibles(3L);

        assertEquals(2, disponibles.size());
    }

    @Test
    void asignaturasDisponibles_cursoSinPlan_vacio() {
        Curso curso = Curso.builder().id(3L).nombre("1° Básico").nivel(1).build();
        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));

        assertEquals(0, cursoService.asignaturasDisponibles(3L).size());
    }

    @Test
    void asignarPlanEstudio_seteaYlimpia() {
        Curso curso = Curso.builder().id(3L).nombre("1° Básico").nivel(1).build();
        PlanEstudio plan = planCon(5L);

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(planEstudioRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(cursoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CursoResponse set = cursoService.asignarPlanEstudio(3L, 5L);
        assertEquals(5L, set.getPlanEstudioId());

        CursoResponse clear = cursoService.asignarPlanEstudio(3L, null);
        assertNull(clear.getPlanEstudioId());
    }
}
