package com.example.schedule.service;

import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.*;
import com.example.schedule.repository.*;
import com.example.schedule.security.AuditoriaHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final HorarioRepository horarioRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final AuditoriaHelper auditoriaHelper;

    private static final int MAX_PASOS = 1_000_000;

    @Transactional
    public List<Horario> generarHorario() {
        horarioRepository.deleteByEditadoManualmenteFalse();

        List<CursoAsignatura> todas = cursoAsignaturaRepository.findAll();
        List<BloqueHorario> bloques = bloqueHorarioRepository.findByEsRecreoFalseOrderByNumeroAsc();

        // Validate all have professors assigned
        for (CursoAsignatura ca : todas) {
            if (ca.getProfesor() == null) {
                throw new SchedulingException(
                        "El curso " + ca.getCurso().getNombre() +
                        " no tiene profesor asignado para la asignatura " +
                        ca.getAsignatura().getNombre() +
                        ". Asigne un profesor antes de generar el horario.");
            }
        }

        // MRV: total weekly teaching load per professor
        Map<Long, Integer> cargaPorProfesor = new HashMap<>();
        for (CursoAsignatura ca : todas) {
            cargaPorProfesor.merge(ca.getProfesor().getId(), ca.getHorasSemanales(), Integer::sum);
        }

        // Order tasks hardest-first: most-loaded professor first, then fewer hours first
        List<CursoAsignatura> ordenCA = new ArrayList<>(todas);
        ordenCA.sort(
                Comparator.<CursoAsignatura>comparingInt(
                                ca -> cargaPorProfesor.get(ca.getProfesor().getId())).reversed()
                        .thenComparingInt(CursoAsignatura::getHorasSemanales));

        // Build the task list: one task per weekly hour of each curso_asignatura
        List<CursoAsignatura> tareas = new ArrayList<>();
        for (CursoAsignatura ca : ordenCA) {
            for (int h = 0; h < ca.getHorasSemanales(); h++) {
                tareas.add(ca);
            }
        }

        // In-memory occupancy + solution accumulator (no DB queries inside the loop)
        Set<String> cursoOcupado = new HashSet<>();
        Set<String> profesorOcupado = new HashSet<>();
        List<Horario> solucion = new ArrayList<>();
        int[] contador = {0};

        boolean ok = asignar(0, tareas, bloques, cursoOcupado, profesorOcupado, solucion, contador);
        if (!ok) {
            throw new SchedulingException(
                    "No se pudo generar un horario completo con las restricciones actuales.");
        }

        String usuario = auditoriaHelper.usuarioActual();
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        for (Horario h : solucion) {
            h.setUsuarioModificacion(usuario);
            h.setFechaModificacion(hoy);
            h.setHoraModificacion(ahora);
        }

        return horarioRepository.saveAll(solucion);
    }

    private boolean asignar(int indice,
                            List<CursoAsignatura> tareas,
                            List<BloqueHorario> bloques,
                            Set<String> cursoOcupado,
                            Set<String> profesorOcupado,
                            List<Horario> solucion,
                            int[] contador) {

        if (indice == tareas.size()) {
            return true; // all weekly hours placed
        }

        CursoAsignatura ca = tareas.get(indice);
        Long cursoId = ca.getCurso().getId();
        Long profesorId = ca.getProfesor().getId();
        Long asignaturaId = ca.getAsignatura().getId();

        // fase 0 = preferred slots (course has < 2 blocks of this subject that day),
        // fase 1 = the rest. Soft rule: never drop a slot, only reorder trials.
        for (int fase = 0; fase < 2; fase++) {
            for (int dia = 1; dia <= 5; dia++) {
                for (BloqueHorario bloque : bloques) {

                    if (++contador[0] > MAX_PASOS) {
                        throw new SchedulingException(
                                "Se superó el número máximo de pasos (" + MAX_PASOS +
                                ") al intentar generar el horario. Las restricciones podrían no tener solución.");
                    }

                    String claveCurso = cursoId + "-" + dia + "-" + bloque.getId();
                    String claveProfesor = profesorId + "-" + dia + "-" + bloque.getId();

                    if (cursoOcupado.contains(claveCurso) || profesorOcupado.contains(claveProfesor)) {
                        continue; // HARD constraint: slot taken for curso or profesor
                    }

                    boolean preferido = contarAsignaturaEnDia(solucion, cursoId, dia, asignaturaId) < 2;
                    if ((fase == 0) != preferido) {
                        continue; // handle this slot only in its matching phase
                    }

                    // place
                    cursoOcupado.add(claveCurso);
                    profesorOcupado.add(claveProfesor);
                    Horario h = Horario.builder()
                            .curso(ca.getCurso())
                            .asignatura(ca.getAsignatura())
                            .profesor(ca.getProfesor())
                            .diaSemana(dia)
                            .bloque(bloque)
                            .editadoManualmente(false)
                            .build();
                    solucion.add(h);

                    if (asignar(indice + 1, tareas, bloques, cursoOcupado, profesorOcupado, solucion, contador)) {
                        return true;
                    }

                    // backtrack
                    solucion.remove(solucion.size() - 1);
                    cursoOcupado.remove(claveCurso);
                    profesorOcupado.remove(claveProfesor);
                }
            }
        }
        return false; // no slot worked for this task
    }

    private int contarAsignaturaEnDia(List<Horario> solucion, Long cursoId, int dia, Long asignaturaId) {
        int n = 0;
        for (Horario h : solucion) {
            if (h.getCurso().getId().equals(cursoId)
                    && h.getDiaSemana() == dia
                    && h.getAsignatura().getId().equals(asignaturaId)) {
                n++;
            }
        }
        return n;
    }
}
