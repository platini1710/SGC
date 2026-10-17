package com.example.schedule.service;

import com.example.schedule.dto.*;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.*;
import com.example.schedule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final ProfesorRepository profesorRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final HorarioRepository horarioRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final PlanEstudioRepository planEstudioRepository;

    @Transactional(readOnly = true)
    public List<CursoResponse> findAll() {
        return cursoRepository.findAllByOrderByNivelAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CursoDetalleResponse findByIdConDetalle(Long id) {
        Curso curso = getById(id);
        List<CursoAsignaturaResponse> asignaturas = cursoAsignaturaRepository.findByCursoId(id).stream()
                .map(this::toCursoAsignaturaResponse)
                .toList();
        Long profesorJefeId = null;
        String nombreProfesorJefe = null;
        if (curso.getProfesorJefe() != null) {
            profesorJefeId = curso.getProfesorJefe().getId();
            nombreProfesorJefe = curso.getProfesorJefe().getNombre() + " " + curso.getProfesorJefe().getApellido();
        }
        Long planEstudioId = null;
        String nombrePlanEstudio = null;
        if (curso.getPlanEstudio() != null) {
            planEstudioId = curso.getPlanEstudio().getId();
            nombrePlanEstudio = curso.getPlanEstudio().getNombre();
        }
        return CursoDetalleResponse.builder()
                .id(curso.getId())
                .nombre(curso.getNombre())
                .nivel(curso.getNivel())
                .profesorJefeId(profesorJefeId)
                .nombreProfesorJefe(nombreProfesorJefe)
                .planEstudioId(planEstudioId)
                .nombrePlanEstudio(nombrePlanEstudio)
                .asignaturas(asignaturas)
                .build();
    }

    // Asigna (o quita, si profesorId es null) el profesor jefe del curso.
    @Transactional
    public CursoResponse asignarProfesorJefe(Long cursoId, Long profesorId) {
        Curso curso = getById(cursoId);
        if (profesorId == null) {
            curso.setProfesorJefe(null);
        } else {
            Profesor profesor = profesorRepository.findById(profesorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado con id: " + profesorId));
            curso.setProfesorJefe(profesor);
        }
        return toResponse(cursoRepository.save(curso));
    }

    @Transactional
    public CursoAsignaturaResponse asignarAsignatura(Long cursoId, CursoAsignaturaRequest request) {
        Curso curso = getById(cursoId);
        Asignatura asignatura = asignaturaRepository.findById(request.getAsignaturaId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada con id: " + request.getAsignaturaId()));

        PlanEstudio plan = curso.getPlanEstudio();
        if (plan == null) {
            throw new SchedulingException(
                    "El curso no tiene un plan de estudio asignado. Asigne primero un plan al curso.");
        }
        boolean enPlan = planEstudioRepository.findByIdFetchAsignaturas(plan.getId())
                .map(p -> p.getAsignaturas().stream().anyMatch(a -> a.getId().equals(asignatura.getId())))
                .orElse(false);
        if (!enPlan) {
            throw new SchedulingException(
                    "La asignatura '" + asignatura.getNombre() + "' no pertenece al plan de estudio '"
                    + plan.getNombre() + "' del curso. Solo puede asignar profesores a asignaturas del plan.");
        }

        Profesor profesor = null;
        if (request.getProfesorId() != null) {
            profesor = profesorRepository.findById(request.getProfesorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado con id: " + request.getProfesorId()));
        }

        CursoAsignatura cursoAsignatura = CursoAsignatura.builder()
                .curso(curso)
                .asignatura(asignatura)
                .horasSemanales(request.getHorasSemanales())
                .profesor(profesor)
                .build();

        return toCursoAsignaturaResponse(cursoAsignaturaRepository.save(cursoAsignatura));
    }

    public void quitarAsignatura(Long cursoId, Long asignaturaId) {
        CursoAsignatura cursoAsignatura = cursoAsignaturaRepository
                .findByCursoIdAndAsignaturaId(cursoId, asignaturaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignatura " + asignaturaId + " no asignada al curso " + cursoId));
        cursoAsignaturaRepository.delete(cursoAsignatura);
    }

    // Asigna (o quita, si planId es null) el plan de estudio del curso.
    @Transactional
    public CursoResponse asignarPlanEstudio(Long cursoId, Long planId) {
        Curso curso = getById(cursoId);
        if (planId == null) {
            curso.setPlanEstudio(null);
        } else {
            PlanEstudio plan = planEstudioRepository.findById(planId)
                    .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado con id: " + planId));
            curso.setPlanEstudio(plan);
        }
        return toResponse(cursoRepository.save(curso));
    }

    // Lista las asignaturas del plan del curso (malla). Vacío si el curso no tiene plan.
    @Transactional
    public List<AsignaturaResponse> asignaturasDisponibles(Long cursoId) {
        Curso curso = getById(cursoId);
        PlanEstudio plan = curso.getPlanEstudio();
        if (plan == null) {
            return List.of();
        }
        return planEstudioRepository.findByIdFetchAsignaturas(plan.getId())
                .map(p -> p.getAsignaturas().stream()
                        .map(a -> AsignaturaResponse.builder()
                                .id(a.getId())
                                .nombre(a.getNombre())
                                .codigo(a.getCodigo())
                                .descripcion(a.getDescripcion())
                                .build())
                        .toList())
                .orElseGet(List::of);
    }

    public GrillaHorarioResponse getHorario(Long cursoId) {
        Curso curso = getById(cursoId);
        // Un curso tiene una sola jornada según su nivel (1..4 => JEC, >=5 => REGULAR).
        // Cargamos SÓLO los bloques de esa jornada, de modo que el put por bloque.numero no
        // colisione entre jornadas. El match sigue siendo por bloque.id (invariante real).
        String jornada = (curso.getNivel() != null && curso.getNivel() <= 4) ? "JEC" : "REGULAR";
        List<BloqueHorario> bloques = bloqueHorarioRepository.findByJornadaOrderByNumeroAsc(jornada);
        List<Horario> horarios = horarioRepository.findByCursoId(cursoId);

        Map<Integer, Map<Integer, HorarioCeldaDTO>> grilla = new HashMap<>();
        for (int dia = 1; dia <= 5; dia++) {
            Map<Integer, HorarioCeldaDTO> filaDia = new HashMap<>();
            for (BloqueHorario bloque : bloques) {
                int diaFinal = dia;
                horarios.stream()
                        // null-safe: avoids NPE when diaSemana is null (finding #2)
                        .filter(h -> Integer.valueOf(diaFinal).equals(h.getDiaSemana()) && h.getBloque().getId().equals(bloque.getId()))
                        .findFirst()
                        .ifPresent(h -> {
                            String color = String.format("#%06X", (0xFFFFFF & (h.getAsignatura().getId() * 2654435761L)));
                            filaDia.put(bloque.getNumero(), HorarioCeldaDTO.builder()
                                    .horarioId(h.getId())
                                    .asignaturaId(h.getAsignatura().getId())
                                    .nombreAsignatura(h.getAsignatura().getNombre())
                                    .profesorId(h.getProfesor().getId())
                                    .nombreProfesor(h.getProfesor().getNombre() + " " + h.getProfesor().getApellido())
                                    .color(color)
                                    .esMedioBloque(Boolean.TRUE.equals(h.getEsMedioBloque()))
                                    .asignaturaSecId(h.getAsignaturaSecundaria() != null ? h.getAsignaturaSecundaria().getId() : null)
                                    .nombreAsignaturaSec(h.getAsignaturaSecundaria() != null ? h.getAsignaturaSecundaria().getNombre() : null)
                                    .profesorSecId(h.getProfesorSecundaria() != null ? h.getProfesorSecundaria().getId() : null)
                                    .nombreProfesorSec(h.getProfesorSecundaria() != null
                                            ? h.getProfesorSecundaria().getNombre() + " " + h.getProfesorSecundaria().getApellido() : null)
                                    .build());
                        });
            }
            grilla.put(dia, filaDia);
        }

        return GrillaHorarioResponse.builder()
                .cursoId(curso.getId())
                .nombreCurso(curso.getNombre())
                .grilla(grilla)
                .bloques(bloques.stream().map(this::toBloqueDTO).toList())
                .build();
    }

    private BloqueDTO toBloqueDTO(BloqueHorario b) {
        String jornada = b.getJornada();
        return BloqueDTO.builder()
                .id(b.getId())
                .numero(b.getNumero())
                .jornada(jornada)
                .horaInicio(b.getHoraInicio())
                .horaFin(b.getHoraFin())
                .esRecreo(b.getEsRecreo())
                .claveRender(jornada + "-" + b.getNumero())
                .build();
    }

    public Curso getById(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + id));
    }

    private CursoResponse toResponse(Curso c) {
        String profesorJefe = null;
        if (c.getProfesorJefe() != null) {
            profesorJefe = c.getProfesorJefe().getNombre() + " " + c.getProfesorJefe().getApellido();
        }
        Long planId = c.getPlanEstudio() != null ? c.getPlanEstudio().getId() : null;
        String planNombre = c.getPlanEstudio() != null ? c.getPlanEstudio().getNombre() : null;
        return CursoResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .nivel(c.getNivel())
                .profesorJefe(profesorJefe)
                .planEstudioId(planId)
                .nombrePlanEstudio(planNombre)
                .build();
    }

    private CursoAsignaturaResponse toCursoAsignaturaResponse(CursoAsignatura ca) {
        Long profesorId = null;
        String nombreProfesor = null;
        if (ca.getProfesor() != null) {
            profesorId = ca.getProfesor().getId();
            nombreProfesor = ca.getProfesor().getNombre() + " " + ca.getProfesor().getApellido();
        }
        return CursoAsignaturaResponse.builder()
                .asignaturaId(ca.getAsignatura().getId())
                .nombreAsignatura(ca.getAsignatura().getNombre())
                .horasSemanales(ca.getHorasSemanales())
                .profesorId(profesorId)
                .nombreProfesor(nombreProfesor)
                .build();
    }
}
