package com.example.schedule.service;

import com.example.schedule.dto.*;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.*;
import com.example.schedule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public List<CursoResponse> findAll() {
        return cursoRepository.findAllByOrderByNivelAsc().stream()
                .map(this::toResponse)
                .toList();
    }

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
        return CursoDetalleResponse.builder()
                .id(curso.getId())
                .nombre(curso.getNombre())
                .nivel(curso.getNivel())
                .profesorJefeId(profesorJefeId)
                .nombreProfesorJefe(nombreProfesorJefe)
                .asignaturas(asignaturas)
                .build();
    }

    // Asigna (o quita, si profesorId es null) el profesor jefe del curso.
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

    public CursoAsignaturaResponse asignarAsignatura(Long cursoId, CursoAsignaturaRequest request) {
        Curso curso = getById(cursoId);
        Asignatura asignatura = asignaturaRepository.findById(request.getAsignaturaId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada con id: " + request.getAsignaturaId()));

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

    public GrillaHorarioResponse getHorario(Long cursoId) {
        Curso curso = getById(cursoId);
        List<BloqueHorario> bloques = bloqueHorarioRepository.findAllByOrderByNumeroAsc();
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
                                    .build());
                        });
            }
            grilla.put(dia, filaDia);
        }

        return GrillaHorarioResponse.builder()
                .cursoId(curso.getId())
                .nombreCurso(curso.getNombre())
                .grilla(grilla)
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
        return CursoResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .nivel(c.getNivel())
                .profesorJefe(profesorJefe)
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
