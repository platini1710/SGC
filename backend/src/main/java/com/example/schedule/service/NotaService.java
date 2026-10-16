package com.example.schedule.service;

import com.example.schedule.dto.*;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.exception.TrimestreCerradoException;
import com.example.schedule.model.*;
import com.example.schedule.repository.*;
import com.example.schedule.security.AuditoriaHelper;
import com.example.schedule.security.NotaAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotaService {

    private static final int MAX_NOTAS = 9;

    private final NotaRepository notaRepository;
    private final TrimestreRepository trimestreRepository;
    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final CalculoNotasService calculoNotasService;
    private final NotaAccessChecker notaAccessChecker;
    private final AuditoriaHelper auditoriaHelper;

    public GrillaNotasResponse getGrilla(Long cursoId, Long asignaturaId, Long trimestreId) {
        notaAccessChecker.verificar(cursoId, asignaturaId);
        Curso curso = getCurso(cursoId);
        Asignatura asignatura = getAsignatura(asignaturaId);
        Trimestre trimestre = getTrimestre(trimestreId);

        // Alumnos del curso según la matrícula del año del trimestre (sin matrículas → filas vacío).
        int anio = trimestre.getAnio();
        List<Estudiante> estudiantes =
                matriculaRepository.findEstudiantesByCursoAndAnio(cursoId, anio);

        // Ids de los tres trimestres del año (null si alguno no existe), con la misma
        // búsqueda que usa getConsolidado.
        Long t1Id = trimestreId(1, anio);
        Long t2Id = trimestreId(2, anio);
        Long t3Id = trimestreId(3, anio);

        List<FilaNotaDTO> filas = estudiantes.stream()
                .map(estudiante -> {
                    List<NotaResponse> notas = notaRepository
                            .findByEstudianteIdAndAsignaturaIdAndTrimestreId(
                                    estudiante.getId(), asignaturaId, trimestreId)
                            .stream()
                            .sorted(Comparator.comparing(Nota::getPosicion))
                            .map(this::toNotaResponse)
                            .toList();
                    return FilaNotaDTO.builder()
                            .estudianteId(estudiante.getId())
                            .nombreEstudiante(nombreEstudiante(estudiante))
                            .notas(notas)
                            .promedio(calculoNotasService.promedioTrimestre(
                                    estudiante.getId(), asignaturaId, trimestreId))
                            .notaFinalT1(promedio(estudiante.getId(), asignaturaId, t1Id))
                            .notaFinalT2(promedio(estudiante.getId(), asignaturaId, t2Id))
                            .notaFinalT3(promedio(estudiante.getId(), asignaturaId, t3Id))
                            .notaFinalAnual(calculoNotasService.notaFinalAnual(
                                    estudiante.getId(), asignaturaId, anio))
                            .build();
                })
                .toList();

        return GrillaNotasResponse.builder()
                .cursoId(curso.getId())
                .nombreCurso(curso.getNombre())
                .asignaturaId(asignatura.getId())
                .nombreAsignatura(asignatura.getNombre())
                .trimestreId(trimestre.getId())
                .numeroTrimestre(trimestre.getNumero())
                .estadoTrimestre(trimestre.getEstado().name())
                .filas(filas)
                .build();
    }

    public NotaResponse guardar(NotaRequest req) {
        Trimestre trimestre = getTrimestre(req.getTrimestreId());
        if (trimestre.getEstado() == EstadoTrimestre.CERRADO) {
            throw new TrimestreCerradoException("El trimestre está cerrado");
        }

        // Derivar curso del alumno desde la matrícula del año del trimestre (I2) y verificar
        // acceso ANTES de ramificar/persistir.
        Estudiante estudiante = getEstudiante(req.getEstudianteId());
        Matricula matricula = matriculaRepository
                .findByEstudianteIdAndAnio(estudiante.getId(), trimestre.getAnio())
                .orElseThrow(() -> new SchedulingException(
                        "El alumno no tiene matricula en el ano " + trimestre.getAnio()));
        Long cursoId = matricula.getCurso().getId();
        notaAccessChecker.verificar(cursoId, req.getAsignaturaId());

        String usuario = auditoriaHelper.usuarioActual();
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        Nota nota = notaRepository
                .findByEstudianteIdAndAsignaturaIdAndTrimestreIdAndPosicion(
                        req.getEstudianteId(), req.getAsignaturaId(), req.getTrimestreId(), req.getPosicion())
                .orElse(null);

        if (nota != null) {
            nota.setValor(req.getValor());
            nota.setUsuarioModificacion(usuario);
            nota.setFechaModificacion(hoy);
            nota.setHoraModificacion(ahora);
            return toNotaResponse(notaRepository.save(nota));
        }

        long existentes = notaRepository.countByEstudianteIdAndAsignaturaIdAndTrimestreId(
                req.getEstudianteId(), req.getAsignaturaId(), req.getTrimestreId());
        if (existentes >= MAX_NOTAS) {
            throw new SchedulingException("Máximo 9 notas por trimestre");
        }

        Asignatura asignatura = getAsignatura(req.getAsignaturaId());

        Nota nueva = Nota.builder()
                .estudiante(estudiante)
                .asignatura(asignatura)
                .trimestre(trimestre)
                .valor(req.getValor())
                .posicion(req.getPosicion())
                .usuarioModificacion(usuario)
                .fechaModificacion(hoy)
                .horaModificacion(ahora)
                .build();
        return toNotaResponse(notaRepository.save(nueva));
    }

    public void eliminar(Long id) {
        Nota nota = notaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada con id: " + id));
        notaAccessChecker.verificar(nota.getEstudiante().getCurso().getId(), nota.getAsignatura().getId());
        if (nota.getTrimestre().getEstado() == EstadoTrimestre.CERRADO) {
            throw new TrimestreCerradoException("El trimestre está cerrado");
        }
        notaRepository.delete(nota);
    }

    public ConsolidadoAnualResponse getConsolidado(Long cursoId, Long asignaturaId, int anio) {
        notaAccessChecker.verificar(cursoId, asignaturaId);
        Curso curso = getCurso(cursoId);
        Asignatura asignatura = getAsignatura(asignaturaId);

        Long t1Id = trimestreId(1, anio);
        Long t2Id = trimestreId(2, anio);
        Long t3Id = trimestreId(3, anio);

        List<Estudiante> estudiantes =
                matriculaRepository.findEstudiantesByCursoAndAnio(cursoId, anio);

        List<FilaConsolidadoDTO> filas = estudiantes.stream()
                .map(estudiante -> FilaConsolidadoDTO.builder()
                        .estudianteId(estudiante.getId())
                        .nombreEstudiante(nombreEstudiante(estudiante))
                        .promedioT1(promedio(estudiante.getId(), asignaturaId, t1Id))
                        .promedioT2(promedio(estudiante.getId(), asignaturaId, t2Id))
                        .promedioT3(promedio(estudiante.getId(), asignaturaId, t3Id))
                        .notaFinal(calculoNotasService.notaFinalAnual(
                                estudiante.getId(), asignaturaId, anio))
                        .build())
                .toList();

        return ConsolidadoAnualResponse.builder()
                .cursoId(curso.getId())
                .asignaturaId(asignatura.getId())
                .anio(anio)
                .filas(filas)
                .build();
    }

    public List<AsignacionDocenteDTO> misAsignaciones() {
        Usuario usuario = notaAccessChecker.usuarioAutenticado();
        List<CursoAsignatura> asignaciones;
        if ("DOCENTE".equals(usuario.getRol()) && usuario.getProfesor() != null) {
            asignaciones = cursoAsignaturaRepository.findByProfesorId(usuario.getProfesor().getId());
        } else if ("ADMIN".equals(usuario.getRol())) {
            asignaciones = cursoAsignaturaRepository.findAll();
        } else {
            asignaciones = List.of();
        }
        return asignaciones.stream()
                .map(ca -> AsignacionDocenteDTO.builder()
                        .cursoId(ca.getCurso().getId())
                        .nombreCurso(ca.getCurso().getNombre())
                        .asignaturaId(ca.getAsignatura().getId())
                        .nombreAsignatura(ca.getAsignatura().getNombre())
                        .build())
                .toList();
    }

    private Long trimestreId(int numero, int anio) {
        return trimestreRepository.findByNumeroAndAnio(numero, anio)
                .map(Trimestre::getId)
                .orElse(null);
    }

    private java.math.BigDecimal promedio(Long estudianteId, Long asignaturaId, Long trimestreId) {
        if (trimestreId == null) {
            return null;
        }
        return calculoNotasService.promedioTrimestre(estudianteId, asignaturaId, trimestreId);
    }

    private String nombreEstudiante(Estudiante e) {
        return apellidos(e) + ", " + e.getNombre();
    }

    private String apellidos(Estudiante e) {
        StringBuilder sb = new StringBuilder();
        if (e.getApellidoPaterno() != null && !e.getApellidoPaterno().isBlank()) {
            sb.append(e.getApellidoPaterno().trim());
        }
        if (e.getApellidoMaterno() != null && !e.getApellidoMaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(e.getApellidoMaterno().trim());
        }
        return sb.toString();
    }

    private NotaResponse toNotaResponse(Nota n) {
        return NotaResponse.builder()
                .id(n.getId())
                .posicion(n.getPosicion())
                .valor(n.getValor())
                .build();
    }

    private Curso getCurso(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + id));
    }

    private Asignatura getAsignatura(Long id) {
        return asignaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada con id: " + id));
    }

    private Trimestre getTrimestre(Long id) {
        return trimestreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trimestre no encontrado con id: " + id));
    }

    private Estudiante getEstudiante(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + id));
    }
}
