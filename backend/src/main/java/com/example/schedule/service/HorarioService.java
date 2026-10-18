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
    private final JecSchedulerService jecSchedulerService;
    private final ConflictValidatorService conflictValidatorService;
    private final AuditoriaHelper auditoriaHelper;

    @Transactional
    public List<HorarioResponse> generarHorario() {
        // Un único borrado preserva ediciones manuales antes de las dos pasadas.
        horarioRepository.deleteByEditadoManualmenteFalse();
        List<Horario> resultado = new ArrayList<>();
        resultado.addAll(jecSchedulerService.colocarCursosJec());                       // nivel 1..4, bloques JEC
        resultado.addAll(schedulerService.colocarCursosRegulares(
                ca -> ca.getCurso().getNivel() != null && ca.getCurso().getNivel() >= 5)); // nivel >=5, bloques REGULAR
        horarioRepository.saveAll(resultado);
        return resultado.stream()
                .map(this::toHorarioResponse)
                .toList();
    }

    /**
     * Regenera el horario de UN solo curso sin tocar el de los demás:
     * <ol>
     *   <li>valida que el curso exista (404 si no);</li>
     *   <li>borra solo las filas no editadas manualmente de ese curso;</li>
     *   <li>siembra la ocupación de profesores con los horarios ya persistidos de los OTROS
     *       cursos (profesor principal y profesor del 2.º subtramo de 45') para no duplicar a un
     *       profesor ya colocado en otro curso;</li>
     *   <li>enruta a la pasada JEC (nivel 1..4) o REGULAR (nivel &gt;=5) solo para este curso;</li>
     *   <li>persiste y devuelve el horario resultante del curso.</li>
     * </ol>
     * Si el curso no tiene cargas (CursoAsignatura) devuelve lista vacía. Si la generación es
     * inviable se propaga la {@link com.example.schedule.exception.SchedulingException} (422).
     */
    @Transactional
    public List<HorarioResponse> generarHorarioCurso(Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + cursoId));

        // Borra solo las filas no editadas manualmente del curso objetivo.
        horarioRepository.deleteByCursoIdAndEditadoManualmenteFalse(cursoId);

        // Ocupación de profesores sembrada con TODOS los horarios restantes (otros cursos y las
        // filas editadas manualmente del propio curso). Incluye el profesor del 2.º subtramo.
        Set<String> profesorOcupado = new HashSet<>();
        for (Horario h : horarioRepository.findAll()) {
            if (h.getProfesor() != null) {
                profesorOcupado.add(claveProfesor(h.getProfesor().getId(), h.getDiaSemana(), h.getBloque()));
            }
            if (h.getProfesorSecundaria() != null) {
                profesorOcupado.add(claveProfesor(h.getProfesorSecundaria().getId(), h.getDiaSemana(), h.getBloque()));
            }
        }

        Integer nivel = curso.getNivel();
        List<Horario> resultado;
        if (nivel != null && nivel <= 4) {
            resultado = jecSchedulerService.colocarCursoJecUnico(curso, profesorOcupado);
        } else {
            resultado = schedulerService.colocarCursosRegulares(
                    ca -> ca.getCurso().getId().equals(cursoId), profesorOcupado);
        }

        horarioRepository.saveAll(resultado);
        return resultado.stream()
                .map(this::toHorarioResponse)
                .toList();
    }

    private String claveProfesor(Long profesorId, Integer diaSemana, BloqueHorario bloque) {
        Long bloqueId = bloque != null ? bloque.getId() : null;
        return profesorId + "-" + diaSemana + "-" + bloqueId;
    }

    public List<BloqueDTO> listarBloques() {
        return bloqueHorarioRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((BloqueHorario b) -> b.getJornada() == null ? "" : b.getJornada())
                        .thenComparing(b -> b.getNumero() == null ? 0 : b.getNumero()))
                .map(this::toBloqueDTO)
                .toList();
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

    public List<HorarioResponse> getHorarioCompleto() {
        return horarioRepository.findAll().stream()
                .map(this::toHorarioResponse)
                .toList();
    }

    public GrillaHorarioResponse getGrillaCurso(Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + cursoId));
        String jornada = (curso.getNivel() != null && curso.getNivel() <= 4) ? "JEC" : "REGULAR";
        List<BloqueHorario> bloques = bloqueHorarioRepository.findByJornadaOrderByNumeroAsc(jornada);
        List<Horario> horarios = horarioRepository.findByCursoId(cursoId);

        Map<Integer, Map<Integer, HorarioCeldaDTO>> grilla = buildGrillaCurso(bloques, horarios);

        return GrillaHorarioResponse.builder()
                .cursoId(curso.getId())
                .nombreCurso(curso.getNombre())
                .grilla(grilla)
                .bloques(bloques.stream().map(this::toBloqueDTO).toList())
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
                        // Match por id de bloque (invariante real); el put usa numero.
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
                .cursoId(null)
                .nombreCurso(profesor.getNombre() + " " + profesor.getApellido())
                .grilla(grilla)
                .bloques(bloques.stream().map(this::toBloqueDTO).toList())
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
                .bloqueNumero(h.getBloque().getNumero())
                .jornada(h.getBloque().getJornada())
                .horaInicio(h.getBloque().getHoraInicio())
                .horaFin(h.getBloque().getHoraFin())
                .editadoManualmente(h.getEditadoManualmente())
                .esMedioBloque(Boolean.TRUE.equals(h.getEsMedioBloque()))
                .asignaturaSecId(h.getAsignaturaSecundaria() != null ? h.getAsignaturaSecundaria().getId() : null)
                .nombreAsignaturaSec(h.getAsignaturaSecundaria() != null ? h.getAsignaturaSecundaria().getNombre() : null)
                .profesorSecId(h.getProfesorSecundaria() != null ? h.getProfesorSecundaria().getId() : null)
                .nombreProfesorSec(h.getProfesorSecundaria() != null
                        ? h.getProfesorSecundaria().getNombre() + " " + h.getProfesorSecundaria().getApellido() : null)
                .build();
    }
}
