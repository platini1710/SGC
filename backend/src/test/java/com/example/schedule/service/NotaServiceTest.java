package com.example.schedule.service;

import com.example.schedule.dto.GrillaNotasResponse;
import com.example.schedule.dto.NotaRequest;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Asignatura;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void getGrilla_cadaFilaLlevaLasCuatroNotasFinales() {
        Curso curso = Curso.builder().id(3L).nombre("1\u00b0 B\u00e1sico").build();
        Asignatura asignatura = Asignatura.builder().id(2L).nombre("Matem\u00e1tica").build();
        Trimestre t1 = Trimestre.builder().id(1L).numero(1).anio(2025)
                .estado(EstadoTrimestre.ABIERTO).build();
        Trimestre t2 = Trimestre.builder().id(2L).numero(2).anio(2025)
                .estado(EstadoTrimestre.ABIERTO).build();
        Trimestre t3 = Trimestre.builder().id(3L).numero(3).anio(2025)
                .estado(EstadoTrimestre.ABIERTO).build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(asignaturaRepository.findById(2L)).thenReturn(Optional.of(asignatura));
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(t1));
        when(matriculaRepository.findEstudiantesByCursoAndAnio(3L, 2025))
                .thenReturn(List.of(estudiante()));
        when(trimestreRepository.findByNumeroAndAnio(1, 2025)).thenReturn(Optional.of(t1));
        when(trimestreRepository.findByNumeroAndAnio(2, 2025)).thenReturn(Optional.of(t2));
        when(trimestreRepository.findByNumeroAndAnio(3, 2025)).thenReturn(Optional.of(t3));
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(5L, 2L, 1L))
                .thenReturn(List.of());

        when(calculoNotasService.promedioTrimestre(5L, 2L, 1L)).thenReturn(new BigDecimal("5.5"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 2L)).thenReturn(new BigDecimal("6.0"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 3L)).thenReturn(new BigDecimal("6.5"));
        when(calculoNotasService.notaFinalAnual(5L, 2L, 2025)).thenReturn(new BigDecimal("6.0"));

        GrillaNotasResponse grilla = notaService.getGrilla(3L, 2L, 1L);

        assertEquals(1, grilla.getFilas().size());
        var fila = grilla.getFilas().get(0);
        assertEquals(new BigDecimal("5.5"), fila.getNotaFinalT1());
        assertEquals(new BigDecimal("6.0"), fila.getNotaFinalT2());
        assertEquals(new BigDecimal("6.5"), fila.getNotaFinalT3());
        assertEquals(new BigDecimal("6.0"), fila.getNotaFinalAnual());
    }

    @Test
    void getGrilla_notaFinalAnualEsNullCuandoFaltaUnTrimestre() {
        Curso curso = Curso.builder().id(3L).nombre("1\u00b0 B\u00e1sico").build();
        Asignatura asignatura = Asignatura.builder().id(2L).nombre("Matem\u00e1tica").build();
        Trimestre t1 = Trimestre.builder().id(1L).numero(1).anio(2025)
                .estado(EstadoTrimestre.ABIERTO).build();
        Trimestre t2 = Trimestre.builder().id(2L).numero(2).anio(2025)
                .estado(EstadoTrimestre.ABIERTO).build();
        Trimestre t3 = Trimestre.builder().id(3L).numero(3).anio(2025)
                .estado(EstadoTrimestre.ABIERTO).build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(asignaturaRepository.findById(2L)).thenReturn(Optional.of(asignatura));
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(t1));
        when(matriculaRepository.findEstudiantesByCursoAndAnio(3L, 2025))
                .thenReturn(List.of(estudiante()));
        when(trimestreRepository.findByNumeroAndAnio(1, 2025)).thenReturn(Optional.of(t1));
        when(trimestreRepository.findByNumeroAndAnio(2, 2025)).thenReturn(Optional.of(t2));
        when(trimestreRepository.findByNumeroAndAnio(3, 2025)).thenReturn(Optional.of(t3));
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(5L, 2L, 1L))
                .thenReturn(List.of());

        when(calculoNotasService.promedioTrimestre(5L, 2L, 1L)).thenReturn(new BigDecimal("5.5"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 2L)).thenReturn(new BigDecimal("6.0"));
        // T3 sin notas.
        when(calculoNotasService.promedioTrimestre(5L, 2L, 3L)).thenReturn(null);
        // Anual null porque falta un trimestre con notas.
        when(calculoNotasService.notaFinalAnual(5L, 2L, 2025)).thenReturn(null);

        GrillaNotasResponse grilla = notaService.getGrilla(3L, 2L, 1L);

        var fila = grilla.getFilas().get(0);
        assertEquals(new BigDecimal("5.5"), fila.getNotaFinalT1());
        assertEquals(new BigDecimal("6.0"), fila.getNotaFinalT2());
        assertNull(fila.getNotaFinalT3());
        assertNull(fila.getNotaFinalAnual());
    }
}
