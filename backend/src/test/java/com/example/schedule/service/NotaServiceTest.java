package com.example.schedule.service;

import com.example.schedule.dto.NotaRequest;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Curso;
import com.example.schedule.model.EstadoTrimestre;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Matricula;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.MatriculaRepository;
import com.example.schedule.repository.NotaRepository;
import com.example.schedule.repository.TrimestreRepository;
import com.example.schedule.security.AuditoriaHelper;
import com.example.schedule.security.NotaAccessChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotaServiceTest {

    @Mock
    private NotaRepository notaRepository;
    @Mock
    private TrimestreRepository trimestreRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Mock
    private CalculoNotasService calculoNotasService;
    @Mock
    private NotaAccessChecker notaAccessChecker;
    @Mock
    private AuditoriaHelper auditoriaHelper;

    @InjectMocks
    private NotaService notaService;

    private Trimestre trimestre() {
        return Trimestre.builder()
                .id(1L).numero(1).anio(2025).estado(EstadoTrimestre.ABIERTO).build();
    }

    private Estudiante estudiante() {
        return Estudiante.builder().id(5L).nombre("Ana").apellidoPaterno("P\u00e9rez").build();
    }

    private NotaRequest request() {
        return NotaRequest.builder()
                .estudianteId(5L)
                .asignaturaId(2L)
                .trimestreId(1L)
                .posicion(1)
                .valor(new BigDecimal("6.0"))
                .build();
    }

    @Test
    void guardar_sinMatriculaEnElAnio_lanza422YNoPersistir() {
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(trimestre()));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante()));
        when(matriculaRepository.findByEstudianteIdAndAnio(5L, 2025))
                .thenReturn(Optional.empty());

        assertThrows(SchedulingException.class, () -> notaService.guardar(request()));

        verify(notaRepository, never()).save(any());
    }

    @Test
    void guardar_cursoNoDictaAsignatura_lanza403YNoPersistir() {
        Curso curso = new Curso();
        curso.setId(3L);
        Matricula matricula = Matricula.builder().id(99L).curso(curso).anio(2025).build();

        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(trimestre()));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante()));
        when(matriculaRepository.findByEstudianteIdAndAnio(5L, 2025))
                .thenReturn(Optional.of(matricula));
        doThrow(new org.springframework.security.access.AccessDeniedException("sin acceso"))
                .when(notaAccessChecker).verificar(eq(3L), eq(2L));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> notaService.guardar(request()));

        verify(notaRepository, never()).save(any());
    }
}
