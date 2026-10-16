package com.example.schedule.service;

import com.example.schedule.dto.PlanEstudioDetalleResponse;
import com.example.schedule.dto.PlanEstudioRequest;
import com.example.schedule.dto.PlanEstudioResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Asignatura;
import com.example.schedule.model.PlanEstudio;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.PlanEstudioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanEstudioServiceTest {

    @Mock
    private PlanEstudioRepository planEstudioRepository;
    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private CursoRepository cursoRepository;

    @InjectMocks
    private PlanEstudioService planEstudioService;

    private Asignatura asignatura(Long id, String nombre) {
        return Asignatura.builder().id(id).nombre(nombre).codigo("C" + id).descripcion("d").build();
    }

    private PlanEstudio plan(Long id, Asignatura... asigs) {
        return PlanEstudio.builder()
                .id(id)
                .nombre("Plan " + id)
                .nivelDesde(1)
                .nivelHasta(4)
                .activo(true)
                .asignaturas(new ArrayList<>(List.of(asigs)))
                .build();
    }

    private PlanEstudioRequest request(int desde, int hasta) {
        return PlanEstudioRequest.builder()
                .nombre("Plan X")
                .nivelDesde(desde)
                .nivelHasta(hasta)
                .build();
    }

    @Test
    void create_ok() {
        when(planEstudioRepository.save(any())).thenAnswer(inv -> {
            PlanEstudio p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        PlanEstudioResponse resp = planEstudioService.create(request(1, 4));

        assertEquals(10L, resp.getId());
        assertEquals(Boolean.TRUE, resp.getActivo());
        verify(planEstudioRepository).save(any());
    }

    @Test
    void create_nivelDesdeMayorQueHasta_lanza422() {
        assertThrows(SchedulingException.class, () -> planEstudioService.create(request(5, 2)));
        verify(planEstudioRepository, never()).save(any());
    }

    @Test
    void addAsignatura_agrega() {
        PlanEstudio p = plan(1L);
        Asignatura a = asignatura(7L, "Matemática");
        when(planEstudioRepository.findByIdFetchAsignaturas(1L)).thenReturn(Optional.of(p));
        when(asignaturaRepository.findById(7L)).thenReturn(Optional.of(a));
        when(planEstudioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanEstudioDetalleResponse resp = planEstudioService.addAsignatura(1L, 7L);

        assertEquals(1, resp.getAsignaturas().size());
        assertEquals(7L, resp.getAsignaturas().get(0).getId());
        verify(planEstudioRepository).save(any());
    }

    @Test
    void addAsignatura_idempotente() {
        Asignatura a = asignatura(7L, "Matemática");
        PlanEstudio p = plan(1L, a);
        when(planEstudioRepository.findByIdFetchAsignaturas(1L)).thenReturn(Optional.of(p));
        when(asignaturaRepository.findById(7L)).thenReturn(Optional.of(a));

        PlanEstudioDetalleResponse resp = planEstudioService.addAsignatura(1L, 7L);

        assertEquals(1, resp.getAsignaturas().size());
        verify(planEstudioRepository, never()).save(any());
    }

    @Test
    void removeAsignatura_quita() {
        Asignatura a = asignatura(7L, "Matemática");
        PlanEstudio p = plan(1L, a);
        when(planEstudioRepository.findByIdFetchAsignaturas(1L)).thenReturn(Optional.of(p));
        when(planEstudioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanEstudioDetalleResponse resp = planEstudioService.removeAsignatura(1L, 7L);

        assertTrue(resp.getAsignaturas().isEmpty());
        verify(planEstudioRepository).save(any());
    }

    @Test
    void removeAsignatura_inexistente_lanza404() {
        PlanEstudio p = plan(1L);
        when(planEstudioRepository.findByIdFetchAsignaturas(1L)).thenReturn(Optional.of(p));

        assertThrows(ResourceNotFoundException.class, () -> planEstudioService.removeAsignatura(1L, 99L));
        verify(planEstudioRepository, never()).save(any());
    }

    @Test
    void findById_devuelveAsignaturas() {
        PlanEstudio p = plan(1L, asignatura(7L, "Matemática"), asignatura(8L, "Lenguaje"));
        when(planEstudioRepository.findByIdFetchAsignaturas(1L)).thenReturn(Optional.of(p));

        PlanEstudioDetalleResponse resp = planEstudioService.findById(1L);

        assertEquals(2, resp.getAsignaturas().size());
        assertEquals(2, resp.getCantidadAsignaturas());
    }

    @Test
    void delete_enUso_lanza422() {
        PlanEstudio p = plan(1L);
        when(planEstudioRepository.findById(1L)).thenReturn(Optional.of(p));
        when(cursoRepository.existsByPlanEstudioId(1L)).thenReturn(true);

        assertThrows(SchedulingException.class, () -> planEstudioService.delete(1L));
        verify(planEstudioRepository, never()).delete(any());
    }
}
