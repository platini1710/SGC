package com.example.schedule.service;

import com.example.schedule.model.Curso;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.ColegioRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.NotaRepository;
import com.example.schedule.repository.TrimestreRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformeAlumnoServiceTest {

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private TrimestreRepository trimestreRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Mock
    private NotaRepository notaRepository;
    @Mock
    private CalculoNotasService calculoNotasService;
    @Mock
    private ColegioRepository colegioRepository;
    @Mock
    private NotaAccessChecker notaAccessChecker;

    @InjectMocks
    private InformeAlumnoService informeAlumnoService;

    private Curso curso(Long id) {
        Curso c = new Curso();
        c.setId(id);
        c.setNombre("1\u00b0 B\u00e1sico");
        return c;
    }

    private Trimestre trimestre(Long id) {
        return Trimestre.builder().id(id).numero(1).anio(2025).build();
    }

    private Estudiante estudiante(Long id, Curso curso) {
        return Estudiante.builder().id(id).nombre("Ana").apellido("P\u00e9rez").curso(curso).build();
    }

    @Test
    void generarBoletin_verificaAccesoAntesDeConstruir() {
        Curso curso = curso(1L);
        lenient().when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        lenient().when(trimestreRepository.findById(1L)).thenReturn(Optional.of(trimestre(1L)));
        lenient().when(estudianteRepository.findByCursoIdOrderByApellidoAscNombreAsc(1L))
                .thenReturn(List.of());
        lenient().when(cursoAsignaturaRepository.findByCursoId(1L)).thenReturn(List.of());
        lenient().when(colegioRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        informeAlumnoService.generarBoletin(1L, 1L, null);

        verify(notaAccessChecker).verificarCurso(1L);
    }

    @Test
    void generarBoletin_unAlumnoSinNotas_devuelvePdfNoVacio() {
        Curso curso = curso(1L);
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(trimestre(1L)));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante(5L, curso)));
        when(cursoAsignaturaRepository.findByCursoId(1L)).thenReturn(List.of());
        when(colegioRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        byte[] pdf = informeAlumnoService.generarBoletin(1L, 1L, 5L);

        assertTrue(pdf.length > 0, "El boletín debe generarse aun sin notas");
    }

    @Test
    void generarBoletin_estudianteIdNull_usaListaDelCurso() {
        Curso curso = curso(1L);
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(trimestre(1L)));
        when(estudianteRepository.findByCursoIdOrderByApellidoAscNombreAsc(1L))
                .thenReturn(List.of(estudiante(1L, curso), estudiante(2L, curso)));
        when(cursoAsignaturaRepository.findByCursoId(1L)).thenReturn(List.of());
        when(colegioRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        byte[] pdf = informeAlumnoService.generarBoletin(1L, 1L, null);

        verify(estudianteRepository).findByCursoIdOrderByApellidoAscNombreAsc(1L);
        assertTrue(pdf.length > 0);
    }

    @Test
    void promedioGeneral_promedioSimpleAUnDecimal() {
        BigDecimal general = informeAlumnoService.promedioGeneral(
                List.of(new BigDecimal("6.0"), new BigDecimal("5.0"), new BigDecimal("4.0")));
        assertEquals(new BigDecimal("5.0"), general);
    }

    @Test
    void promedioGeneral_sinPromedios_devuelveNull() {
        assertNull(informeAlumnoService.promedioGeneral(List.of()));
    }

    @Test
    void mapearNotas_listaNula_devuelveMapaVacio() {
        assertTrue(informeAlumnoService.mapearNotas(null).isEmpty());
    }
}
