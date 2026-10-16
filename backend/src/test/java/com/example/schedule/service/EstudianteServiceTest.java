package com.example.schedule.service;

import com.example.schedule.dto.EstudianteRequest;
import com.example.schedule.model.Curso;
import com.example.schedule.model.EstadoMatricula;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Matricula;
import com.example.schedule.repository.ApoderadoRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.MatriculaRepository;
import com.example.schedule.security.AuditoriaHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AnioEscolarService anioEscolarService;
    @Mock
    private AuditoriaHelper auditoriaHelper;

    @InjectMocks
    private EstudianteService estudianteService;

    private Curso curso(Long id) {
        Curso c = new Curso();
        c.setId(id);
        c.setNombre("1\u00b0 B\u00e1sico");
        c.setNivel(1);
        return c;
    }

    private EstudianteRequest requestConCurso(Long cursoId) {
        return EstudianteRequest.builder()
                .nombre("Ana")
                .apellidoPaterno("P\u00e9rez")
                .rut("20.000.000-1")
                .cursoId(cursoId)
                .build();
    }

    @Test
    void create_conMatriculaExistente_actualizaCursoSin409() {
        Curso curso = curso(3L);
        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(inv -> {
                    Estudiante e = inv.getArgument(0);
                    e.setId(10L);
                    return e;
                });
        lenient().when(auditoriaHelper.usuarioActual()).thenReturn("admin");
        when(anioEscolarService.resolver(null)).thenReturn(2025);
        Matricula existente = Matricula.builder()
                .id(99L).anio(2025).estado(EstadoMatricula.ACTIVA).build();
        when(matriculaRepository.findByEstudianteIdAndAnio(10L, 2025))
                .thenReturn(Optional.of(existente));

        estudianteService.create(requestConCurso(3L));

        // El camino "existe" actualiza (no inserta): nunca lanza 409.
        ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
        verify(matriculaRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getId());
        assertEquals(curso, captor.getValue().getCurso());
    }

    @Test
    void create_sinMatriculaPrevia_creaMatriculaActiva() {
        Curso curso = curso(3L);
        when(cursoRepository.findById(3L)).thenReturn(Optional.of(curso));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(inv -> {
                    Estudiante e = inv.getArgument(0);
                    e.setId(11L);
                    return e;
                });
        lenient().when(auditoriaHelper.usuarioActual()).thenReturn("admin");
        when(anioEscolarService.resolver(null)).thenReturn(2025);
        when(matriculaRepository.findByEstudianteIdAndAnio(11L, 2025))
                .thenReturn(Optional.empty());

        estudianteService.create(requestConCurso(3L));

        ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
        verify(matriculaRepository).save(captor.capture());
        Matricula creada = captor.getValue();
        assertEquals(2025, creada.getAnio());
        assertEquals(EstadoMatricula.ACTIVA, creada.getEstado());
        assertEquals(curso, creada.getCurso());
    }

    @Test
    void create_sinCurso_noTocaMatriculas() {
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(inv -> {
                    Estudiante e = inv.getArgument(0);
                    e.setId(12L);
                    return e;
                });
        lenient().when(auditoriaHelper.usuarioActual()).thenReturn("admin");

        estudianteService.create(requestConCurso(null));

        verify(matriculaRepository, never()).save(any());
        verify(matriculaRepository, never()).findByEstudianteIdAndAnio(anyLong(), anyInt());
        verify(anioEscolarService, never()).resolver(eq(null));
    }
}
