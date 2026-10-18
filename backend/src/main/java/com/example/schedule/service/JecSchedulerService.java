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

/**
 * Generador de horario para la Jornada Escolar Completa (JEC) de 1° a 4° Básico.
 *
 * <p>Usa bloques de 90' (jornada "JEC") y permite que un bloque contenga dos medios subtramos de
 * 45' de asignaturas distintas (medios bloques). La semana tiene 19 bloques: Lun–Jue = 4 bloques,
 * Viernes = 3 (no hay 4.º bloque el viernes). Las cargas impares (Historia, Ciencias) usan un
 * bloque completo + un medio; los medios se emparejan {Ciencias, Tecnología} y {Historia,
 * Orientación} en bloques compartidos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JecSchedulerService {

    private final HorarioRepository horarioRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final ParametroRepository parametroRepository;
    private final CursoRepository cursoRepository;
    private final AuditoriaHelper auditoriaHelper;

    private static final int MAX_PASOS = 5_000_000;

    // Códigos de asignatura que preferentemente comparten un medio bloque.
    private static final Map<String, String> PAREJAS_PREFERENTES = Map.of(
            "CN", "TEC",
            "TEC", "CN",
            "HIS", "ORI",
            "ORI", "HIS");

    /** Par (inicio, fin) de tiempos de un bloque de clase. */
    public record BloqueTiempo(LocalTime inicio, LocalTime fin) {}

    /**
     * Calcula los tiempos de los 4 bloques de clase JEC leyendo los parámetros de forma
     * null-safe (defaults 08:00 / 90' / recreo 15' / almuerzo 75'). El almuerzo va tras el
     * bloque 3; entre los demás bloques va el recreo.
     */
    public List<BloqueTiempo> computarTiemposBloque(Parametro p) {
        LocalTime inicio = parseOrDefault(p == null ? null : p.getHoraInicioJornada(), LocalTime.of(8, 0));
        int bloqueMin = orDefault(p == null ? null : p.getDuracionBloqueMin(), 90);
        int recreo = orDefault(p == null ? null : p.getDuracionRecreo(), 15);
        int almuerzo = orDefault(p == null ? null : p.getDuracionAlmuerzo(), 75);

        List<BloqueTiempo> tiempos = new ArrayList<>();
        LocalTime cursor = inicio;
        for (int n = 1; n <= 4; n++) {
            LocalTime fin = cursor.plusMinutes(bloqueMin);
            tiempos.add(new BloqueTiempo(cursor, fin));
            int gap = (n == 3) ? almuerzo : recreo;
            cursor = fin.plusMinutes(gap);
        }
        return tiempos;
    }

    private LocalTime parseOrDefault(String valor, LocalTime def) {
        if (valor == null || valor.isBlank()) {
            return def;
        }
        try {
            return LocalTime.parse(valor);
        } catch (Exception e) {
            log.warn("horaInicioJornada '{}' no parseable, usando default {}", valor, def);
            return def;
        }
    }

    private int orDefault(Integer valor, int def) {
        return valor == null ? def : valor;
    }

    /** Entrada aislada para tests: borra, coloca y guarda las cargas JEC. */
    @Transactional
    public List<Horario> generarHorarioJec() {
        horarioRepository.deleteByEditadoManualmenteFalse();
        return horarioRepository.saveAll(colocarCursosJec());
    }

    /**
     * Coloca las cargas JEC de los cursos de nivel 1..4 sobre los 4 bloques de 90' de la jornada
     * JEC, sin borrar ni guardar. Devuelve la solución en memoria con la auditoría estampada.
     */
    public List<Horario> colocarCursosJec() {
        List<BloqueHorario> bloquesJec =
                bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("JEC");
        if (bloquesJec.size() < 4) {
            throw new SchedulingException(
                    "No hay suficientes bloques de clase JEC para generar el horario (se requieren 4).");
        }

        // Ranuras disponibles por día: Lun–Jue (1..4) = los 4 bloques; Viernes (5) = los 3 primeros.
        Map<Integer, List<BloqueHorario>> ranurasPorDia = new LinkedHashMap<>();
        for (int dia = 1; dia <= 4; dia++) {
            ranurasPorDia.put(dia, bloquesJec.subList(0, 4));
        }
        ranurasPorDia.put(5, bloquesJec.subList(0, 3));

        List<Curso> cursos = cursoRepository.findAllByOrderByNivelAsc().stream()
                .filter(c -> c.getNivel() != null && c.getNivel() <= 4)
                .toList();

        Set<String> cursoOcupado = new HashSet<>();
        Set<String> profesorOcupado = new HashSet<>();
        List<Horario> solucion = new ArrayList<>();
        int[] contador = {0};

        for (Curso curso : cursos) {
            List<Unidad> unidades = construirUnidades(curso);
            if (!colocarCurso(curso, unidades, 0, ranurasPorDia,
                    cursoOcupado, profesorOcupado, solucion, contador)) {
                throw new SchedulingException(
                        "No se pudo generar el horario JEC para el curso " + curso.getNombre()
                        + " con las restricciones actuales.");
            }
        }

        estamparAuditoria(solucion);
        return solucion;
    }

    /**
     * Coloca las cargas JEC de UN solo curso (nivel 1..4) sobre los 4 bloques de 90' de la jornada
     * JEC, sin borrar ni guardar. Siembra la ocupación de profesores con {@code profesorOcupado}
     * (claves "profesorId-dia-bloqueId" de los horarios ya persistidos de los OTROS cursos) para
     * no duplicar a un profesor que ya está colocado en otro curso. Devuelve la solución en
     * memoria con la auditoría estampada.
     */
    public List<Horario> colocarCursoJecUnico(Curso curso, Set<String> profesorOcupado) {
        List<BloqueHorario> bloquesJec =
                bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("JEC");
        if (bloquesJec.size() < 4) {
            throw new SchedulingException(
                    "No hay suficientes bloques de clase JEC para generar el horario (se requieren 4).");
        }

        Map<Integer, List<BloqueHorario>> ranurasPorDia = new LinkedHashMap<>();
        for (int dia = 1; dia <= 4; dia++) {
            ranurasPorDia.put(dia, bloquesJec.subList(0, 4));
        }
        ranurasPorDia.put(5, bloquesJec.subList(0, 3));

        List<Unidad> unidades = construirUnidades(curso);
        if (unidades.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> cursoOcupado = new HashSet<>();
        List<Horario> solucion = new ArrayList<>();
        int[] contador = {0};

        if (!colocarCurso(curso, unidades, 0, ranurasPorDia,
                cursoOcupado, profesorOcupado, solucion, contador)) {
            throw new SchedulingException(
                    "No se pudo generar el horario JEC para el curso " + curso.getNombre()
                    + " con las restricciones actuales.");
        }

        estamparAuditoria(solucion);
        return solucion;
    }

    private void estamparAuditoria(List<Horario> solucion) {
        String usuario = auditoriaHelper.usuarioActual();
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        for (Horario h : solucion) {
            h.setUsuarioModificacion(usuario);
            h.setFechaModificacion(hoy);
            h.setHoraModificacion(ahora);
        }
    }

    // ─── Construcción de unidades (bloques completos + medios emparejados) ───────

    /** Una unidad de colocación: un bloque completo o un bloque compartido (dos medios 45'). */
    private static final class Unidad {
        final Asignatura asignatura;
        final Profesor profesor;
        final Asignatura asignaturaSec;   // null si es bloque completo
        final Profesor profesorSec;       // null si es bloque completo
        final boolean medio;

        Unidad(Asignatura a, Profesor p) {
            this(a, p, null, null, false);
        }

        Unidad(Asignatura a, Profesor p, Asignatura aSec, Profesor pSec, boolean medio) {
            this.asignatura = a;
            this.profesor = p;
            this.asignaturaSec = aSec;
            this.profesorSec = pSec;
            this.medio = medio;
        }
    }

    /** Medio bloque suelto pendiente de emparejar. */
    private record Medio(Asignatura asignatura, Profesor profesor) {}

    private List<Unidad> construirUnidades(Curso curso) {
        List<CursoAsignatura> cargas = cursoAsignaturaRepository.findByCursoId(curso.getId());

        // Ordena las cargas por nº de bloques completos descendente (MRV: la asignatura más
        // demandante —p.ej. Lenguaje con 4 bloques— se coloca antes que las de 1 bloque). El
        // desempate por id de asignatura mantiene el resultado determinista.
        List<CursoAsignatura> cargasOrdenadas = new ArrayList<>(cargas);
        cargasOrdenadas.sort(
                Comparator.comparingInt((CursoAsignatura ca) -> ca.getHorasSemanales() / 2).reversed()
                        .thenComparing(ca -> ca.getAsignatura().getId()));

        List<Unidad> completas = new ArrayList<>();
        List<Medio> medios = new ArrayList<>();

        for (CursoAsignatura ca : cargasOrdenadas) {
            if (ca.getProfesor() == null) {
                throw new SchedulingException(
                        "El curso " + curso.getNombre() +
                        " no tiene profesor asignado para la asignatura " +
                        ca.getAsignatura().getNombre() +
                        ". Asigne un profesor antes de generar el horario.");
            }
            int horas = ca.getHorasSemanales();
            int fulls = horas / 2;
            for (int i = 0; i < fulls; i++) {
                completas.add(new Unidad(ca.getAsignatura(), ca.getProfesor()));
            }
            if (horas % 2 == 1) {
                medios.add(new Medio(ca.getAsignatura(), ca.getProfesor()));
            }
        }

        if (medios.size() % 2 != 0) {
            throw new SchedulingException(
                    "La carga JEC del curso " + curso.getNombre() +
                    " produce un número impar de medios bloques y no puede emparejarse.");
        }

        List<Unidad> compartidas = emparejarMedios(medios);

        // Orden hardest-first: compartidas (dos profesores) primero, luego las completas.
        List<Unidad> unidades = new ArrayList<>(compartidas);
        unidades.addAll(completas);
        return unidades;
    }

    private List<Unidad> emparejarMedios(List<Medio> medios) {
        List<Medio> pendientes = new ArrayList<>(medios);
        List<Unidad> compartidas = new ArrayList<>();

        // 1) Parejas preferentes por código ({CN,TEC}, {HIS,ORI}).
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (int i = 0; i < pendientes.size() && !cambio; i++) {
                Medio a = pendientes.get(i);
                String codigoEsperado = PAREJAS_PREFERENTES.get(a.asignatura().getCodigo());
                if (codigoEsperado == null) {
                    continue;
                }
                for (int j = 0; j < pendientes.size(); j++) {
                    if (j == i) {
                        continue;
                    }
                    Medio b = pendientes.get(j);
                    if (codigoEsperado.equals(b.asignatura().getCodigo())) {
                        compartidas.add(new Unidad(a.asignatura(), a.profesor(),
                                b.asignatura(), b.profesor(), true));
                        pendientes.remove(Math.max(i, j));
                        pendientes.remove(Math.min(i, j));
                        cambio = true;
                        break;
                    }
                }
            }
        }

        // 2) Restantes emparejados de forma determinista por asignaturaId ascendente.
        pendientes.sort(Comparator.comparing(m -> m.asignatura().getId()));
        for (int i = 0; i + 1 < pendientes.size(); i += 2) {
            Medio a = pendientes.get(i);
            Medio b = pendientes.get(i + 1);
            compartidas.add(new Unidad(a.asignatura(), a.profesor(),
                    b.asignatura(), b.profesor(), true));
        }

        return compartidas;
    }

    // ─── Backtracking de colocación ──────────────────────────────────────────────

    private boolean colocarCurso(Curso curso,
                                 List<Unidad> unidades,
                                 int indice,
                                 Map<Integer, List<BloqueHorario>> ranurasPorDia,
                                 Set<String> cursoOcupado,
                                 Set<String> profesorOcupado,
                                 List<Horario> solucion,
                                 int[] contador) {
        if (indice == unidades.size()) {
            return true;
        }

        Unidad u = unidades.get(indice);
        Long cursoId = curso.getId();

        for (Map.Entry<Integer, List<BloqueHorario>> entrada : ranurasPorDia.entrySet()) {
            int dia = entrada.getKey();
            for (BloqueHorario bloque : entrada.getValue()) {

                if (++contador[0] > MAX_PASOS) {
                    throw new SchedulingException(
                            "Se superó el número máximo de pasos (" + MAX_PASOS +
                            ") al generar el horario JEC. Las restricciones podrían no tener solución.");
                }

                String claveCurso = cursoId + "-" + dia + "-" + bloque.getId();
                if (cursoOcupado.contains(claveCurso)) {
                    continue;
                }
                String claveProf = u.profesor.getId() + "-" + dia + "-" + bloque.getId();
                if (profesorOcupado.contains(claveProf)) {
                    continue;
                }
                String claveProfSec = null;
                if (u.medio) {
                    claveProfSec = u.profesorSec.getId() + "-" + dia + "-" + bloque.getId();
                    if (profesorOcupado.contains(claveProfSec)) {
                        continue;
                    }
                }

                // place
                cursoOcupado.add(claveCurso);
                profesorOcupado.add(claveProf);
                if (claveProfSec != null) {
                    profesorOcupado.add(claveProfSec);
                }
                Horario h = Horario.builder()
                        .curso(curso)
                        .asignatura(u.asignatura)
                        .profesor(u.profesor)
                        .asignaturaSecundaria(u.asignaturaSec)
                        .profesorSecundaria(u.profesorSec)
                        .esMedioBloque(u.medio)
                        .diaSemana(dia)
                        .bloque(bloque)
                        .editadoManualmente(false)
                        .build();
                solucion.add(h);

                if (colocarCurso(curso, unidades, indice + 1, ranurasPorDia,
                        cursoOcupado, profesorOcupado, solucion, contador)) {
                    return true;
                }

                // backtrack
                solucion.remove(solucion.size() - 1);
                cursoOcupado.remove(claveCurso);
                profesorOcupado.remove(claveProf);
                if (claveProfSec != null) {
                    profesorOcupado.remove(claveProfSec);
                }
            }
        }
        return false;
    }
}
