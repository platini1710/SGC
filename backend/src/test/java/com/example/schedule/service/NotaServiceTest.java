package com.example.schedule.service;

import com.example.schedule.dto.GrillaNotasResponse;
import com.example.schedule.dto.NotaRequest;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Asignatura;
import com.example.schedule.model.Curso;
import com.example.schedule.model.EstadoTrimestre;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Matricula;
import com.example.schedule.model.NotaFinal;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.MatriculaRepository;
import com.example.schedule.repository.NotaFinalRepository;
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
    private NotaFinalService notaFinalService;
    @Mock
    private NotaFinalRepository notaFinalRepository;
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
    void getGrilla_cadaFilaLeeLasCuatroNotasFinalesPersistidas() {
        Curso curso = Curso.builder().id(3L).nombre("1\u00b0 B\u00e1sico").build();
        Asignatura asignatura = Asignatura.builder().id(2L).nombre("Matem\u00e1tica").build();
        Trimestre t1 = Trimestre.builder().id(1L).numero(1).anio(2025)
                .estado(EstadoTrimestre.ABIERTO).build();

        NotaFinal notaFinal = NotaFinal.builder()
                .estudiante(estudiante())
                .asignatura(asignatura)
                .anio(2025)
                .nfT1(new BigDecimal("5.5"))
                .nfT2(new BigDecimal("6.0"))
                .nfT3(new BigDecimal("6.5"))
                .nfAnual(new BigDecimal("6.0"))
                .build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(asignaturaRepository.findById(2L)).thenReturn(Optional.of(asignatura));
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(t1));
        when(matriculaRepository.findEstudiantesByCursoAndAnio(3L, 2025))
                .thenReturn(List.of(estudiante()));
        when(notaFinalRepository.findByAsignaturaIdAndAnio(2L, 2025))
                .thenReturn(List.of(notaFinal));
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(5L, 2L, 1L))
                .thenReturn(List.of());

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

        // Fila persistida con T3 y anual en null (p.ej. aún no hay notas en el 3er trimestre).
        NotaFinal notaFinal = NotaFinal.builder()
                .estudiante(estudiante())
                .asignatura(asignatura)
                .anio(2025)
                .nfT1(new BigDecimal("5.5"))
                .nfT2(new BigDecimal("6.0"))
                .nfT3(null)
                .nfAnual(null)
                .build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(asignaturaRepository.findById(2L)).thenReturn(Optional.of(asignatura));
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(t1));
        when(matriculaRepository.findEstudiantesByCursoAndAnio(3L, 2025))
                .thenReturn(List.of(estudiante()));
        when(notaFinalRepository.findByAsignaturaIdAndAnio(2L, 2025))
                .thenReturn(List.of(notaFinal));
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(5L, 2L, 1L))
                .thenReturn(List.of());

        GrillaNotasResponse grilla = notaService.getGrilla(3L, 2L, 1L);

        var fila = grilla.getFilas().get(0);
        assertEquals(new BigDecimal("5.5"), fila.getNotaFinalT1());
        assertEquals(new BigDecimal("6.0"), fila.getNotaFinalT2());
        assertNull(fila.getNotaFinalT3());
        assertNull(fila.getNotaFinalAnual());
    }

    @Test
    void getGrilla_sinFilaPersistidaDejaLasNotasFinalesEnNull() {
        Curso curso = Curso.builder().id(3L).nombre("1\u00b0 B\u00e1sico").build();
        Asignatura asignatura = Asignatura.builder().id(2L).nombre("Matem\u00e1tica").build();
        Trimestre t1 = Trimestre.builder().id(1L).numero(1).anio(2025)
                .estado(EstadoTrimestre.ABIERTO).build();

        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(asignaturaRepository.findById(2L)).thenReturn(Optional.of(asignatura));
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(t1));
        when(matriculaRepository.findEstudiantesByCursoAndAnio(3L, 2025))
                .thenReturn(List.of(estudiante()));
        when(notaFinalRepository.findByAsignaturaIdAndAnio(2L, 2025)).thenReturn(List.of());
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(5L, 2L, 1L))
                .thenReturn(List.of());

        GrillaNotasResponse grilla = notaService.getGrilla(3L, 2L, 1L);

        var fila = grilla.getFilas().get(0);
        assertNull(fila.getNotaFinalT1());
        assertNull(fila.getNotaFinalT2());
        assertNull(fila.getNotaFinalT3());
        assertNull(fila.getNotaFinalAnual());
    }

    @Test
    void guardar_notaNueva_disparaUpsertDeNotaFinal() {
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(trimestre()));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante()));
        Curso curso = new Curso();
        curso.setId(3L);
        Matricula matricula = Matricula.builder().id(99L).curso(curso).anio(2025).build();
        when(matriculaRepository.findByEstudianteIdAndAnio(5L, 2025))
                .thenReturn(Optional.of(matricula));
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreIdAndPosicion(
                5L, 2L, 1L, 1)).thenReturn(Optional.empty());
        when(notaRepository.countByEstudianteIdAndAsignaturaIdAndTrimestreId(5L, 2L, 1L))
                .thenReturn(0L);
        when(asignaturaRepository.findById(2L))
                .thenReturn(Optional.of(Asignatura.builder().id(2L).build()));
        when(notaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notaService.guardar(request());

        verify(notaRepository).save(any());
        verify(notaFinalService).recalcularYGuardar(5L, 2L, 2025);
    }
}
