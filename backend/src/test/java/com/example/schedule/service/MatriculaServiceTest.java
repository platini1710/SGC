package com.example.schedule.service;

import com.example.schedule.dto.PromocionConfirmRequest;
import com.example.schedule.dto.PromocionDecisionDTO;
import com.example.schedule.dto.PromocionItemDTO;
import com.example.schedule.dto.PromocionPreviewResponse;
import com.example.schedule.dto.PromocionResultadoResponse;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Curso;
import com.example.schedule.model.EstadoMatricula;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Matricula;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private AnioEscolarService anioEscolarService;
    @Mock
    private TrimestreService trimestreService;
    @Mock
    private AuditoriaHelper auditoriaHelper;

    @InjectMocks
    private MatriculaService matriculaService;

    private Curso curso(Long id, int nivel) {
        Curso c = new Curso();
        c.setId(id);
        c.setNivel(nivel);
        c.setNombre(nivel + "\u00b0 B\u00e1sico");
        return c;
    }

    private Estudiante estudiante(Long id) {
        Estudiante e = new Estudiante();
        e.setId(id);
        e.setNombre("Ana");
        e.setApellidoPaterno("P\u00e9rez");
        e.setActivo(true);
        return e;
    }

    private Matricula matriculaActiva(Long id, Estudiante e, Curso c, int anio) {
        return Matricula.builder()
                .id(id).estudiante(e).curso(c).anio(anio).estado(EstadoMatricula.ACTIVA)
                .build();
    }

    // ─── previewPromocion ──────────────────────────────────────────────────

    @Test
    void preview_promueveAlNivelSiguiente_sinEscribir() {
        Estudiante e = estudiante(10L);
        Curso c3 = curso(3L, 3);
        Curso c4 = curso(4L, 4);
        when(cursoRepository.findAllByOrderByNivelAsc())
                .thenReturn(List.of(curso(1L, 1), c3, c4, curso(8L, 8)));
        when(matriculaRepository.findByAnioAndEstado(2025, EstadoMatricula.ACTIVA))
                .thenReturn(List.of(matriculaActiva(1L, e, c3, 2025)));
        when(cursoRepository.findByNivel(4)).thenReturn(Optional.of(c4));
        when(matriculaRepository.existsByEstudianteIdAndAnio(10L, 2026)).thenReturn(false);

        PromocionPreviewResponse res = matriculaService.previewPromocion(2025, 2026);

        assertEquals(1, res.getItems().size());
        PromocionItemDTO item = res.getItems().get(0);
        assertEquals("PROMOVER", item.getAccionPropuesta());
        assertEquals(4L, item.getCursoDestinoId());
        assertEquals(false, item.isYaExisteEnDestino());
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void preview_nivelMaximoEgresa() {
        Estudiante e = estudiante(20L);
        Curso c8 = curso(8L, 8);
        when(cursoRepository.findAllByOrderByNivelAsc())
                .thenReturn(List.of(curso(1L, 1), c8));
        when(matriculaRepository.findByAnioAndEstado(2025, EstadoMatricula.ACTIVA))
                .thenReturn(List.of(matriculaActiva(1L, e, c8, 2025)));
        when(matriculaRepository.existsByEstudianteIdAndAnio(20L, 2026)).thenReturn(true);

        PromocionPreviewResponse res = matriculaService.previewPromocion(2025, 2026);

        PromocionItemDTO item = res.getItems().get(0);
        assertEquals("EGRESA", item.getAccionPropuesta());
        assertTrue(item.isYaExisteEnDestino());
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void preview_sinCursoDestino_marcaIncidencia() {
        Estudiante e = estudiante(30L);
        Curso c3 = curso(3L, 3);
        // nivelMax = 8 pero no existe curso de nivel 4
        when(cursoRepository.findAllByOrderByNivelAsc())
                .thenReturn(List.of(curso(1L, 1), c3, curso(8L, 8)));
        when(matriculaRepository.findByAnioAndEstado(2025, EstadoMatricula.ACTIVA))
                .thenReturn(List.of(matriculaActiva(1L, e, c3, 2025)));
        when(cursoRepository.findByNivel(4)).thenReturn(Optional.empty());
        when(matriculaRepository.existsByEstudianteIdAndAnio(30L, 2026)).thenReturn(false);

        PromocionPreviewResponse res = matriculaService.previewPromocion(2025, 2026);

        assertEquals("SIN_CURSO_DESTINO", res.getItems().get(0).getAccionPropuesta());
        assertEquals(1, res.getIncidencias().size());
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void preview_mismoAnio_lanza422() {
        assertThrows(SchedulingException.class,
                () -> matriculaService.previewPromocion(2025, 2025));
    }

    // ─── confirmarPromocion ─────────────────────────────────────────────────

    @Test
    void confirmar_promoverRepitenteExcluirEgresa_cuentaYCreaTrimestres() {
        Estudiante ePro = estudiante(10L);
        Estudiante eRep = estudiante(11L);
        Estudiante eExc = estudiante(12L);
        Estudiante eEgr = estudiante(13L);
        Curso c3 = curso(3L, 3);
        Curso c4 = curso(4L, 4);
        Curso c8 = curso(8L, 8);

        when(matriculaRepository.findByEstudianteIdAndAnio(10L, 2025))
                .thenReturn(Optional.of(matriculaActiva(1L, ePro, c3, 2025)));
        when(matriculaRepository.findByEstudianteIdAndAnio(11L, 2025))
                .thenReturn(Optional.of(matriculaActiva(2L, eRep, c3, 2025)));
        when(matriculaRepository.findByEstudianteIdAndAnio(12L, 2025))
                .thenReturn(Optional.of(matriculaActiva(3L, eExc, c4, 2025)));
        when(matriculaRepository.findByEstudianteIdAndAnio(13L, 2025))
                .thenReturn(Optional.of(matriculaActiva(4L, eEgr, c8, 2025)));
        // Sin matrícula destino preexistente -> insert.
        when(matriculaRepository.findByEstudianteIdAndAnio(10L, 2026)).thenReturn(Optional.empty());
        when(matriculaRepository.findByEstudianteIdAndAnio(11L, 2026)).thenReturn(Optional.empty());
        when(cursoRepository.findById(4L)).thenReturn(Optional.of(c4));
        when(anioEscolarService.resolver(null)).thenReturn(2025); // destino 2026 != activo -> no espejo
        lenient().when(auditoriaHelper.usuarioActual()).thenReturn("admin");

        PromocionConfirmRequest req = PromocionConfirmRequest.builder()
                .anioOrigen(2025).anioDestino(2026)
                .decisiones(List.of(
                        PromocionDecisionDTO.builder().estudianteId(10L).accion("PROMOVER").cursoDestinoId(4L).build(),
                        PromocionDecisionDTO.builder().estudianteId(11L).accion("REPITENTE").build(),
                        PromocionDecisionDTO.builder().estudianteId(12L).accion("EXCLUIR").build(),
                        PromocionDecisionDTO.builder().estudianteId(13L).accion("EGRESA").build()))
                .build();

        PromocionResultadoResponse res = matriculaService.confirmarPromocion(req);

        assertEquals(1, res.getPromovidos());
        assertEquals(1, res.getRepitentes());
        assertEquals(1, res.getExcluidos());
        assertEquals(1, res.getEgresados());
        assertEquals(0, res.getActualizados());
        verify(trimestreService).crearTrimestresDeAnio(2026);
        // Dos upserts (PROMOVER + REPITENTE), ninguno al espejo (destino != activo).
        verify(matriculaRepository, times(2)).save(any(Matricula.class));
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void confirmar_upsertPreexistente_actualizaSin409() {
        Estudiante e = estudiante(10L);
        Curso c3 = curso(3L, 3);
        Curso c4 = curso(4L, 4);
        when(matriculaRepository.findByEstudianteIdAndAnio(10L, 2025))
                .thenReturn(Optional.of(matriculaActiva(1L, e, c3, 2025)));
        Matricula destinoPrevio = matriculaActiva(99L, e, c3, 2026);
        when(matriculaRepository.findByEstudianteIdAndAnio(10L, 2026))
                .thenReturn(Optional.of(destinoPrevio));
        when(cursoRepository.findById(4L)).thenReturn(Optional.of(c4));
        when(anioEscolarService.resolver(null)).thenReturn(2026); // destino == activo -> espejo
        lenient().when(auditoriaHelper.usuarioActual()).thenReturn("admin");

        PromocionConfirmRequest req = PromocionConfirmRequest.builder()
                .anioOrigen(2025).anioDestino(2026)
                .decisiones(List.of(
                        PromocionDecisionDTO.builder().estudianteId(10L).accion("PROMOVER").cursoDestinoId(4L).build()))
                .build();

        PromocionResultadoResponse res = matriculaService.confirmarPromocion(req);

        assertEquals(1, res.getPromovidos());
        assertEquals(1, res.getActualizados());
        // Actualiza la matrícula preexistente al nuevo curso (no inserta otra).
        ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
        verify(matriculaRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getId());
        assertEquals(c4, captor.getValue().getCurso());
        // Espejo sincronizado (destino == activo).
        verify(estudianteRepository).save(e);
    }

    @Test
    void confirmar_accionInvalida_lanza422YNoEscribe() {
        Estudiante e = estudiante(10L);
        Curso c3 = curso(3L, 3);
        lenient().when(matriculaRepository.findByEstudianteIdAndAnio(10L, 2025))
                .thenReturn(Optional.of(matriculaActiva(1L, e, c3, 2025)));
        lenient().when(anioEscolarService.resolver(null)).thenReturn(2025);

        PromocionConfirmRequest req = PromocionConfirmRequest.builder()
                .anioOrigen(2025).anioDestino(2026)
                .decisiones(List.of(
                        PromocionDecisionDTO.builder().estudianteId(10L).accion("PROMOCION").build()))
                .build();

        assertThrows(SchedulingException.class, () -> matriculaService.confirmarPromocion(req));
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void confirmar_promoverSinCursoDestino_lanza422() {
        Estudiante e = estudiante(10L);
        Curso c3 = curso(3L, 3);
        when(matriculaRepository.findByEstudianteIdAndAnio(10L, 2025))
                .thenReturn(Optional.of(matriculaActiva(1L, e, c3, 2025)));
        lenient().when(anioEscolarService.resolver(null)).thenReturn(2025);

        PromocionConfirmRequest req = PromocionConfirmRequest.builder()
                .anioOrigen(2025).anioDestino(2026)
                .decisiones(List.of(
                        PromocionDecisionDTO.builder().estudianteId(10L).accion("PROMOVER").build()))
                .build();

        assertThrows(SchedulingException.class, () -> matriculaService.confirmarPromocion(req));
        verify(matriculaRepository, never()).save(any());
    }
}
