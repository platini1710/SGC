package com.example.schedule.service;

import com.example.schedule.dto.*;
import com.example.schedule.exception.ConflictoHorarioException;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.*;
import com.example.schedule.repository.*;
import com.example.schedule.security.AuditoriaHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final ProfesorRepository profesorRepository;
    private final SchedulerService schedulerService;
    private final ConflictValidatorService conflictValidatorService;
    private final AuditoriaHelper auditoriaHelper;

    public List<HorarioResponse> generarHorario() {
        return schedulerService.generarHorario().stream()
                .map(this::toHorarioResponse)
                .toList();
    }

    public List<HorarioResponse> getHorarioCompleto() {
        return horarioRepository.findAll().stream()
                .map(this::toHorarioResponse)
                .toList();
    }

    public GrillaHorarioResponse getGrillaCurso(Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + cursoId));
        List<BloqueHorario> bloques = bloqueHorarioRepository.findAllByOrderByNumeroAsc();
        List<Horario> horarios = horarioRepository.findByCursoId(cursoId);

        Map<Integer, Map<Integer, HorarioCeldaDTO>> grilla = buildGrillaCurso(bloques, horarios);

        return GrillaHorarioResponse.builder()
                .cursoId(curso.getId())
                .nombreCurso(curso.getNombre())
                .grilla(grilla)
                .build();
    }

    public GrillaHorarioResponse getGrillaProfesor(Long profesorId) {
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado con id: " + profesorId));
        List<BloqueHorario> bloques = bloqueHorarioRepository.findAllByOrderByNumeroAsc();
        List<Horario> horarios = horarioRepository.findByProfesorId(profesorId);

        Map<Integer, Map<Integer, HorarioCeldaDTO>> grilla = new HashMap<>();
        for (int dia = 1; dia <= 5; dia++) {
            Map<Integer, HorarioCeldaDTO> filaDia = new HashMap<>();
            for (BloqueHorario bloque : bloques) {
                int diaFinal = dia;
                horarios.stream()
                        // null-safe: avoids NPE when diaSemana is null (finding #2)
                        .filter(h -> Integer.valueOf(diaFinal).equals(h.getDiaSemana())
                                && h.getBloque().getId().equals(bloque.getId()))
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
                                    .nombreCurso(h.getCurso().getNombre())
                                    .build());
                        });
            }
            grilla.put(dia, filaDia);
        }

        return GrillaHorarioResponse.builder()
                .cursoId(null)
                .nombreCurso(profesor.getNombre() + " " + profesor.getApellido())
                .grilla(grilla)
                .build();
    }

    @Transactional
    public HorarioResponse moverBloque(Long horarioId, MoverBloqueRequest request) {
        Horario horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado con id: " + horarioId));

        List<ConflictoResponse> conflictos = conflictValidatorService.validarMovimiento(
                horarioId, request.getDiaSemana(), request.getBloqueId(), request.getProfesorId());

        if (!conflictos.isEmpty()) {
            throw new ConflictoHorarioException(conflictos.get(0));
        }

        BloqueHorario bloque = bloqueHorarioRepository.findById(request.getBloqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Bloque no encontrado con id: " + request.getBloqueId()));

        Profesor profesor = profesorRepository.findById(request.getProfesorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado con id: " + request.getProfesorId()));

        horario.setDiaSemana(request.getDiaSemana());
        horario.setBloque(bloque);
        horario.setProfesor(profesor);
        horario.setEditadoManualmente(true);
        horario.setUsuarioModificacion(auditoriaHelper.usuarioActual());
        horario.setFechaModificacion(LocalDate.now());
        horario.setHoraModificacion(LocalTime.now());

        return toHorarioResponse(horarioRepository.save(horario));
    }

    @Transactional
    public void borrarHorario() {
        horarioRepository.deleteByEditadoManualmenteFalse();
    }

    private Map<Integer, Map<Integer, HorarioCeldaDTO>> buildGrillaCurso(
            List<BloqueHorario> bloques, List<Horario> horarios) {
        Map<Integer, Map<Integer, HorarioCeldaDTO>> grilla = new HashMap<>();
        for (int dia = 1; dia <= 5; dia++) {
            Map<Integer, HorarioCeldaDTO> filaDia = new HashMap<>();
            for (BloqueHorario bloque : bloques) {
                int diaFinal = dia;
                horarios.stream()
                        // null-safe: avoids NPE when diaSemana is null (finding #2)
                        .filter(h -> Integer.valueOf(diaFinal).equals(h.getDiaSemana())
                                && h.getBloque().getId().equals(bloque.getId()))
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
        return grilla;
    }

    private HorarioResponse toHorarioResponse(Horario h) {
        return HorarioResponse.builder()
                .id(h.getId())
                .cursoId(h.getCurso().getId())
                .nombreCurso(h.getCurso().getNombre())
                .asignaturaId(h.getAsignatura().getId())
                .nombreAsignatura(h.getAsignatura().getNombre())
                .profesorId(h.getProfesor().getId())
                .nombreProfesor(h.getProfesor().getNombre() + " " + h.getProfesor().getApellido())
                .diaSemana(h.getDiaSemana())
                .bloqueId(h.getBloque().getId())
                .horaInicio(h.getBloque().getHoraInicio())
                .horaFin(h.getBloque().getHoraFin())
                .editadoManualmente(h.getEditadoManualmente())
                .build();
    }
}
